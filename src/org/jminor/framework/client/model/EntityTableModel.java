/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.DbException;
import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.TableSorter;
import org.jminor.framework.client.model.reporting.EntityJRDataSource;
import org.jminor.framework.db.EntityDb;
import org.jminor.framework.db.criteria.SelectCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;
import org.jminor.framework.i18n.FrameworkMessages;

import net.sf.jasperreports.engine.JRDataSource;
import org.apache.log4j.Logger;

import javax.swing.DefaultListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * A TableModel implementation for showing entities
 */
public class EntityTableModel extends AbstractTableModel implements Refreshable {

  private static final Logger log = Util.getLogger(EntityTableModel.class);

  private final Event evtRefreshStarted = new Event();
  private final Event evtRefreshDone = new Event();
  private final Event evtFilteringStarted = new Event();
  private final Event evtFilteringDone = new Event();
  private final Event evtTableDataChanged = new Event();
  private final Event evtSelectedIndexChanged = new Event();
  private final Event evtSelectionChangedAdjusting = new Event();
  private final Event evtSelectionChanged = new Event();

  private final State stSelectionEmpty = new State(true);

  /**
   * The entity ID
   */
  private final String entityID;

  /**
   * The EntityDb connection provider
   */
  private final EntityDbProvider dbProvider;

  /**
   * Holds visible entities
   */
  private final List<Entity> visibleEntities = new ArrayList<Entity>();

  /**
   * Holds entities that are hidden
   */
  private final List<Entity> hiddenEntities = new ArrayList<Entity>();

  /**
   * The TableColumnModel
   */
  private final TableColumnModel tableColumnModel;

  /**
   * The search model
   */
  private final EntityTableSearchModel tableSearchModel;

  /**
   * Maps PropertySummaryModels to their respective properties
   */
  private final Map<String, PropertySummaryModel> propertySummaryModels = new HashMap<String, PropertySummaryModel>();

  /**
   * The sorter model
   */
  private final TableSorter tableSorter;

  /**
   * True if the underlying query should be configurable by the user
   */
  private final boolean queryConfigurationAllowed;

  /**
   * Holds the selected items while sorting
   */
  private List<Entity.Key> selectedPrimaryKeys;

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
   * true while the model data is being filtered
   */
  private boolean isFiltering = false;

  /**
   * true while the model data is being sorted
   */
  private boolean isSorting = false;

  /**
   * true while the selection is being updated
   */
  private boolean isUpdatingSelection = false;

  /**
   * Holds the topmost (minimum) selected index
   */
  private int minSelectedIndex = -1;

