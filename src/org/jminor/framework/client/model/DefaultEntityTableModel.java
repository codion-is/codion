/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.model.AbstractFilteredTableModel;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.SortingDirective;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.client.model.event.DeleteListener;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import org.apache.log4j.Logger;

import javax.swing.table.TableColumn;
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
 */
public class DefaultEntityTableModel extends AbstractFilteredTableModel<Entity, Property> implements EntityTableModel {

  private static final Logger LOG = Util.getLogger(DefaultEntityTableModel.class);

  /**
   * The entity ID
   */
  private final String entityID;

  /**
   * The EntityDb connection provider
   */
  private final EntityDbProvider dbProvider;

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
   * If true this table model behaves like a detail model, that is
   */
  private boolean isDetailModel = false;

  /**
   * the maximum number of records to fetch via the underlying query, -1 meaning all records should be fetched
   */
  private int fetchCount = -1;

  /**
   * If true then querying should be disabled if no criteria is specified
   */
  private boolean queryCriteriaRequired = true;

  private final State stAllowMultipleUpdate = States.state(true);

  private final State stAllowDelete = States.state(true);

  private ReportDataWrapper reportDataSource;

  public DefaultEntityTableModel(final String entityID, final EntityDbProvider dbProvider) {
    this(entityID, dbProvider, new DefaultEntityTableColumnModel(entityID));
  }

  public DefaultEntityTableModel(final String entityID, final EntityDbProvider dbProvider,
                                 final EntityTableColumnModel columnModel) {
    this(entityID, dbProvider, columnModel, new DefaultEntityTableSearchModel(entityID, dbProvider, false));
  }

  public DefaultEntityTableModel(final String entityID, final EntityDbProvider dbProvider,
                                 final EntityTableColumnModel columnModel,
                                 final EntityTableSearchModel searchModel) {
    super(columnModel, searchModel.getPropertyFilterModelsOrdered());
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.searchModel = searchModel;
    bindEvents();
  }

  @Override
  public boolean isCellEditable(final int rowIndex, final int columnIndex) {
    return false;
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + entityID;
  }

  public final void setEditModel(final EntityEditModel editModel) {
    if (this.editModel != null) {
      throw new RuntimeException("Edit model has already been set for table model: " + this);
    }
    this.editModel = editModel;
    this.editModel.addAfterDeleteListener(new DeleteListener() {
      @Override
      protected void deleted(final DeleteEvent event) {
        handleDeleteInternal(event);
      }
    });
  }

  public final List<Property> getTableColumnProperties() {
    final List<Property> propertyList = new ArrayList<Property>(getColumnModel().getColumnCount());
    final Enumeration<TableColumn> columnEnumeration = getColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements()) {
      propertyList.add((Property) columnEnumeration.nextElement().getIdentifier());
    }

