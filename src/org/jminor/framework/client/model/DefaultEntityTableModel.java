/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.table.AbstractFilteredTableModel;
import org.jminor.common.model.table.SortingDirective;
import org.jminor.common.model.valuemap.exception.ValidationException;
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
    super(initializeColumnModel(entityID), Util.rejectNullValue(searchModel, "searchModelModel").getPropertyFilterModels());
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
    bindEditModelEventsInternal();
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
    final Entity entity = getItemAt(rowIndex);

    return getValue(entity, property);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isCellEditable(final int rowIndex, final int columnIndex) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final void setValueAt(final Object value, final int rowIndex, final int columnIndex) {
    throw new UnsupportedOperationException("setValueAt is not supported");
  }

  /** {@inheritDoc} */
  @Override
  public Color getPropertyBackgroundColor(final int row, final Property property) {
    return (Color) getItemAt(row).getBackgroundColor(property);
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

    return Integer.toString(getRowCount()) + " (" + Integer.toString(getSelectionCount()) + " " +
            FrameworkMessages.get(FrameworkMessages.SELECTED) + (filteredItemCount > 0 ? ", " +
            filteredItemCount + " " + FrameworkMessages.get(FrameworkMessages.HIDDEN) + ")" : ")");
  }

  /** {@inheritDoc} */
  @Override
  public final void addEntities(final List<Entity> entities, final boolean atFront) {
    addItems(entities, atFront);
  }

  /** {@inheritDoc} */
  @Override
  public final void replaceEntities(final Collection<Entity> entities) {
    for (final Entity entity : getVisibleItems()) {
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

    setSelectedIndexes(indexes);
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
      propertySummaryModels.put(property.getPropertyID(),
              new DefaultPropertySummaryModel(property, new SummaryValueProvider(editModel, this, property)));
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
      final List<Entity> queryResult = performQuery(getQueryCriteria());
      clear();
      addItems(queryResult, false);
      searchModel.rememberCurrentSearchState();
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

  /**
   * Return the value to display in a table cell for the given property of the given entity.
   * Note that this method is responsible for providing a "human readable" version of the value,
   * such as the caption for value list properties and string versions of foreign key values.
   * @param entity the entity
   * @param property the property
   * @return the value of the given property for the given entity for display
   * @throws IllegalArgumentException in case entity or property is null
   */
  protected Object getValue(final Entity entity, final Property property) {
    Util.rejectNullValue(entity, "entity");
    Util.rejectNullValue(property, "property");
    if (property instanceof Property.ValueListProperty || property instanceof Property.ForeignKeyProperty) {
      return entity.getValueAsString(property);
    }

    return entity.getValue(property);
  }

  /** {@inheritDoc} */
  @Override
  protected final Class getColumnClass(final Property columnIdentifier) {
    return columnIdentifier.getTypeClass();
  }

  /** {@inheritDoc} */
  @Override
  protected final Comparable getComparable(final Entity rowObject, final Property columnIdentifier) {
    return (Comparable) rowObject.getValue(columnIdentifier);
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
  protected void handleDelete(final EntityEditModel.DeleteEvent event) {}

  /**
   * Override to bind events using the edit model, called after the edit model has been set
   */
  protected void bindEditModelEvents() {}

  private void bindEvents() {
    addColumnHiddenListener(new EventAdapter<Property>() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred(final Property eventInfo) {
        handleColumnHidden(eventInfo);
      }
    });
    searchModel.addSimpleSearchListener(new EventAdapter() {
      @Override
      public void eventOccurred() {
        refresh();
      }
    });
  }

  private void bindEditModelEventsInternal() {
    editModel.addAfterDeleteListener(new EventAdapter<EntityEditModel.DeleteEvent>() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred(final EntityEditModel.DeleteEvent eventInfo) {
        handleDeleteInternal(eventInfo);
      }
    });
    editModel.addAfterRefreshListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        refresh();
      }
    });
    addSelectedIndexListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
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

  private void handleDeleteInternal(final EntityEditModel.DeleteEvent e) {
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
    final List<Property> visibleProperties = Entities.getVisibleProperties(entityID);
    if (visibleProperties.isEmpty()) {
      throw new IllegalStateException("No visible properties defined for entity: " + entityID);
    }
    for (final Property property : visibleProperties) {
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

  private static final class SummaryValueProvider implements PropertySummaryModel.PropertyValueProvider {

    private final EntityEditModel editModel;
    private final EntityTableModel tableModel;
    private final Property property;

    private SummaryValueProvider(final EntityEditModel editModel, final EntityTableModel tableModel, final Property property) {
      this.editModel = editModel;
      this.tableModel = tableModel;
      this.property = property;
    }

    /** {@inheritDoc} */
    @Override
    public void bindValuesChangedEvent(final Event event) {
      if (editModel != null) {
        editModel.addAfterInsertListener(event);
      }
      tableModel.addFilteringListener(event);//todo summary is updated twice per refresh
      tableModel.addRefreshDoneListener(event);
      tableModel.addSelectionChangedListener(event);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<?> getValues() {
      return tableModel.getValues(property, isValueSubset());
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValueSubset() {
      return !tableModel.isSelectionEmpty();
    }
  }
}