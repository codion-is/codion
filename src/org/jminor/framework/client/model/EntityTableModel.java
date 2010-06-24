/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.model.AbstractFilteredTableModel;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
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
public class EntityTableModel extends AbstractFilteredTableModel<Entity> implements Refreshable {

  private static final Logger LOG = Util.getLogger(EntityTableModel.class);

  private final Event evtRefreshStarted = new Event();
  private final Event evtRefreshDone = new Event();

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
  private final EntityTableSearchModel tableSearchModel;

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

  /**
   * true while the model data is being refreshed
   */
  private boolean isRefreshing = false;

  private final State stAllowMultipleUpdate = new State(true);

  private final State stAllowDelete = new State(true);

  public EntityTableModel(final String entityID, final EntityDbProvider dbProvider) {
    this(entityID, dbProvider, new EntityTableColumnModel(entityID));
  }

  public EntityTableModel(final String entityID, final EntityDbProvider dbProvider, final EntityTableColumnModel columnModel) {
    super(columnModel);
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.tableSearchModel = initializeSearchModel();
    bindEventsInternal();
    bindEvents();
  }

  /**
   * Associates the given edit model with this table model, this enables delete/update
   * functionality via this table model as well as enabling it to
   * react to delete events in the edit model.
   * Throws a RuntimeException in case the edit model has been previously set
   * @param editModel the edit model to associate with this table model
   * @see #deleteSelected()
   * @see #update(java.util.List)
   */
  public void setEditModel(final EntityEditModel editModel) {
    if (this.editModel != null) {
      throw new RuntimeException("Edit model has already been set for table model: " + this);
    }
    this.editModel = editModel;
    this.editModel.eventAfterDelete().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        handleDelete((DeleteEvent) e);
      }
    });
  }

  /**
   * @return the underlying table column properties
   */
  public List<Property> getTableColumnProperties() {
    final List<Property> propertyList = new ArrayList<Property>(getColumnModel().getColumnCount());
    final Enumeration<TableColumn> columnEnumeration = getColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements()) {
      propertyList.add((Property) columnEnumeration.nextElement().getIdentifier());
    }

    return propertyList;
  }

  /**
   * @return true if the underlying query should be configurable by the user
   */
  public boolean isQueryConfigurationAllowed() {
    return queryConfigurationAllowed;
  }

  public void setQueryConfigurationAllowed(final boolean queryConfigurationAllowed) {
    this.queryConfigurationAllowed = queryConfigurationAllowed;
  }

  /**
   * Returns the maximum number of records to fetch via the underlying query,
   * by default this returns -1, meaning all records should be fetched
   * @return the fetch count
   */
  public int getFetchCount() {
    return fetchCount;
  }

  /**
   * Sets the maximum number of records to fetch via the underlying query,
   * a value of -1 means all records should be fetched
   * @param fetchCount the fetch count
   * @return this table model
   */
  public EntityTableModel setFetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
    return this;
  }

  /**
   * @return true if this table model should not run a query unless a query criteria has been specified
   */
  public boolean isDetailModel() {
    return isDetailModel;
  }

  /**
   * @param detailModel if set to true then this table model will not run a query unless a query criteria has been specified
   * @see #setQueryCriteriaRequired(boolean)
   */
  public void setDetailModel(final boolean detailModel) {
    this.isDetailModel = detailModel;
  }

  /**
   * @return whether to show all underlying entities when no criteria is applied.
   */
  public boolean isQueryCriteriaRequired() {
    return queryCriteriaRequired;
  }

  /**
   * @param queryCriteriaRequired if set to true then all underlying entities are shown
   * when no criteria is applied, which can be problematic in the case of huge datasets.
   */
  public void setQueryCriteriaRequired(final boolean queryCriteriaRequired) {
    this.queryCriteriaRequired = queryCriteriaRequired;
  }

  /**
   * @return the ID of the entity this table model represents
   */
  public String getEntityID() {
    return entityID;
  }

  /**
   * @return the EntityTableSearchModel instance used by this table model
   */
  public EntityTableSearchModel getSearchModel() {
    return tableSearchModel;
  }

  /**
   * Returns the edit model associated with this table model,
   * throws a RuntimeExcption in case no edit model has been associated with this table model
   * @return the edit model associated with this table model
   * @see #setEditModel(EntityEditModel)
   */
  public EntityEditModel getEditModel() {
    if (editModel == null) {
      throw new RuntimeException("No edit model has been set for table model: " + this);
    }
    return editModel;
  }

  /**
   * @param propertyID the ID of the property to sort by
   * @param status the sorting status, use TableSorter.DESCENDING, .NOT_SORTED, .ASCENDING
   */
  public void setSortingStatus(final String propertyID, final int status) {
    final int columnIndex = getColumnModel().getColumnIndex(EntityRepository.getProperty(entityID, propertyID));
    if (columnIndex == -1) {
      throw new RuntimeException("Column based on property '" + propertyID + " not found");
    }

    super.setSortingStatus(columnIndex, status);
  }

  /**
   * @return the EntityDbConnection provider
   */
  public EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  /**
   * @return the database connection
   */
  public EntityDb getEntityDb() {
    return dbProvider.getEntityDb();
  }

  /**
   * @return true if the model is either refreshing, filtering or sorting
   */
  public boolean isChangingState() {
    return isRefreshing || isFiltering() || isSorting();
  }

  /**
   * @return true if the model data is being refreshed
   */
  public boolean isRefreshing() {
    return isRefreshing;
  }

  /**
   * @return true if this model allows multiple entities to be updated at a time
   */
  public boolean isMultipleUpdateAllowed() {
    return true;
  }

  /**
   * @return the state used to determine if updating should be enabled
   * @see #isMultipleUpdateAllowed()
   */
  public State stateAllowMultipleUpdate() {
    return stAllowMultipleUpdate.getLinkedState();
  }

  /**
   * @return true if this model should allow records to be deleted
   */
  public boolean isDeleteAllowed() {
    return stAllowDelete.isActive();
  }

  /**
   * @param value true if this model should allow records to be deleted
   */
  public void setDeleteAllowed(final boolean value) {
    stAllowDelete.setActive(value);
  }

  /**
   * @return the state used to determine if deleting should be enabled
   * @see #isDeleteAllowed()
   * @see #setDeleteAllowed(boolean)
   */
  public State stateAllowDelete() {
    return stAllowDelete.getLinkedState();
  }

  /**
   * @return true if this model is read only or if no edit model has been specified.
   * by default this returns the isReadOnly value of the underlying entity
   * @see #setEditModel(EntityEditModel)
   */
  public boolean isReadOnly() {
    return editModel == null || EntityRepository.isReadOnly(entityID);
  }

  /**
   * Returns an initialized ReportDataWrapper instance, the default implementation returns null.
   * @return an initialized ReportDataWrapper
   * @see #getSelectedEntitiesIterator()
   */
  public ReportDataWrapper getReportDataSource() {
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isCellEditable(final int rowIndex, final int columnIndex) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public Class<?> getColumnClass(final int columnIndex) {
    final Property columnProperty = (Property) getColumnModel().getColumn(convertColumnIndexToView(columnIndex)).getIdentifier();

    return columnProperty.getTypeClass();
  }

  /** {@inheritDoc} */
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final Property property = (Property) getColumnModel().getColumn(convertColumnIndexToView(columnIndex)).getIdentifier();
    final Object value = getItemAt(rowIndex).getValue(property);
    if (property instanceof Property.ValueListProperty) {
      return ((Property.ValueListProperty) property).getCaption(value);
    }

    return value;
  }

  /**
   * @param row the row for which to retrieve the background color
   * @return the background color for this row, specified by the row entity
   * @see org.jminor.framework.domain.Entity.Proxy#getBackgroundColor(org.jminor.framework.domain.Entity)
   * @see org.jminor.framework.client.ui.EntityTableCellRenderer
   */
  public Color getRowBackgroundColor(final int row) {
    final Entity rowEntity = getItemAtViewIndex(row);

    return Entity.getProxy(rowEntity.getEntityID()).getBackgroundColor(rowEntity);
  }

  /**
   * @param property the property for which to retrieve the values
   * @param selectedOnly if true only values from the selected entities are returned
   * @return the values of <code>property</code> from the entities in the table model
   */
  public Collection<Object> getValues(final Property property, final boolean selectedOnly) {
    return EntityUtil.getPropertyValues(property.getPropertyID(),
            selectedOnly ? getSelectedItems() : getVisibleItems(), false);
  }

  /**
   * @param primaryKey the primary key to search by
   * @return the entity with the given primary key from the table model, null if it's not found
   */
  public Entity getEntityByPrimaryKey(final Entity.Key primaryKey) {
    for (final Entity entity : getVisibleItems()) {
      if (entity.getPrimaryKey().equals(primaryKey)) {
        return entity;
      }
    }

    return null;
  }

  public int getViewIndexByPrimaryKey(final Entity.Key primaryKey) {
    return viewIndexOf(getEntityByPrimaryKey(primaryKey));
  }

  /**
   * @return a String describing the selected/filtered state of this table model
   */
  public String getStatusMessage() {
    final int hiddenCount = getHiddenItemCount();

    return new StringBuilder(Integer.toString(getRowCount())).append(" (").append(
            Integer.toString(getSelectionCount())).append(" ").append(
            FrameworkMessages.get(FrameworkMessages.SELECTED)).append(
            hiddenCount > 0 ? ", " + hiddenCount + " "
                    + FrameworkMessages.get(FrameworkMessages.HIDDEN) + ")" : ")").toString();
  }

  /**
   * Refreshes the data in this table model, keeping the selected items
   * @see #evtRefreshStarted
   * @see #evtRefreshDone
   */
  public void refresh() {
    if (isRefreshing) {
      return;
    }

    try {
      LOG.trace(this + " refreshing");
      isRefreshing = true;
      evtRefreshStarted.fire();
      final List<Entity.Key> selectedPrimaryKeys = getPrimaryKeysOfSelectedEntities();
      final List<Entity> queryResult = performQuery(getQueryCriteria());
      clear();
      addItems(queryResult, false);
      setSelectedByPrimaryKeys(selectedPrimaryKeys);
    }
    finally {
      isRefreshing = false;
      evtRefreshDone.fire();
      LOG.trace(this + " refreshing done");
    }
  }

  /**
   * Retrieves the entities identified by the given primary keys and adds them to this table model
   * @param primaryKeys the primary keys
   * @param atFrontOfList if true the entities are added to the front
   */
  public void addEntitiesByPrimaryKeys(final List<Entity.Key> primaryKeys, boolean atFrontOfList) {
    try {
      addItems(getEntityDb().selectMany(primaryKeys), atFrontOfList);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Replaces the given entities in this table model
   * @param entities the entities to replace
   */
  public void replaceEntities(final List<Entity> entities) {
    replaceEntities(entities.toArray(new Entity[entities.size()]));
  }

  /**
   * Replaces the given entities in this table model
   * @param entities the entities to replace
   */
  public void replaceEntities(final Entity... entities) {
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

    for (final Entity entity : getHiddenItems()) {
      for (final Entity newEntity : entities) {
        if (entity.getPrimaryKey().equals(newEntity.getPrimaryKey())) {
          entity.setAs(newEntity);
        }
      }
    }
  }

  /**
   * Filters this table model according the the given values by finding the first foreign key property
   * referencing the entity identified by <code>referencedEntityID</code> and setting <code>referenceEntities</code>
   * as the criteria values. If no foreign key property is found this method has no effect.
   * @param referencedEntityID the ID of the master entity
   * @param referenceEntities the entities to use as criteria values
   * @see #isDetailModel()
   */
  public void searchByForeignKeyValues(final String referencedEntityID, final List<Entity> referenceEntities) {
    final List<Property.ForeignKeyProperty> properties = EntityRepository.getForeignKeyProperties(entityID, referencedEntityID);
    if (properties.size() > 0 && isDetailModel && tableSearchModel.setSearchValues(properties.get(0).getPropertyID(), referenceEntities)) {
      refresh();
    }
  }

  /**
   * @return a list containing the primary keys of the selected entities,
   * if none are selected an empty list is returned
   */
  public List<Entity.Key> getPrimaryKeysOfSelectedEntities() {
    return EntityUtil.getPrimaryKeys(getSelectedItems());
  }

  /**
   * Sets the selected entities according to the primary keys in <code>primaryKeys</code>
   * @param primaryKeys the primary keys of the entities to select
   */
  public void setSelectedByPrimaryKeys(final List<Entity.Key> primaryKeys) {
    final List<Integer> indexes = new ArrayList<Integer>();
    for (final Entity visibleEntity : getVisibleItems()) {
      final int index = primaryKeys.indexOf(visibleEntity.getPrimaryKey());
      if (index >= 0) {
        indexes.add(viewIndexOf(visibleEntity));
        primaryKeys.remove(index);
      }
    }

    setSelectedItemIndexes(indexes);
  }

  /**
   * Finds entities according to the values in <code>keys</code>
   * @param keys the primary key values to use as condition
   * @return the entities having the primary key values as in <code>keys</code>
   */
  public List<Entity> getEntitiesByPrimaryKeys(final List<Entity.Key> keys) {
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

  /**
   * Finds entities according to the values of <code>propertyValues</code>
   * @param propertyValues the property values to use as condition mapped
   * to their respective propertyIDs
   * @return the entities having the exact same property values as in <code>properties</properties>
   */
  public Entity[] getEntitiesByPropertyValues(final Map<String, Object> propertyValues) {
    final List<Entity> entities = new ArrayList<Entity>();
    for (final Entity entity : getAllItems()) {
      boolean equal = true;
      for (final Map.Entry<String, Object> entries : propertyValues.entrySet()) {
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

    return entities.toArray(new Entity[entities.size()]);
  }

  public void deleteSelected() throws CancelException, DbException {
    if (editModel == null) {
      throw new RuntimeException("No edit model has been set for table model: " + this);
    }
     editModel.delete(getSelectedItems());
  }

  public void update(final List<Entity> entitites) throws CancelException, ValidationException, DbException {
    if (editModel == null) {
      throw new RuntimeException("No edit model has been set for table model: " + this);
    }
     editModel.update(entitites);
  }

  /**
   * @return a Map containing all entities which depend on the selected entities,
   * where the keys are entityIDs and the value is an array of entities of that type
   */
  public final Map<String, List<Entity>> getSelectionDependencies() {
    try {
      return getEntityDb().selectDependentEntities(getSelectedItems());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return entityID;
  }

  /** {@inheritDoc} */
  public boolean include(final Entity item) {
    return tableSearchModel.include(item);
  }

  /**
   * Returns the PropertySummaryModel associated with the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property
   * @return the PropertySummaryModel for the given property ID
   */
  public PropertySummaryModel getPropertySummaryModel(final String propertyID) {
    return getPropertySummaryModel(EntityRepository.getProperty(entityID, propertyID));
  }

  /**
   * Returns the PropertySummaryModel associated with the given property
   * @param property the property
   * @return the PropertySummaryModel for the given property
   */
  public PropertySummaryModel getPropertySummaryModel(final Property property) {
    if (!propertySummaryModels.containsKey(property.getPropertyID())) {
      propertySummaryModels.put(property.getPropertyID(), new PropertySummaryModel(property,
              new PropertySummaryModel.PropertyValueProvider() {
                public void bindValuesChangedEvent(final Event event) {
                  eventFilteringDone().addListener(event);//todo summary is updated twice per refresh and should update on insert
                  eventRefreshDone().addListener(event);
                  eventSelectionChanged().addListener(event);
                }

                public Collection<?> getValues() {
                  return EntityTableModel.this.getValues(property, isValueSubset());
                }

                public boolean isValueSubset() {
                  return !stateSelectionEmpty().isActive();
                }
              }));
    }

    return propertySummaryModels.get(property.getPropertyID());
  }

  /**
   * Returns the property the column at the given index is based on
   * @param columnIndex the column index
   * @return the column property
   */
  public Property getColumnProperty(final int columnIndex) {
    return (Property) getColumnModel().getColumn(columnIndex).getIdentifier();
  }

  @Override
  public EntityTableColumnModel getColumnModel() {
    return (EntityTableColumnModel) super.getColumnModel();
  }

  /**
   * Returns an Iterator which iterates through the selected entities
   * @return the iterator used when generating reports
   * @see #getReportDataSource()
   */
  public Iterator<Entity> getSelectedEntitiesIterator() {
    return getSelectedItems().iterator();
  }

  /**
   * @return an Event fired when the model has been refreshed, N.B. this event
   * is fired even if the refresh results in an exception
   */
  public Event eventRefreshDone() {
    return evtRefreshDone;
  }

  /**
   * @return an Event fired when the model is about to be refreshed
   */
  public Event eventRefreshStarted() {
    return evtRefreshStarted;
  }

  /**
   * @param entityID the ID of the entity the table is based on
   * @return a list of Properties that should be used as basis for this table models column model
   */
  protected List<Property> initializeColumnProperties(final String entityID) {
    return new ArrayList<Property>(EntityRepository.getVisibleProperties(entityID));
  }

  /**
   * Queries for the data used to populate this EntityTableModel when it is refreshed
   * @param criteria a criteria
   * @return entities selected from the database according the the query criteria.
   */
  protected List<Entity> performQuery(final Criteria<Property> criteria) {
    if (isDetailModel && criteria == null && queryCriteriaRequired) {
      return new ArrayList<Entity>();
    }

    try {
      return getEntityDb().selectMany(new EntitySelectCriteria(entityID, criteria,
              EntityRepository.getOrderByClause(entityID), fetchCount));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected String getSearchValueAt(final int rowIndex, final int columnIndex) {
    final Property property = (Property) getColumnModel().getColumn(convertColumnIndexToView(columnIndex)).getIdentifier();

    return getItemAt(rowIndex).getValueAsString(property);
  }

  /**
   * Initializes a EntityTableSearchModel based on the properties constituting this EntityTableModel
   * @return a EntityTableSearchModel for this EntityTableModel
   */
  protected EntityTableSearchModel initializeSearchModel() {
    return new EntityTableSearchModel(entityID, getColumnModel().getColumnProperties(), dbProvider, false);
  }

  /**
   * @return a Criteria object used to filter the result when this
   * table models data is queried, the default implementation returns
   * the result retrieved via the <code>getSearchCriteria()</code> method
   * found in the underlying EntityTableSearchModel
   * @see org.jminor.framework.client.model.EntityTableSearchModel#getSearchCriteria()
   */
  protected Criteria<Property> getQueryCriteria() {
    return tableSearchModel.getSearchCriteria();
  }

  protected void handleColumnHidden(final Property property) {
    //disable the search model for the column to be hidden, to prevent confusion
    tableSearchModel.setSearchEnabled(property.getPropertyID(), false);
  }

  protected void handleDelete(final DeleteEvent e) {
    removeItems(e.getDeletedEntities());
  }

  /**
   * Override to add event bindings
   */
  protected void bindEvents() {}

  private void bindEventsInternal() {
    eventColumnHidden().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        handleColumnHidden((Property) e.getSource());
      }
    });
    tableSearchModel.eventFilterStateChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        filterTable();
      }
    });
    evtRefreshDone.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        tableSearchModel.setSearchModelState();
      }
    });
  }
}