    return propertyList;
  }

  public final boolean isQueryConfigurationAllowed() {
    return queryConfigurationAllowed;
  }

  public final EntityTableModel setQueryConfigurationAllowed(final boolean value) {
    this.queryConfigurationAllowed = value;
    return this;
  }

  public final int getFetchCount() {
    return fetchCount;
  }

  public final DefaultEntityTableModel setFetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
    return this;
  }

  public final boolean isDetailModel() {
    return isDetailModel;
  }

  public final void setDetailModel(final boolean detailModel) {
    this.isDetailModel = detailModel;
  }

  public final boolean isQueryCriteriaRequired() {
    return queryCriteriaRequired;
  }

  public final EntityTableModel setQueryCriteriaRequired(final boolean value) {
    this.queryCriteriaRequired = value;
    return this;
  }

  public final String getEntityID() {
    return entityID;
  }

  public final EntityTableSearchModel getSearchModel() {
    return searchModel;
  }

  public final EntityEditModel getEditModel() {
    if (editModel == null) {
      throw new RuntimeException("No edit model has been set for table model: " + this);
    }
    return editModel;
  }

  public final void setSortingDirective(final String propertyID, final SortingDirective directive) {
    final int columnIndex = getColumnModel().getColumnIndex(Entities.getProperty(entityID, propertyID));
    if (columnIndex == -1) {
      throw new IllegalArgumentException("Column based on property '" + propertyID + " not found");
    }

    super.setSortingDirective(columnIndex, directive);
  }

  public final EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  public final boolean isMultipleUpdateAllowed() {
    return stAllowMultipleUpdate.isActive();
  }

  public final EntityTableModel setMultipleUpdateAllowed(final boolean multipleUpdateAllowed) {
    stAllowMultipleUpdate.setActive(multipleUpdateAllowed);
    return this;
  }

  public final StateObserver getAllowMultipleUpdateState() {
    return stAllowMultipleUpdate.getObserver();
  }

  public final boolean isDeleteAllowed() {
    return stAllowDelete.isActive();
  }

  public final EntityTableModel setDeleteAllowed(final boolean deleteAllowed) {
    stAllowDelete.setActive(deleteAllowed);
    return this;
  }

  public final StateObserver getAllowDeleteState() {
    return stAllowDelete.getObserver();
  }

  public final boolean isReadOnly() {
    return editModel != null && editModel.isReadOnly();
  }

  /**
   * Returns an initialized ReportDataWrapper instance.
   * @return an initialized ReportDataWrapper
   * @see #getSelectedEntitiesIterator()
   */
  public final ReportDataWrapper getReportDataSource() {
    return reportDataSource;
  }

  public final EntityTableModel setReportDataSource(final ReportDataWrapper reportDataSource) {
    this.reportDataSource = reportDataSource;
    return this;
  }

  @Override
  public final Class<?> getColumnClass(final int columnIndex) {
    final Property columnProperty = (Property) getColumnModel().getColumn(convertColumnIndexToView(columnIndex)).getIdentifier();

    return columnProperty.getTypeClass();
  }

  public final Object getValueAt(final int rowIndex, final int columnIndex) {
    final Property property = (Property) getColumnModel().getColumn(convertColumnIndexToView(columnIndex)).getIdentifier();
    final Object value = getItemAt(rowIndex).getValue(property);
    if (property instanceof Property.ValueListProperty) {
      return ((Property.ValueListProperty) property).getCaption(value);
    }

    return value;
  }

  @Override
  public void setValueAt(final Object aValue, final int rowIndex, final int columnIndex) {
    throw new RuntimeException("setValueAt is not supported");
  }

  public Color getRowBackgroundColor(final int row) {
    final Entity rowEntity = getItemAt(row);

    return Entities.getProxy(rowEntity.getEntityID()).getBackgroundColor(rowEntity);
  }

  public final Collection<Object> getValues(final Property property, final boolean selectedOnly) {
    return EntityUtil.getPropertyValues(property.getPropertyID(),
            selectedOnly ? getSelectedItems() : getVisibleItems(), false);
  }

  public final Entity getEntityByPrimaryKey(final Entity.Key primaryKey) {
    for (final Entity entity : getVisibleItems()) {
      if (entity.getPrimaryKey().equals(primaryKey)) {
        return entity;
      }
    }

    return null;
  }

  public final int indexOf(final Entity.Key primaryKey) {
    return indexOf(getEntityByPrimaryKey(primaryKey));
  }

  public final String getStatusMessage() {
    final int filteredItemCount = getFilteredItemCount();

    return new StringBuilder(Integer.toString(getRowCount())).append(" (").append(
            Integer.toString(getSelectionCount())).append(" ").append(
            FrameworkMessages.get(FrameworkMessages.SELECTED)).append(
            filteredItemCount > 0 ? ", " + filteredItemCount + " "
                    + FrameworkMessages.get(FrameworkMessages.HIDDEN) + ")" : ")").toString();
  }

  public final void addEntitiesByPrimaryKeys(final List<Entity.Key> primaryKeys, final boolean atFront) {
    try {
      addItems(dbProvider.getEntityDb().selectMany(primaryKeys), atFront);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

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

  public void searchByForeignKeyValues(final String referencedEntityID, final List<Entity> referenceEntities) {
    final List<Property.ForeignKeyProperty> properties = Entities.getForeignKeyProperties(entityID, referencedEntityID);
    if (!properties.isEmpty() && isDetailModel && searchModel.setSearchValues(properties.get(0).getPropertyID(), referenceEntities)) {
      refresh();
    }
  }

  public final List<Entity.Key> getPrimaryKeysOfSelectedEntities() {
    return EntityUtil.getPrimaryKeys(getSelectedItems());
  }

  public final void setSelectedByPrimaryKeys(final List<Entity.Key> keys) {
    final List<Integer> indexes = new ArrayList<Integer>();
    for (final Entity visibleEntity : getVisibleItems()) {
      final int index = keys.indexOf(visibleEntity.getPrimaryKey());
      if (index >= 0) {
        indexes.add(indexOf(visibleEntity));
        keys.remove(index);
      }
    }

    setSelectedItemIndexes(indexes);
  }

  public final List<Entity> getEntitiesByPrimaryKeys(final List<Entity.Key> keys) {
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

  public final void deleteSelected() throws CancelException, DbException {
    if (editModel == null) {
      throw new RuntimeException("No edit model has been set for table model: " + this);
    }
    editModel.delete(getSelectedItems());
  }

  public final void update(final List<Entity> entities) throws CancelException, ValidationException, DbException {
    if (editModel == null) {
      throw new RuntimeException("No edit model has been set for table model: " + this);
    }
    editModel.update(entities);
  }

  public final Map<String, Collection<Entity>> getSelectionDependencies() {
    try {
      return dbProvider.getEntityDb().selectDependentEntities(getSelectedItems());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public final PropertySummaryModel getPropertySummaryModel(final String propertyID) {
    return getPropertySummaryModel(Entities.getProperty(entityID, propertyID));
  }

  public final PropertySummaryModel getPropertySummaryModel(final Property property) {
    if (!propertySummaryModels.containsKey(property.getPropertyID())) {
      final PropertySummaryModel.PropertyValueProvider valueProvider = new PropertySummaryModel.PropertyValueProvider() {
        public void bindValuesChangedEvent(final Event event) {
          addFilteringListener(event);//todo summary is updated twice per refresh and should update on insert
          addRefreshDoneListener(event);
          addSelectionChangedListener(event);
        }

        public Collection<?> getValues() {
          return DefaultEntityTableModel.this.getValues(property, isValueSubset());
        }

        public boolean isValueSubset() {
          return !isSelectionEmpty();
        }
      };
      propertySummaryModels.put(property.getPropertyID(), new DefaultPropertySummaryModel(property, valueProvider));
    }

    return propertySummaryModels.get(property.getPropertyID());
  }

  public final Property getColumnProperty(final int columnIndex) {
    return (Property) getColumnModel().getColumn(columnIndex).getIdentifier();
  }

  public final Iterator<Entity> getSelectedEntitiesIterator() {
    return getSelectedItems().iterator();
  }

  @Override
  protected final void doRefresh() {
    try {
      LOG.debug(this + " refreshing");
      final List<Entity.Key> selectedPrimaryKeys = getPrimaryKeysOfSelectedEntities();
      final List<Entity> queryResult = performQuery(getQueryCriteria());
      clear();
      addItems(queryResult, false);
      setSelectedByPrimaryKeys(selectedPrimaryKeys);
    }
    finally {
      LOG.debug(this + " refreshing done");
    }
  }

  /**
   * Queries for the data used to populate this EntityTableModel when it is refreshed
   * @param criteria a criteria
   * @return entities selected from the database according the the query criteria.
   * @see #getQueryCriteria()
   */
  protected List<Entity> performQuery(final Criteria<Property.ColumnProperty> criteria) {
    if (isDetailModel && criteria == null && queryCriteriaRequired) {
      return new ArrayList<Entity>();
    }

    try {
      return dbProvider.getEntityDb().selectMany(EntityCriteriaUtil.selectCriteria(entityID, criteria,
              Entities.getOrderByClause(entityID), fetchCount));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected final Comparable getComparable(final Object object, final int columnIndex) {
    final Property property = getColumnProperty(columnIndex);
    return (Comparable) ((Entity) object).getValue(property);
  }

  @Override
  protected final String getSearchValueAt(final int rowIndex, final int columnIndex) {
    final Property property = (Property) getColumnModel().getColumn(convertColumnIndexToView(columnIndex)).getIdentifier();

    return getItemAt(rowIndex).getValueAsString(property);
  }

  /**
   * @return a Criteria object used to filter the result when this
   * table models data is queried, the default implementation returns
   * the result retrieved via the <code>getSearchCriteria()</code> method
   * found in the underlying EntityTableSearchModel
   * @see EntityTableSearchModel#getSearchCriteria()
   */
  protected final Criteria<Property.ColumnProperty> getQueryCriteria() {
    return searchModel.getSearchCriteria();
  }

  protected void handleDelete(final DeleteEvent event) {}

  private void bindEvents() {
    addColumnHiddenListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        handleColumnHidden((Property) e.getSource());
      }
    });
    addRefreshDoneListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        searchModel.setSearchModelState();
      }
    });
    searchModel.addFilterStateListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        filterContents();
      }
    });
  }

  private void handleDeleteInternal(final DeleteEvent e) {
    removeItems(e.getDeletedEntities());
    handleDelete(e);
  }

  private void handleColumnHidden(final Property property) {
    //disable the search model for the column to be hidden, to prevent confusion
    searchModel.setSearchEnabled(property.getPropertyID(), false);
  }
}