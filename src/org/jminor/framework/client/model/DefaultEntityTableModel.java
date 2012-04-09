/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.AbstractFilteredTableModel;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.SortingDirective;
import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.DeleteListener;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A TableModel implementation for displaying and working with entities.
 *
 * <pre>
 * String entityID = "some.entity";
 * String clientTypeID = "JavadocDemo";
 * User user = new User("scott", "tiger");
 *
 * EntityConnectionProvider connectionProvider = EntityConnectionProviders.createConnectionProvider(user, clientTypeID);
 *
 * EntityTableModel tableModel = new DefaultEntityTableModel(entityID, connectionProvider);
 *
 * EntityEditModel editModel = ...;
 *
 * tableModel.setEditModel(editModel);
 *
 * EntityTablePanel panel = new EntityTablePanel(model);
 * </pre>
 */
public class DefaultEntityTableModel extends AbstractFilteredTableModel<Entity, Property> implements EntityTableModel {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityTableModel.class);

  /**
   * The entity ID
   */
  private final String entityID;

  /**
   * The EntityConnection provider
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * The search model
   */
  private final EntityTableSearchModel searchModel;

  /**
   * Maps PropertySummaryModels to their respective properties
   */
  private final Map<String, PropertySummaryModel> propertySummaryModels = new HashMap<String, PropertySummaryModel>();

  /**
   * True if the underlying query should be configurable by the user
   */
  private boolean queryConfigurationAllowed = true;

  /**
   * The edit model to use when updating/deleting entities
   */
  private EntityEditModel editModel;

  /**
   * the maximum number of records to fetch via the underlying query, -1 meaning all records should be fetched
   */
  private int fetchCount = -1;

  /**
   * If true then querying should be disabled if no criteria is specified
   */
  private boolean queryCriteriaRequired = false;

  /**
   * If true then items deleted via the edit model are removed from this table model
   */
  private boolean removeItemsOnDelete = true;

  /**
   * Indicates if multiple entities can be updated at a time
   */
  private boolean batchUpdateAllowed = true;

  private ReportDataWrapper reportDataSource;

  /**
   * Instantiates a new DefaultEntityTableModel with default column and search models.
   * @param entityID the entity ID
   * @param connectionProvider the db provider
   */
  public DefaultEntityTableModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this(entityID, connectionProvider, new DefaultEntityTableSearchModel(entityID, connectionProvider));
  }

  /**
   * Instantiates a new DefaultEntityTableModel.
   * @param entityID the entity ID
   * @param connectionProvider the db provider
   * @param searchModel the search model
   * @throws IllegalArgumentException if <code>searchModel</code> is null or the search model entityID
   * does not match the one supplied as parameter
   */
  public DefaultEntityTableModel(final String entityID, final EntityConnectionProvider connectionProvider,
                                 final EntityTableSearchModel searchModel) {
    super(initializeColumnModel(entityID), Util.rejectNullValue(searchModel, "searchModelModel").getPropertyFilterModelsOrdered());
    if (!searchModel.getEntityID().equals(entityID)) {
      throw new IllegalArgumentException("Entity ID mismatch, searchModel: " + searchModel.getEntityID() + ", tableModel: " + entityID);
    }
    this.entityID = entityID;
    this.connectionProvider = connectionProvider;
    this.searchModel = searchModel;
    bindEvents();
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + entityID;
  }

  /** {@inheritDoc} */
  @Override
  public final void setEditModel(final EntityEditModel editModel) {
    Util.rejectNullValue(editModel, "editModel");
    if (this.editModel != null) {
      throw new IllegalStateException("Edit model has already been set for table model: " + this);
    }
    if (!editModel.getEntityID().equals(entityID)) {
      throw new IllegalArgumentException("Entity ID mismatch, editModel: " + editModel.getEntityID() + ", tableModel: " + entityID);
    }
    this.editModel = editModel;
    bindEditModelEvents();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean hasEditModel() {
    return this.editModel != null;
  }

  /** {@inheritDoc} */
  @Override
  public final List<Property> getTableColumnProperties() {
    final List<Property> propertyList = new ArrayList<Property>(getColumnModel().getColumnCount());
    final Enumeration<TableColumn> columnEnumeration = getColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements()) {
      propertyList.add((Property) columnEnumeration.nextElement().getIdentifier());
    }

    return propertyList;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isQueryConfigurationAllowed() {
    return queryConfigurationAllowed;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableModel setQueryConfigurationAllowed(final boolean value) {
    this.queryConfigurationAllowed = value;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final int getFetchCount() {
    return fetchCount;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableModel setFetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isQueryCriteriaRequired() {
    return queryCriteriaRequired;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableModel setQueryCriteriaRequired(final boolean value) {
    this.queryCriteriaRequired = value;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isRemoveItemsOnDelete() {
    return removeItemsOnDelete;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableModel setRemoveItemsOnDelete(final boolean value) {
    this.removeItemsOnDelete = value;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableSearchModel getSearchModel() {
    return searchModel;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityEditModel getEditModel() {
    if (editModel == null) {
      throw new IllegalStateException("No edit model has been set for table model: " + this);
    }
    return editModel;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isBatchUpdateAllowed() {
    return batchUpdateAllowed;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableModel setBatchUpdateAllowed(final boolean batchUpdateAllowed) {
    this.batchUpdateAllowed = batchUpdateAllowed;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isDeleteAllowed() {
    return editModel != null && editModel.isDeleteAllowed();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isUpdateAllowed() {
    return editModel != null && editModel.isUpdateAllowed();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isReadOnly() {
    return editModel == null || editModel.isReadOnly();
  }

  /** {@inheritDoc} */
  @Override
  public final ReportDataWrapper getReportDataSource() {
    return reportDataSource;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableModel setReportDataSource(final ReportDataWrapper reportDataSource) {
    this.reportDataSource = reportDataSource;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Class<?> getColumnClass(final int columnIndex) {
    return getColumnClass(getColumnIdentifier(columnIndex));
  }

  /** {@inheritDoc} */
  @Override
  public final int getPropertyColumnIndex(final String propertyID) {
    return getColumnModel().getColumnIndex(Entities.getProperty(getEntityID(), propertyID));
  }

  /** {@inheritDoc} */
  @Override
  public final Object getValueAt(final int rowIndex, final int columnIndex) {
    final Property property = getColumnIdentifier(columnIndex);
    final Entity rowEntity = getItemAt(rowIndex);
    if (property instanceof Property.ValueListProperty || property instanceof Property.ForeignKeyProperty) {
      return rowEntity.getValueAsString(property);
    }

    return rowEntity.getValue(property);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isCellEditable(final int rowIndex, final int columnIndex) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
    throw new UnsupportedOperationException("setValueAt is not supported");
  }

  /** {@inheritDoc} */
  @Override
  public Color getPropertyBackgroundColor(final int row, final Property columnProperty) {
    return getItemAt(row).getBackgroundColor(columnProperty);
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<Object> getValues(final Property property, final boolean selectedOnly) {
    return EntityUtil.getPropertyValues(property.getPropertyID(),
            selectedOnly ? getSelectedItems() : getVisibleItems(), false);
  }

  /** {@inheritDoc} */
  @Override
  public final Entity getEntityByPrimaryKey(final Entity.Key primaryKey) {
    for (final Entity entity : getVisibleItems()) {
      if (entity.getPrimaryKey().equals(primaryKey)) {
        return entity;
      }
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final int indexOf(final Entity.Key primaryKey) {
    return indexOf(getEntityByPrimaryKey(primaryKey));
  }

  /** {@inheritDoc} */
  @Override
  public final String getStatusMessage() {
    final int filteredItemCount = getFilteredItemCount();

    return new StringBuilder(Integer.toString(getRowCount())).append(" (").append(
            Integer.toString(getSelectionCount())).append(" ").append(
            FrameworkMessages.get(FrameworkMessages.SELECTED)).append(
            filteredItemCount > 0 ? ", " + filteredItemCount + " "
                    + FrameworkMessages.get(FrameworkMessages.HIDDEN) + ")" : ")").toString();
  }

  /** {@inheritDoc} */
  @Override
  public final void addEntitiesByPrimaryKeys(final List<Entity.Key> primaryKeys, final boolean atFront) {
    try {
      addItems(connectionProvider.getConnection().selectMany(primaryKeys), atFront);
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void replaceEntities(final Collection<Entity> entities) {
    for (int i = 0; i < getVisibleItemCount(); i++) {
      final Entity entity = getItemAt(i);
      for (final Entity newEntity : entities) {
        if (entity.getPrimaryKey().equals(newEntity.getPrimaryKey())) {
          entity.setAs(newEntity);
          final int index = indexOf(entity);
          fireTableRowsUpdated(index, index);
        }
      }
    }

    for (final Entity entity : getFilteredItems()) {
      for (final Entity newEntity : entities) {
        if (entity.getPrimaryKey().equals(newEntity.getPrimaryKey())) {
          entity.setAs(newEntity);
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setForeignKeySearchValues(final String foreignKeyEntityID, final Collection<Entity> foreignKeyValues) {
    final List<Property.ForeignKeyProperty> properties = Entities.getForeignKeyProperties(entityID, foreignKeyEntityID);
    if (!properties.isEmpty()) {
      setForeignKeySearchValues(properties.get(0), foreignKeyValues);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setForeignKeySearchValues(final Property.ForeignKeyProperty foreignKeyProperty, final Collection<Entity> foreignKeyValues) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    if (searchModel.setSearchValues(foreignKeyProperty.getPropertyID(), foreignKeyValues)) {
      refresh();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void replaceForeignKeyValues(final String foreignKeyEntityID, final Collection<Entity> foreignKeyValues) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(this.entityID, foreignKeyEntityID);
    boolean changed = false;
    for (final Entity entity : getAllItems()) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
        for (final Entity foreignKeyValue : foreignKeyValues) {
          final Entity currentForeignKeyValue = entity.getForeignKeyValue(foreignKeyProperty.getPropertyID());
          if (currentForeignKeyValue != null && currentForeignKeyValue.equals(foreignKeyValue)) {
            currentForeignKeyValue.setAs(foreignKeyValue);
            changed = true;
          }
        }
      }
    }
    if (changed) {
      fireTableChanged(new TableModelEvent(this, 0, getRowCount() - 1));
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<Entity.Key> getPrimaryKeysOfSelectedEntities() {
    return EntityUtil.getPrimaryKeys(getSelectedItems());
  }

  /** {@inheritDoc} */
  @Override
  public final void setSelectedByPrimaryKeys(final Collection<Entity.Key> keys) {
    final List<Entity.Key> keyList = new ArrayList<Entity.Key>(keys);
    final List<Integer> indexes = new ArrayList<Integer>();
    for (final Entity visibleEntity : getVisibleItems()) {
      final int index = keyList.indexOf(visibleEntity.getPrimaryKey());
      if (index >= 0) {
        indexes.add(indexOf(visibleEntity));
        keyList.remove(index);
      }
    }

    setSelectedItemIndexes(indexes);
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<Entity> getEntitiesByPrimaryKeys(final Collection<Entity.Key> keys) {
    final List<Entity> entities = new ArrayList<Entity>();
    for (final Entity entity : getAllItems()) {
      for (final Entity.Key key : keys) {
        if (entity.getPrimaryKey().equals(key)) {
          entities.add(entity);
          break;
        }
      }
    }

    return entities;
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<Entity> getEntitiesByPropertyValues(final Map<String, Object> values) {
    final List<Entity> entities = new ArrayList<Entity>();
    for (final Entity entity : getAllItems()) {
      boolean equal = true;
      for (final Map.Entry<String, Object> entries : values.entrySet()) {
        final String propertyID = entries.getKey();
        if (!entity.getValue(propertyID).equals(entries.getValue())) {
          equal = false;
          break;
        }
      }
      if (equal) {
        entities.add(entity);
      }
    }

    return entities;
  }

  /** {@inheritDoc} */
  @Override
  public final void deleteSelected() throws CancelException, DatabaseException {
    if (editModel == null) {
      throw new IllegalStateException("No edit model has been set for table model: " + this);
    }
    editModel.delete(getSelectedItems());
  }

  /** {@inheritDoc} */
  @Override
  public final void update(final List<Entity> entities) throws CancelException, ValidationException, DatabaseException {
    if (editModel == null) {
      throw new IllegalStateException("No edit model has been set for table model: " + this);
    }
    editModel.update(entities);
  }

  /** {@inheritDoc} */
  @Override
  public final PropertySummaryModel getPropertySummaryModel(final String propertyID) {
    return getPropertySummaryModel(Entities.getProperty(entityID, propertyID));
  }

  /** {@inheritDoc} */
  @Override
  public final PropertySummaryModel getPropertySummaryModel(final Property property) {
    if (!propertySummaryModels.containsKey(property.getPropertyID())) {
      final PropertySummaryModel.PropertyValueProvider valueProvider = new PropertySummaryModel.PropertyValueProvider() {
        /** {@inheritDoc} */
        @Override
        public void bindValuesChangedEvent(final Event event) {
          if (editModel != null) {
            editModel.addAfterInsertListener(event);
          }
          addFilteringListener(event);//todo summary is updated twice per refresh
          addRefreshDoneListener(event);
          addSelectionChangedListener(event);
        }

        /** {@inheritDoc} */
        @Override
        public Collection<?> getValues() {
          return DefaultEntityTableModel.this.getValues(property, isValueSubset());
        }

        /** {@inheritDoc} */
        @Override
        public boolean isValueSubset() {
          return !isSelectionEmpty();
        }
      };
      propertySummaryModels.put(property.getPropertyID(), new DefaultPropertySummaryModel(property, valueProvider));
    }

    return propertySummaryModels.get(property.getPropertyID());
  }

  /** {@inheritDoc} */
  @Override
  public final Iterator<Entity> getSelectedEntitiesIterator() {
    return getSelectedItems().iterator();
  }

  /** {@inheritDoc} */
  @Override
  public final SortingDirective getSortingDirective(final String propertyID) {
    return super.getSortingDirective(Entities.getProperty(entityID, propertyID));
  }

  /** {@inheritDoc} */
  @Override
  public final void setSortingDirective(final String propertyID, final SortingDirective directive,
                                        final boolean addColumnToSort) {
    super.setSortingDirective(Entities.getProperty(entityID, propertyID), directive, addColumnToSort);
  }

  /** {@inheritDoc} */
  @Override
  protected final void doRefresh() {
    try {
      LOG.debug("{} refreshing", this);
      clear();
      addItems(performQuery(getQueryCriteria()), false);
    }
    finally {
      LOG.debug("{} refreshing done", this);
    }
  }

  /**
   * Queries for the data used to populate this EntityTableModel when it is refreshed
   * @param criteria a criteria
   * @return entities selected from the database according the the query criteria.
   * @see #getQueryCriteria()
   */
  protected List<Entity> performQuery(final Criteria<Property.ColumnProperty> criteria) {
    if (criteria == null && queryCriteriaRequired) {
      return new ArrayList<Entity>();
    }

    try {
      return connectionProvider.getConnection().selectMany(EntityCriteriaUtil.selectCriteria(entityID, criteria,
              Entities.getOrderByClause(entityID), fetchCount));
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected final Class getColumnClass(final Property columnIdentifier) {
    return columnIdentifier.getTypeClass();
  }

  /** {@inheritDoc} */
  @Override
  protected final Comparable getComparable(final Object object, final Property columnIdentifier) {
    return (Comparable) ((Entity) object).getValue(columnIdentifier);
  }

  /** {@inheritDoc} */
  @Override
  protected final String getSearchValueAt(final int rowIndex, final Property columnIdentifier) {
    return getItemAt(rowIndex).getValueAsString(columnIdentifier);
  }

  /**
   * @return a Criteria object used to filter the result when this
   * table models data is queried, this implementation returns
   * the result retrieved via the <code>getSearchCriteria()</code> method
   * found in the underlying EntityTableSearchModel
   * @see EntityTableSearchModel#getSearchCriteria()
   */
  protected final Criteria<Property.ColumnProperty> getQueryCriteria() {
    return searchModel.getSearchCriteria();
  }

  @SuppressWarnings({"UnusedDeclaration"})
  protected void handleDelete(final DeleteEvent event) {}

  private void bindEvents() {
    addColumnHiddenListener(new ActionListener() {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        handleColumnHidden((Property) e.getSource());
      }
    });
    addRefreshDoneListener(new ActionListener() {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        searchModel.rememberCurrentSearchState();
      }
    });
    searchModel.addSimpleSearchListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        refresh();
      }
    });
  }

  private void bindEditModelEvents() {
    editModel.addAfterDeleteListener(new DeleteListener() {
      /** {@inheritDoc} */
      @Override
      protected void deleted(final DeleteEvent event) {
        handleDeleteInternal(event);
      }
    });
    editModel.addAfterRefreshListener(new ActionListener() {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        refresh();
      }
    });
    addSelectedIndexListener(new ActionListener() {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        final Entity itemToSelect = isSelectionEmpty() ? null : getSelectedItem();
        editModel.setEntity(itemToSelect);
      }
    });

    addTableModelListener(new TableModelListener() {
      /** {@inheritDoc} */
      @Override
      public void tableChanged(final TableModelEvent e) {
        //if the selected record is being updated via the table model refresh the one in the edit model
        if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() == getSelectedIndex()) {
          editModel.setEntity(null);
          editModel.setEntity(getSelectedItem());
        }
      }
    });
  }

  private void handleDeleteInternal(final DeleteEvent e) {
    if (removeItemsOnDelete) {
      removeItems(e.getDeletedEntities());
    }
    handleDelete(e);
  }

  private void handleColumnHidden(final Property property) {
    //disable the search model for the column to be hidden, to prevent confusion
    searchModel.setSearchEnabled(property.getPropertyID(), false);
  }

  private static TableColumnModel initializeColumnModel(final String entityID) {
    final DefaultTableColumnModel model = new DefaultTableColumnModel();
    int modelIndex = 0;
    for (final Property property : Entities.getVisibleProperties(entityID)) {
      final TableColumn column = new TableColumn(modelIndex++);
      column.setIdentifier(property);
      column.setHeaderValue(property.getCaption());
      if (property.getPreferredColumnWidth() > 0) {
        column.setPreferredWidth(property.getPreferredColumnWidth());
      }
      model.addColumn(column);
    }

    return model;
  }
}