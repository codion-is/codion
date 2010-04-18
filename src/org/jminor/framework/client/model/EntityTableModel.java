/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.AbstractFilteredTableModel;
import org.jminor.framework.client.model.reporting.EntityJRDataSource;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;
import org.jminor.framework.i18n.FrameworkMessages;

import net.sf.jasperreports.engine.JRDataSource;
import org.apache.log4j.Logger;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A TableModel implementation for displaying and working with entities.
 */
public class EntityTableModel extends AbstractFilteredTableModel<Entity> implements Refreshable {

  private static final Logger log = Util.getLogger(EntityTableModel.class);

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
  private final boolean queryConfigurationAllowed;

  /**
   * If true the underlying query should be filtered by the selected master record
   */
  private boolean queryFilteredByMaster = false;

  /**
   * If true then all underlying records should be shown if no master record is selected
   */
  private boolean showAllWhenNotFiltered = false;

  /**
   * true while the model data is being refreshed
   */
  private boolean isRefreshing = false;

  /**
   * Initializes a new EntityTableModel
   * @param entityID the ID of the entity this table model should represent
   * @param dbProvider a EntityDbProvider instance
   */
  public EntityTableModel(final String entityID, final EntityDbProvider dbProvider) {
    this(entityID, dbProvider, true);
  }

  /**
   * Initializes a new EntityTableModel
   * @param entityID the ID of the entity this table model should represent
   * @param dbProvider a EntityDbProvider instance
   * @param queryConfigurationAllowed true if the underlying query should be configurable by the user
   */
  public EntityTableModel(final String entityID, final EntityDbProvider dbProvider,
                          final boolean queryConfigurationAllowed) {
    super(entityID);
    if (dbProvider == null)
      throw new IllegalArgumentException("dbProvider can not be null");
    if (entityID == null || entityID.length() == 0)
      throw new IllegalArgumentException("entityID must be specified");
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.queryConfigurationAllowed = queryConfigurationAllowed;
    this.tableSearchModel = initializeSearchModel();
    bindEventsInternal();
    bindEvents();
  }

  /**
   * @return the underlying table column properties
   */
  public List<Property> getTableColumnProperties() {
    final List<Property> propertyList = new ArrayList<Property>(getColumnModel().getColumnCount());
    final Enumeration<TableColumn> columnEnumeration = getColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements())
      propertyList.add((Property) columnEnumeration.nextElement().getIdentifier());