  /**
   * The selection model
   */
  private final DefaultListSelectionModel selectionModel = new DefaultListSelectionModel() {
    @Override
    public void fireValueChanged(int min, int max, boolean isAdjusting) {
      super.fireValueChanged(min, max, isAdjusting);
      stSelectionEmpty.setActive(isSelectionEmpty());
      final int minSelIndex = getMinSelectionIndex();
      if (minSelectedIndex != minSelIndex) {
        minSelectedIndex = minSelIndex;
        evtSelectedIndexChanged.fire();
      }
      if (isAdjusting || isUpdatingSelection || isSorting)
        evtSelectionChangedAdjusting.fire();
      else
        evtSelectionChanged.fire();
    }
  };

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
    if (dbProvider == null)
      throw new IllegalArgumentException("dbProvider can not be null");
    if (entityID == null || entityID.length() == 0)
      throw new IllegalArgumentException("entityID must be specified");
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.queryConfigurationAllowed = queryConfigurationAllowed;
    this.tableColumnModel = initializeTableColumnModel();
    this.tableSearchModel = initializeSearchModel();
    this.tableSorter = new TableSorter(this);
    bindEventsInternal();
    bindEvents();
  }

  /**
   * @return the underlying table column properties
   */
  public List<Property> getTableColumnProperties() {
    final List<Property> propertyList = new ArrayList<Property>(tableColumnModel.getColumnCount());
    final Enumeration<TableColumn> columnEnumeration = tableColumnModel.getColumns();
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
   * @return the TableSorter used by this EntityTableModel
   */
  public TableSorter getTableSorter() {
    return tableSorter;
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
    final int columnIndex = getTableColumnModel().getColumnIndex(EntityRepository.getProperty(getEntityID(), propertyID));
    if (columnIndex == -1)
      throw new RuntimeException("Column based on property '" + propertyID + " not found");

    tableSorter.setSortingStatus(columnIndex, status);
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
    return isRefreshing || isFiltering || isSorting;
  }

  /**
   * @return the ListSelectionModel this EntityTableModel uses
   */
  public DefaultListSelectionModel getSelectionModel() {
    return selectionModel;
  }

  /**
   * Moves all selected indexes up one index, wraps around
   * @see #evtSelectionChanged
   */
  public void moveSelectionUp() {
    if (visibleEntities.size() > 0) {
      if (getSelectionModel().isSelectionEmpty())
        getSelectionModel().setSelectionInterval(visibleEntities.size() - 1, visibleEntities.size() - 1);
      else {
        final Collection<Integer> selected = getSelectedViewIndexes();
        final List<Integer> newSelected = new ArrayList<Integer>(selected.size());
        for (final Integer index : selected)
          newSelected.add(index == 0 ? visibleEntities.size() - 1 : index - 1);

        setSelectedItemIndexes(newSelected);
      }
    }
  }

  /**
   * Moves all selected indexes down one index, wraps around
   * @see #evtSelectionChanged
   */
  public void moveSelectionDown() {
    if (visibleEntities.size() > 0) {
      if (getSelectionModel().isSelectionEmpty())
        getSelectionModel().setSelectionInterval(0,0);
      else {
        final Collection<Integer> selected = getSelectedViewIndexes();
        final List<Integer> newSelected = new ArrayList<Integer>(selected.size());
        for (final Integer index : selected)
          newSelected.add(index == visibleEntities.size() - 1 ? 0 : index + 1);

        setSelectedItemIndexes(newSelected);
      }
    }
  }

  /**
   * Selects all visible entities
   * @see #evtSelectionChanged
   */
  public void selectAll() {
    getSelectionModel().setSelectionInterval(0, visibleEntities.size() - 1);
  }

  /**
   * Clears the selection
   * @see #evtSelectionChanged
   */
  public void clearSelection() {
    getSelectionModel().clearSelection();
  }

  /**
   * Clears all entities from this EntityTableModel
   */
  public void clear() {
    hiddenEntities.clear();
    final int size = getRowCount();
    if (size > 0) {
      visibleEntities.clear();
      fireTableRowsDeleted(0, size - 1);
    }
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
    final Property columnProperty = (Property) getTableColumnModel().getColumn(convertColumnIndexToView(columnIndex)).getIdentifier();

    return getValueClass(columnProperty.getPropertyType(), getEntityAtViewIndex(0).getValue(columnProperty.getPropertyID()));
  }

  /** {@inheritDoc} */
  public int getColumnCount() {
    return getTableColumnModel().getColumnCount();
  }

  /** {@inheritDoc} */
  public int getRowCount() {
    return visibleEntities.size();
  }

  /** {@inheritDoc} */
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    return visibleEntities.get(rowIndex).getTableValue((Property) getTableColumnModel().getColumn(convertColumnIndexToView(columnIndex)).getIdentifier());
  }

  /**
   * Returns the TableColumn for the given property
   * @param property the property for which to retrieve the column
   * @return the TableColumn associated with the given property
   */
  public TableColumn getTableColumn(final Property property) {
    return tableColumnModel.getColumn(tableColumnModel.getColumnIndex(property));
  }

  /**
   * @param row the row for which to retrieve the background color
   * @return the background color for this row, specified by the row entity
   * @see org.jminor.framework.domain.Entity.Proxy#getBackgroundColor(org.jminor.framework.domain.Entity)
   * @see org.jminor.framework.client.ui.EntityTableCellRenderer
   */
  public Color getRowBackgroundColor(final int row) {
    final Entity rowEntity = getEntityAtViewIndex(row);

    return Entity.getProxy(rowEntity.getEntityID()).getBackgroundColor(rowEntity);
  }

  /**
   * @param property the property for which to retrieve the values
   * @param selectedOnly if true only values from the selected entities are returned
   * @return the values of <code>property</code> from the entities in the table model
   */
  public Collection<Object> getValues(final Property property, final boolean selectedOnly) {
    return EntityUtil.getPropertyValues(property.getPropertyID(),
            selectedOnly ? getSelectedEntities() : visibleEntities, false);
  }

  /**
   * @param primaryKey the primary key to search by
   * @return the entity with the given primary key from the table model, null if it's not found
   */
  public Entity getEntityByPrimaryKey(final Entity.Key primaryKey) {
    for (final Entity entity : visibleEntities)
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
    final int hiddenCount = getHiddenCount();

    return new StringBuilder(Integer.toString(getRowCount())).append(" (").append(
            Integer.toString(getSelectedModelIndexes().size())).append(" ").append(
            FrameworkMessages.get(FrameworkMessages.SELECTED)).append(
            hiddenCount > 0 ? ", " + hiddenCount + " "
                    + FrameworkMessages.get(FrameworkMessages.HIDDEN) + ")" : ")").toString();
  }

  public Collection<Integer> getSelectedViewIndexes() {
    final List<Integer> indexes = new ArrayList<Integer>();
    final int min = selectionModel.getMinSelectionIndex();
    final int max = selectionModel.getMaxSelectionIndex();
    for (int i = min; i <= max; i++)
      if (selectionModel.isSelectedIndex(i))
        indexes.add(i);

    return indexes;
  }

  public Collection<Integer> getSelectedModelIndexes() {
    final Collection<Integer> indexes = new ArrayList<Integer>();
    final int min = selectionModel.getMinSelectionIndex();
    final int max = selectionModel.getMaxSelectionIndex();
    if (min >= 0 && max >= 0) {
      for (int i = min; i <= max; i++)
        if (selectionModel.isSelectedIndex(i))
          indexes.add(tableSorter.modelIndex(i));
    }

    return indexes;
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
      addEntities(queryResult, false);
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
      addEntities(getEntityDb().selectMany(primaryKeys), atFrontOfList);
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
    for (int i = 0; i < visibleEntities.size(); i++) {
      final Entity entity = visibleEntities.get(i);
      for (final Entity newEntity : entities)
        if (entity.getPrimaryKey().equals(newEntity.getPrimaryKey())) {
          entity.setAs(newEntity);
          final int index = visibleEntities.indexOf(entity);
          fireTableRowsUpdated(index, index);
        }
    }

    for (final Entity entity : hiddenEntities) {
      for (final Entity newEntity : entities)
        if (entity.getPrimaryKey().equals(newEntity.getPrimaryKey()))
          entity.setAs(newEntity);
    }
  }

  /**
   * Removes the given entities from this table model
   * @param entities the entities to remove from the model
   */
  public void removeEntities(final List<Entity> entities) {
    for (final Entity entity : entities)
      removeEntity(entity);
  }

  /**
   * Removes the given entity from this table model
   * @param entity the entity to remove from the model
   */
  public void removeEntity(final Entity entity) {
    int index = visibleEntities.indexOf(entity);
    if (index >= 0) {
      visibleEntities.remove(index);
      fireTableRowsDeleted(index, index);
    }
    else {
      index = hiddenEntities.indexOf(entity);
      if (index >= 0)
        hiddenEntities.remove(index);
    }
  }

  /**
   * @param index the index
   * @return the Entity at <code>index</code>
   */
  public Entity getEntityAtViewIndex(final int index) {
    if (index >= 0 && index < visibleEntities.size())
      return visibleEntities.get(tableSorter.modelIndex(index));

    throw new ArrayIndexOutOfBoundsException("No visible entity found at index: " + index + ", size: " + visibleEntities.size());
  }

  /**
   * Filters this table model
   * @see #evtFilteringStarted
   * @see #evtFilteringDone
   */
  public void filterTable() {
    try {
      isFiltering = true;
      evtFilteringStarted.fire();
      final List<Entity.Key> selectedPrimaryKeys = getPrimaryKeysOfSelectedEntities();
      visibleEntities.addAll(hiddenEntities);
      hiddenEntities.clear();
      for (final ListIterator<Entity> iterator = visibleEntities.listIterator(); iterator.hasNext();) {
        final Entity entity = iterator.next();
        if (!tableSearchModel.include(entity)) {
          hiddenEntities.add(entity);
          iterator.remove();
        }
      }
      fireTableChanged(new TableModelEvent(this, 0, Integer.MAX_VALUE, -1));
      setSelectedByPrimaryKeys(selectedPrimaryKeys);
    }
    finally {
      isFiltering = false;
      evtFilteringDone.fire();
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
    return EntityUtil.getPrimaryKeys(getSelectedEntities());
  }

  /**
   * @return a list containing the selected entities
   */
  public List<Entity> getSelectedEntities() {
    final Collection<Integer> selectedModelIndexes = getSelectedModelIndexes();
    final List<Entity> selectedEntities = new ArrayList<Entity>();
    for (final int modelIndex : selectedModelIndexes)
      selectedEntities.add(visibleEntities.get(modelIndex));

    return selectedEntities;
  }

  /**
   * Selects the given entities
   * @param entities the entities to select
   */
  public void setSelectedEntities(final List<Entity> entities) {
    final List<Integer> indexes = new ArrayList<Integer>();
    for (final Entity entity : entities) {
      final int index = viewIndexOf(entity);
      if (index >= 0)
        indexes.add(index);
    }

    setSelectedItemIndexes(indexes);
  }

  /**
   * @return the selected entity, null if none is selected
   */
  public Entity getSelectedEntity() {
    final int index = selectionModel.getMinSelectionIndex();
    if (index >= 0 && index < visibleEntities.size())
      return getEntityAtViewIndex(index);
    else
      return null;
  }

  /**
   * Sets the selected entity
   * @param entity the entity to select
   */
  public void setSelectedEntity(final Entity entity) {
    setSelectedEntities(Arrays.asList(entity));
  }

  /**
   * Sets the selected entities according to the primary keys in <code>primaryKeys</code>
   * @param primaryKeys the primary keys of the entities to select
   */
  public void setSelectedByPrimaryKeys(final List<Entity.Key> primaryKeys) {
    final List<Integer> indexes = new ArrayList<Integer>();
    for (final Entity visibleEntity : visibleEntities) {
      final int index = primaryKeys.indexOf(visibleEntity.getPrimaryKey());
      if (index >= 0) {
        indexes.add(viewIndexOf(visibleEntity));
        primaryKeys.remove(index);
      }
    }

    setSelectedItemIndexes(indexes);
  }

  /**
   * Clears the selection and selects the entity at <code>index</code>
   * @param index the index
   */
  public void setSelectedItemIndex(final int index) {
    selectionModel.setSelectionInterval(index, index);
  }

  /**
   * Selects the entity at <code>index</code>
   * @param index the index
   */
  public void addSelectedItemIndex(final int index) {
    selectionModel.addSelectionInterval(index, index);
  }

  /**
   * Selects the given indexes
   * @param indexes the indexes to select
   */
  public void setSelectedItemIndexes(final List<Integer> indexes) {
    selectionModel.clearSelection();
    addSelectedItemIndexes(indexes);
  }

  /**
   * Adds these indexes to the selection
   * @param indexes the indexes to add to the selection
   */
  public void addSelectedItemIndexes(final List<Integer> indexes) {
    try {
      isUpdatingSelection = true;
      for (int i = 0; i < indexes.size()-1; i++) {
        final int index = indexes.get(i);
        selectionModel.addSelectionInterval(index, index);
      }
    }
    finally {
      isUpdatingSelection = false;
      if (indexes.size() > 0) {
        final int lastIndex = indexes.get(indexes.size()-1);
        selectionModel.addSelectionInterval(lastIndex, lastIndex);
      }
    }
  }

  /**
   * @return the index of the selected record, -1 if none is selected and
   * the lowest index if more than one record is selected
   */
  public int getSelectedIndex() {
    return minSelectedIndex;
  }

  /**
   * Finds entities according to the values in <code>keys</code>
   * @param keys the primary key values to use as condition
   * @return the entities having the primary key values as in <code>keys</code>
   */
  public List<Entity> getEntitiesByPrimaryKeys(final List<Entity.Key> keys) {
    final List<Entity> entities = new ArrayList<Entity>();
    for (final Entity entity : getAllEntities()) {
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
    for (final Entity entity : getAllEntities()) {
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
      return getEntityDb().selectDependentEntities(getSelectedEntities());
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the number of currently filtered (hidden) items
   */
  public int getHiddenCount() {
    return hiddenEntities.size();
  }

  /**
   * @param entity the object to search for
   * @param includeHidden set to true if the search should include hidden entities
   * @return true if this table model contains the given object
   */
  public boolean contains(final Entity entity, final boolean includeHidden) {
    final boolean ret = viewIndexOf(entity) >= 0;
    if (!ret && includeHidden)
      return hiddenEntities.indexOf(entity) >= 0;

    return ret;
  }

  /**
   * @return all visible and hidden entities in this table model
   */
  public List<Entity> getAllEntities() {
    return getAllEntities(true);
  }

  /**
   * @param includeHidden if true then filtered entities are included
   * @return all entities in this table model
   */
  public List<Entity> getAllEntities(final boolean includeHidden) {
    final List<Entity> entities = new ArrayList<Entity>(visibleEntities);
    if (includeHidden)
      entities.addAll(hiddenEntities);

    return entities;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return getEntityID();
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
                  evtFilteringDone.addListener(event);//todo summary is updated twice per refresh and should update on insert
                  evtRefreshDone.addListener(event);
                  evtSelectionChangedAdjusting.addListener(event);
                  evtSelectionChanged.addListener(event);
                }

                public Collection<?> getValues() {
                  return EntityTableModel.this.getValues(property, isValueSubset());
                }

                public boolean isValueSubset() {
                  return !stSelectionEmpty.isActive();
                }
              }));

    return propertySummaryModels.get(property.getPropertyID());
  }

  public Property getColumnProperty(final int columnIndex) {
    return (Property) tableColumnModel.getColumn(columnIndex).getIdentifier();
  }

  public TableColumnModel getTableColumnModel() {
    return tableColumnModel;
  }

  /**
   * @return a State active when the selection is empty
   */
  public State stateSelectionEmpty() {
    return stSelectionEmpty;
  }

  /**
   * @return an Event fired when the model has been filtered
   */
  public Event eventFilteringDone() {
    return evtFilteringDone;
  }

  /**
   * @return an Event fired when the model is about to be filtered
   */
  public Event eventFilteringStarted() {
    return evtFilteringStarted;
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
   * @return an event fired when the minimum (topmost) selected index changes (minSelectionIndex property in ListSelectionModel)
   */
  public Event eventSelectedIndexChanged() {
    return evtSelectedIndexChanged;
  }

  /**
   * @return an Event fired after the selection has changed
   */
  public Event eventSelectionChanged() {
    return evtSelectionChanged;
  }

  /**
   * @return an Event fired when the selection is changing
   */
  public Event eventSelectionChangedAdjusting() {
    return evtSelectionChangedAdjusting;
  }

  /**
   * @return an Event fired after the table data has changed
   */
  public Event eventTableDataChanged() {
    return evtTableDataChanged;
  }

  protected TableColumnModel initializeTableColumnModel() {
    final TableColumnModel columnModel = new DefaultTableColumnModel();
    int i = 0;
    for (final Property property : initializeColumnProperties()) {
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
      return getEntityDb().selectMany(new SelectCriteria(getEntityID(), criteria,
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
    return new EntityTableSearchModel(getEntityID(), getTableColumnModel(), getDbProvider(), false);
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
  protected List<Property> initializeColumnProperties() {
    return new ArrayList<Property>(EntityRepository.getVisibleProperties(getEntityID()));
  }

  /**
   * Returns an Iterator which iterates through the selected entities
   * @return the iterator used when generating reports
   * @see #getJRDataSource()
   */
  protected Iterator<Entity> initializeReportIterator() {
    return getSelectedEntities().iterator();
  }

  /**
   * Override to add event bindings
   */
  protected void bindEvents() {}

  /**
   * Adds the given entities to this table model
   * @param entities the entities to add
   * @param atFront if true then the entities are added at the front
   */
  protected void addEntities(final List<Entity> entities, final boolean atFront) {
    for (final Entity entity : entities) {
      if (tableSearchModel.include(entity)) {
        if (atFront)
          visibleEntities.add(0, entity);
        else
          visibleEntities.add(entity);
      }
      else
        hiddenEntities.add(entity);
    }
    fireTableDataChanged();
  }

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

    for (int index = 0; index < tableColumnModel.getColumnCount(); index++)
      if (tableColumnModel.getColumn(index).getModelIndex() == modelColumnIndex)
        return index;

    return -1;
  }

  private int modelIndexOf(final Entity entity) {
    return visibleEntities.indexOf(entity);
  }

  private int viewIndexOf(final Entity entity) {
    return tableSorter.viewIndex(modelIndexOf(entity));
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
    addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent event) {
        evtTableDataChanged.fire();
      }
    });
    tableSorter.eventBeforeSort().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        isSorting = true;
        selectedPrimaryKeys = getPrimaryKeysOfSelectedEntities();
      }
    });
    tableSorter.eventAfterSort().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        setSelectedByPrimaryKeys(selectedPrimaryKeys);
        isSorting = false;
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