    return propertyList;
  }

  /**
   * @return true if the underlying query is filtered instead of simply hiding filtered table items
   */
  public boolean isQueryFilteredByMaster() {
    return queryFilteredByMaster;
  }

  /**
   * @return true if the underlying query should be configurable by the user
   */
  public boolean isQueryConfigurationAllowed() {
    return queryConfigurationAllowed;
  }

  /**
   * @param queryFilteredByMaster if set to true then master selection changes affect the underlying query,
   * otherwise filtering is performed by simply hiding filtered items in the table without re-running the query
   */
  public void setQueryFilteredByMaster(final boolean queryFilteredByMaster) {
    this.queryFilteredByMaster = queryFilteredByMaster;
  }

  /**
   * @return whether to show all underlying entities when no filter is applied.
   */
  public boolean isShowAllWhenNotFiltered() {
    return showAllWhenNotFiltered;
  }

  /**
   * @param showAllWhenNotFiltered if set to true then all underlying entities are shown
   * when no filters are applied, which can be problematic in the case of huge datasets.
   */
  public void setShowAllWhenNotFiltered(final boolean showAllWhenNotFiltered) {
    this.showAllWhenNotFiltered = showAllWhenNotFiltered;
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
   * @param propertyID the ID of the property to sort by
   * @param status the sorting status, use TableSorter.DESCENDING, .NOT_SORTED, .ASCENDING
   */
  public void setSortingStatus(final String propertyID, final int status) {
    final int columnIndex = getColumnModel().getColumnIndex(EntityRepository.getProperty(getEntityID(), propertyID));
    if (columnIndex == -1)
      throw new RuntimeException("Column based on property '" + propertyID + " not found");

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
    return getDbProvider().getEntityDb();
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
   * Returns an initialized JRDataSource instance, the default implementation
   * returns an instance of EntityJRDataSource using the Iterator returned by
   * the <code>initializeReportIterator()</code> method
   * @return an initialized JRDataSource
   * @see #initializeReportIterator()
   * @see org.jminor.framework.client.model.reporting.EntityJRDataSource
   */
  public JRDataSource getJRDataSource() {
    return new EntityJRDataSource(initializeReportIterator());
  }

  /** {@inheritDoc} */
  @Override
  public boolean isCellEditable(final int row, final int column) {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public Class<?> getColumnClass(final int columnIndex) {
    final Property columnProperty = (Property) getColumnModel().getColumn(convertColumnIndexToView(columnIndex)).getIdentifier();

    return getValueClass(columnProperty.getPropertyType(), getItemAtViewIndex(0).getValue(columnProperty.getPropertyID()));
  }

  /** {@inheritDoc} */
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    return getVisibleItems().get(rowIndex).getTableValue((Property) getColumnModel().getColumn(convertColumnIndexToView(columnIndex)).getIdentifier());
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
    for (final Entity entity : getVisibleItems())
      if (entity.getPrimaryKey().equals(primaryKey))
        return entity;

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
            Integer.toString(getSelectedModelIndexes().size())).append(" ").append(
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
    if (isRefreshing)
      return;

    try {
      log.trace(this + " refreshing");
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
      log.trace(this + " refreshing done");
    }
  }

  /**
   * Retrieves the entities identified by the given primary keys and adds them to this table model
   * @param primaryKeys the primary keys
   * @param atFrontOfList if true the entities are added to the front
   * @throws DbException in case of a database exception
   */
  public void addEntitiesByPrimaryKeys(final List<Entity.Key> primaryKeys, boolean atFrontOfList) throws DbException {
    try {
      addItems(getEntityDb().selectMany(primaryKeys), atFrontOfList);
    }
    catch (DbException dbe) {
      throw dbe;
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
      for (final Entity newEntity : entities)
        if (entity.getPrimaryKey().equals(newEntity.getPrimaryKey())) {
          entity.setAs(newEntity);
          final int index = indexOf(entity);
          fireTableRowsUpdated(index, index);
        }
    }

    for (final Entity entity : getHiddenItems()) {
      for (final Entity newEntity : entities)
        if (entity.getPrimaryKey().equals(newEntity.getPrimaryKey()))
          entity.setAs(newEntity);
    }
  }

  /**
   * Filters this table model according the the given values by finding the first foreign key property
   * referencing the entity identified by <code>referencedEntityID</code> and setting <code>referenceEntities</code>
   * as the criteria values. If no foreign key property is found this method has no effect.
   * @param referencedEntityID the ID of the master entity
   * @param referenceEntities the entities to use as criteria values
   * @see #isQueryFilteredByMaster()
   */
  public void filterByReference(final String referencedEntityID, final List<Entity> referenceEntities) {
    final List<Property.ForeignKeyProperty> properties = EntityRepository.getForeignKeyProperties(getEntityID(), referencedEntityID);
    if (properties.size() > 0) {
      if (isQueryFilteredByMaster()) {
        if (tableSearchModel.setSearchValues(properties.get(0).getPropertyID(), referenceEntities))
          refresh();
      }
      else {
        tableSearchModel.setFilterValue(properties.get(0).getPropertyID(),
                (referenceEntities == null || referenceEntities.size() == 0) ? null : referenceEntities.get(0).toString());
      }
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
      if (equal)
        entities.add(entity);
    }

    return entities.toArray(new Entity[entities.size()]);
  }

  /**
   * @return a Map containing all entities which depend on the selected entities,
   * where the keys are entityIDs and the value is an array of entities of that type
   * @throws DbException in case of a database exception
   */
  public final Map<String, List<Entity>> getSelectionDependencies() throws DbException {
    try {
      return getEntityDb().selectDependentEntities(getSelectedItems());
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return getEntityID();
  }

  /** {@inheritDoc} */
  public boolean include(final Entity item) {
    return getSearchModel().include(item);
  }

  /**
   * Returns the PropertySummaryModel associated with the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property
   * @return the PropertySummaryModel for the given property ID
   */
  public PropertySummaryModel getPropertySummaryModel(final String propertyID) {
    return getPropertySummaryModel(EntityRepository.getProperty(getEntityID(), propertyID));
  }

  /**
   * Returns the PropertySummaryModel associated with the given property
   * @param property the property
   * @return the PropertySummaryModel for the given property
   */
  public PropertySummaryModel getPropertySummaryModel(final Property property) {
    if (!propertySummaryModels.containsKey(property.getPropertyID()))
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

    return propertySummaryModels.get(property.getPropertyID());
  }

  public Property getColumnProperty(final int columnIndex) {
    return (Property) getColumnModel().getColumn(columnIndex).getIdentifier();
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

  protected TableColumnModel initializeColumnModel(final String tableIdentifier) {
    final TableColumnModel columnModel = new DefaultTableColumnModel();
    int i = 0;
    for (final Property property : initializeColumnProperties(tableIdentifier)) {
      final TableColumn column = new TableColumn(i++);
      column.setIdentifier(property);
      column.setHeaderValue(property.getCaption());
      if (property.getPreferredColumnWidth() > 0)
        column.setPreferredWidth(property.getPreferredColumnWidth());
      columnModel.addColumn(column);
    }

    return columnModel;
  }

  /**
   * Queries for the data used to populate this EntityTableModel when it is refreshed
   * @param criteria a criteria
   * @return entities selected from the database according the the query criteria.
   */
  protected List<Entity> performQuery(final Criteria criteria) {
    if (isQueryFilteredByMaster() && criteria == null && !isShowAllWhenNotFiltered())
      return new ArrayList<Entity>();

    try {
      return getEntityDb().selectMany(new EntitySelectCriteria(getEntityID(), criteria,
              EntityRepository.getOrderByClause(getEntityID()), getFetchCount()));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the maximum number of records to fetch via the underlying query,
   * by default this returns -1, meaning all records should be fetched
   * @return the fetch count
   */
  protected int getFetchCount() {
    return -1;
  }

  /**
   * Initializes a EntityTableSearchModel based on the properties constituting this EntityTableModel
   * @return a EntityTableSearchModel for this EntityTableModel
   */
  protected EntityTableSearchModel initializeSearchModel() {
    return new EntityTableSearchModel(getEntityID(), getColumnModel(), getDbProvider(), false);
  }

  /**
   * @return a Criteria object used to filter the result when this
   * table models data is queried, the default implementation returns
   * the result retrieved via the <code>getSearchCriteria()</code> method
   * found in the underlying EntityTableSearchModel
   * @see org.jminor.framework.client.model.EntityTableSearchModel#getSearchCriteria()
   */
  protected Criteria getQueryCriteria() {
    return tableSearchModel.getSearchCriteria();
  }

  /**
   * @return a list of Properties that should be used as basis for this table models column model
   */
  protected List<Property> initializeColumnProperties(final String entityID) {
    return new ArrayList<Property>(EntityRepository.getVisibleProperties(entityID));
  }

  /**
   * Returns an Iterator which iterates through the selected entities
   * @return the iterator used when generating reports
   * @see #getJRDataSource()
   */
  protected Iterator<Entity> initializeReportIterator() {
    return getSelectedItems().iterator();
  }

  /**
   * Override to add event bindings
   */
  protected void bindEvents() {}

  /**
   * Maps the index of the column in the table model at
   * <code>modelColumnIndex</code> to the index of the column
   * in the view.  Returns the index of the
   * corresponding column in the view; returns -1 if this column is not
   * being displayed.  If <code>modelColumnIndex</code> is less than zero,
   * returns <code>modelColumnIndex</code>.
   * @param modelColumnIndex the index of the column in the model
   * @return the index of the corresponding column in the view
   */
  private int convertColumnIndexToView(final int modelColumnIndex) {
    if (modelColumnIndex < 0)
      return modelColumnIndex;

    for (int index = 0; index < getColumnCount(); index++)
      if (getColumnModel().getColumn(index).getModelIndex() == modelColumnIndex)
        return index;

    return -1;
  }

  private void bindEventsInternal() {
    tableSearchModel.eventFilterStateChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        filterTable();
      }
    });
    evtRefreshDone.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        tableSearchModel.setSearchModelState();
      }
    });
  }

  private static Class<?> getValueClass(final Type type, final Object value) {
    if (type == Type.INT)
      return Integer.class;
    if (type == Type.DOUBLE)
      return Double.class;
    if (type == Type.BOOLEAN)
      return Boolean.class;
    if (type == Type.DATE || type == Type.TIMESTAMP)
      return Date.class;
    if (type == Type.CHAR)
      return Character.class;
    if (type == Type.ENTITY)
      return Entity.class;

    return value == null ? Object.class : value.getClass();
  }
}