/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.DbException;
import org.jminor.common.db.ICriteria;
import org.jminor.common.model.Event;
import org.jminor.common.model.IRefreshable;
import org.jminor.common.model.IntArray;
import org.jminor.common.model.State;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.TableSorter;
import org.jminor.framework.client.model.reporting.EntityJRDataSource;
import org.jminor.framework.db.IEntityDb;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityKey;
import org.jminor.framework.domain.EntityProxy;
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
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * A TableModel implementation for showing entities
 */
public class EntityTableModel extends AbstractTableModel implements IRefreshable {

  private static final Logger log = Util.getLogger(EntityTableModel.class);

  /**
   * Fired when the model is about to be refreshed
   */
  public final Event evtRefreshStarted = new Event();

  /**
   * Fired when the model has been refreshed, N.B. this event
   * is fired even if the refresh results in an exception
   */
  public final Event evtRefreshDone = new Event();

  /**
   * Fired when the model is about to be filtered
   */
  public final Event evtFilteringStarted = new Event();

  /**
   * Fired when the model has been filtered
   */
  public final Event evtFilteringDone = new Event();

  /**
   * Fired after the table data has changed
   */
  public final Event evtTableDataChanged = new Event();

  /**
   * Fired when the minimum (topmost) selected index changes (minSelectionIndex property in ListSelectionModel)
   */
  public final Event evtSelectedIndexChanged = new Event();

  /**
   * Fired when the selection is changing
   */
  public final Event evtSelectionChangedAdjusting = new Event();

  /**
   * Fired after the selection has changed
   */
  public final Event evtSelectionChanged = new Event();

  /**
   * Active when the selection is empty
   */
  public final State stSelectionEmpty = new State("EntityTableModel.stSelectionEmpty", true);

  /**
   * The entity ID
   */
  private final String entityID;

  /**
   * The IEntityDb connection provider
   */
  private final IEntityDbProvider dbProvider;

  /**
   * Holds visible entities
   */
  private final List<Entity> visibleEntities = new ArrayList<Entity>();

  /**
   * Holds entities that are filtered and hidden
   */
  private final List<Entity> filteredEntities = new ArrayList<Entity>();

  /**
   * The properties on which to base the table columns
   */
  private final List<Property> tableColumnProperties;

  /**
   * The search model
   */
  private final EntityTableSearchModel tableSearchModel;

  /**
   * The sorter model
   */
  private final TableSorter tableSorter;

  /**
   * Holds the selected items while sorting
   */
  private List<EntityKey> selectedPrimaryKeys;

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
   * @param dbProvider a IEntityDbProvider instance
   */
  public EntityTableModel(final String entityID, final IEntityDbProvider dbProvider) {
    if (dbProvider == null)
      throw new IllegalArgumentException("dbProvider can not be null");
    if (entityID == null || entityID.length() == 0)
      throw new IllegalArgumentException("entityID must be specified");
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.tableColumnProperties = initializeColumnProperties();
    this.tableSearchModel = initializeSearchModel();
    this.tableSorter = new TableSorter(this);
    bindEvents();
  }

  /**
   * @return the underlying table column properties
   */
  public List<Property> getTableColumnProperties() {
    return tableColumnProperties;
  }

  /**
   * @return true if the underlying query is filtered instead of simply hiding filtered table items
   */
  public boolean isQueryFilteredByMaster() {
    return queryFilteredByMaster;
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
    int idx = 0;
    int columnIndex = -1;
    for (final Property property : EntityRepository.getVisibleProperties(getEntityID())) {
      if (property.propertyID.equals(propertyID)) {
        columnIndex = idx;
        break;
      }
      idx++;
    }
    if (columnIndex == -1)
      throw new RuntimeException("Property not found '" + propertyID + "' for sorting");

    tableSorter.setSortingStatus(columnIndex, status);
  }

  /**
   * @return the IEntityDbConnection provider
   */
  public IEntityDbProvider getDbProvider() {
    return dbProvider;
  }

  /**
   * @return the database connection
   * @throws UserException in case of an exception
   */
  public IEntityDb getEntityDb() throws UserException {
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
  public void selectionUp() {
    if (visibleEntities.size() > 0) {
      if (getSelectionModel().isSelectionEmpty())
        getSelectionModel().setSelectionInterval(visibleEntities.size()-1, visibleEntities.size()-1);
      else {
        final int[] selected = getSelectedViewIndexes();
        final int[] newSelected = new int[selected.length];
        for (int i = 0; i < selected.length; i++)
          newSelected[i] = selected[i] == 0 ? visibleEntities.size()-1 : selected[i] - 1;

        setSelectedItemIndexes(newSelected);
      }
    }
  }

  /**
   * Moves all selected indexes down one index, wraps around
   * @see #evtSelectionChanged
   */
  public void selectionDown() {
    if (visibleEntities.size() > 0) {
      if (getSelectionModel().isSelectionEmpty())
        getSelectionModel().setSelectionInterval(0,0);
      else {
        final int[] selected = getSelectedViewIndexes();
        final int[] newSelected = new int[selected.length];
        for (int i = 0; i < selected.length; i++)
          newSelected[i] = selected[i] == visibleEntities.size()-1 ? 0 : selected[i] + 1;

        setSelectedItemIndexes(newSelected);
      }
    }
  }

  /**
   * Selects all visible entities
   * @see #evtSelectionChanged
   */
  public void selectAll() {
    getSelectionModel().setSelectionInterval(0, visibleEntities.size()-1);
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
    removeAll();
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
  public Class getColumnClass(final int columnIndex) {
    return getValueClass(tableColumnProperties.get(columnIndex).getPropertyType(),
            getEntityAtViewIndex(0).getValue(tableColumnProperties.get(columnIndex).propertyID));
  }

  /** {@inheritDoc} */
  public int getColumnCount() {
    return tableColumnProperties.size();
  }

  /** {@inheritDoc} */
  public int getRowCount() {
    return visibleEntities.size();
  }

  /** {@inheritDoc} */
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    return visibleEntities.get(rowIndex).getTableValue(tableColumnProperties.get(columnIndex));
  }

  /**
   * @param row the row for which to retrieve the background color
   * @return the background color for this row, specified by the row entity
   * @see org.jminor.framework.domain.EntityProxy#getBackgroundColor(org.jminor.framework.domain.Entity)
   * @see org.jminor.framework.client.ui.EntityTableCellRenderer
   */
  public Color getRowBackgroundColor(final int row) {
    final Entity rowEntity = getEntityAtViewIndex(row);

    return EntityProxy.getEntityProxy(rowEntity.getEntityID()).getBackgroundColor(rowEntity);
  }

  /**
   * @param property the property for which to retrieve the values
   * @param selectedOnly if true only values from the selected entities are returned
   * @return the values of <code>property</code> from the entities in the table model
   */
  public Collection<Object> getValues(final Property property, final boolean selectedOnly) {
    return Arrays.asList(EntityUtil.getPropertyValues(property.propertyID,
            selectedOnly ? getSelectedEntities() : visibleEntities, false));
  }

  /**
   * @param primaryKey the primary key to search by
   * @return the entity with the given primary key from the table model, null if it's not found
   */
  public Entity getEntityByPrimaryKey(final EntityKey primaryKey) {
    for (final Entity entity : visibleEntities)
      if (entity.getPrimaryKey().equals(primaryKey))
        return entity;

    return null;
  }

  public int getViewIndexByPrimaryKey(final EntityKey primaryKey) {
    return viewIndexOf(getEntityByPrimaryKey(primaryKey));
  }

  /**
   * @return a String describing the selected/filtered state of this table model
   */
  public String getStatusMessage() {
    final int filteredCount = getFilteredCount();

    return new StringBuilder(Integer.toString(getRowCount())).append(" (").append(
            Integer.toString(getSelectedModelIndexes().length)).append(" ").append(
            FrameworkMessages.get(FrameworkMessages.SELECTED)).append(
            filteredCount > 0 ? ", " + filteredCount + " "
                    + FrameworkMessages.get(FrameworkMessages.HIDDEN) + ")" : ")").toString();
  }

  public int[] getSelectedViewIndexes() {
    final IntArray ret = new IntArray();
    final int min = selectionModel.getMinSelectionIndex();
    final int max = selectionModel.getMaxSelectionIndex();
    for (int i = min; i <= max; i++)
      if (selectionModel.isSelectedIndex(i))
        ret.add(i);

    return ret.toIntArray();
  }

  public int[] getSelectedModelIndexes() {
    final IntArray ret = new IntArray();
    final int min = selectionModel.getMinSelectionIndex();
    final int max = selectionModel.getMaxSelectionIndex();
    if (min >= 0 && max >= 0) {
      for (int i = min; i <= max; i++)
        if (selectionModel.isSelectedIndex(i))
          ret.add(tableSorter.modelIndex(i));
    }

    return ret.toIntArray();
  }

  /**
   * Refreshes this table model
   * @throws UserException in case of an exception
   * @see #evtRefreshStarted
   * @see #evtRefreshDone
   */
  public void refresh() throws UserException {
    if (isRefreshing)
      return;

    try {
      log.trace(this + " refreshing");
      isRefreshing = true;
      evtRefreshStarted.fire();
      final List<EntityKey> selectedPrimaryKeys = getPrimaryKeysOfSelectedEntities();
      removeAll();
      addEntities(performQuery(getQueryCriteria()), false);
      setSelectedByPrimaryKeys(selectedPrimaryKeys);
    }
    catch (DbException e) {
      throw new UserException(e);
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
   * @throws UserException in case of an exception
   * @throws DbException in case of a database exception
   */
  public void addEntitiesByPrimaryKeys(final List<EntityKey> primaryKeys, boolean atFrontOfList)
          throws UserException, DbException {
    try {
      addEntities(getEntityDb().selectMany(primaryKeys), atFrontOfList);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new UserException(e);
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

    for (final Entity entity : filteredEntities) {
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
      index = filteredEntities.indexOf(entity);
      if (index >= 0)
        filteredEntities.remove(index);
    }
  }

  /**
   * @param index the index
   * @return the Entity at <code>index</code>
   */
  public Entity getEntityAtViewIndex(final int index) {
    if (index >= 0 && index < visibleEntities.size())
      return visibleEntities.get(tableSorter.modelIndex(index));

    throw new ArrayIndexOutOfBoundsException("No visible entity found at index: "
            + index + ", size: " + visibleEntities.size());
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
      final List<EntityKey> selectedPrimaryKeys = getPrimaryKeysOfSelectedEntities();
      visibleEntities.addAll(filteredEntities);
      filteredEntities.clear();
      for (final ListIterator<Entity> iterator = visibleEntities.listIterator(); iterator.hasNext();) {
        final Entity entity = iterator.next();
        if (!tableSearchModel.include(entity)) {
          filteredEntities.add(entity);
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

  public void filterByReference(final List<Entity> referenceEntities, final String referencedEntityID) throws UserException {
    if (isQueryFilteredByMaster()) {
      if (tableSearchModel.setExactSearchValue(referencedEntityID, referenceEntities))
        refresh();
    }
    else {
      tableSearchModel.setExactFilterValue((referenceEntities == null || referenceEntities.size() == 0)
              ? null : referenceEntities.get(0).toString(), getColumnIndex(referencedEntityID));
    }
  }

  /**
   * @return a list containing the primary keys of the selected entities,
   * if none are selected an empty list is returned
   */
  public List<EntityKey> getPrimaryKeysOfSelectedEntities() {
    return EntityUtil.getPrimaryKeys(getSelectedEntities());
  }

  /**
   * @return a list containing the selected entities
   */
  public List<Entity> getSelectedEntities() {
    final int[] selectedModelIndexes = getSelectedModelIndexes();
    final List<Entity> ret = new ArrayList<Entity>();
    for (final int modelIndex : selectedModelIndexes)
      ret.add(visibleEntities.get(modelIndex));

    return ret;
  }

  /**
   * Selects the given entities
   * @param entities the entities to select
   */
  public void setSelectedEntities(final List<Entity> entities) {
    final IntArray indexArray = new IntArray();
    for (final Entity entity : entities) {
      final int index = viewIndexOf(entity);
      if (index >= 0)
        indexArray.add(index);
    }

    setSelectedItemIndexes(indexArray.toIntArray());
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
  public void setSelectedByPrimaryKeys(final List<EntityKey> primaryKeys) {
    final IntArray indexArray = new IntArray(primaryKeys.size());
    for (final Entity visibleEntity : visibleEntities) {
      final int index = primaryKeys.indexOf(visibleEntity.getPrimaryKey());
      if (index >= 0) {
        indexArray.add(viewIndexOf(visibleEntity));
        primaryKeys.remove(index);
      }
    }

    setSelectedItemIndexes(indexArray.toIntArray());
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
  public void setSelectedItemIndexes(final int[] indexes) {
    selectionModel.clearSelection();
    addSelectedItemIndexes(indexes);
  }

  /**
   * Adds these indexes to the selection
   * @param indexes the indexes to add to the selection
   */
  public void addSelectedItemIndexes(final int[] indexes) {
    try {
      isUpdatingSelection = true;
      for (int i = 0; i < indexes.length-1; i++)
        selectionModel.addSelectionInterval(indexes[i], indexes[i]);
    }
    finally {
      isUpdatingSelection = false;
      if (indexes.length > 0)
        selectionModel.addSelectionInterval(indexes[indexes.length-1], indexes[indexes.length-1]);
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
  public List<Entity> getEntitiesByPrimaryKeys(final List<EntityKey> keys) {
    final List<Entity> ret = new ArrayList<Entity>();
    final List<Entity> allEntities = new ArrayList<Entity>(visibleEntities.size() + filteredEntities.size());
    allEntities.addAll(visibleEntities);
    allEntities.addAll(filteredEntities);
    for (final Entity entity : allEntities) {
      for (final EntityKey key : keys) {
        if (entity.getPrimaryKey().equals(key)) {
          ret.add(entity);
          break;
        }
      }
    }

    return ret;
  }

  /**
   * Finds entities according to the values of <code>propertyValues</code>
   * @param propertyValues the property values to use as condition mapped
   * to their respective propertyIDs
   * @return the entities having the exact same property values as in <code>properties</properties>
   */
  public Entity[] getEntitiesByPropertyValues(final Map<String, Object> propertyValues) {
    final List<Entity> ret = new ArrayList<Entity>();
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
        ret.add(entity);
    }

    return ret.toArray(new Entity[ret.size()]);
  }

  /**
   * @return a Map containing all entities which depend on the selected entities,
   * where the keys are entityIDs and the value is an array of entities of that type
   * @throws DbException in case of a database exception
   * @throws UserException in case of a user exception
   */
  public final Map<String, List<Entity>> getSelectionDependencies() throws DbException, UserException {
    try {
      return getEntityDb().selectDependentEntities(getSelectedEntities());
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  /**
   * @return the number of currently filtered (hidden) items
   */
  public int getFilteredCount() {
    return filteredEntities.size();
  }

  /**
   * @param entity the object to search for
   * @param includeFiltered set to true if the filtered objects should
   * be included in the search
   * @return true if this table model contains the given object
   */
  public boolean contains(final Entity entity, final boolean includeFiltered) {
    final boolean ret = viewIndexOf(entity) >= 0;
    if (!ret && includeFiltered)
      return filteredEntities.indexOf(entity) >= 0;

    return ret;
  }

  /**
   * @return all filtered and non-filtered entities in this table model
   */
  public List<Entity> getAllEntities() {
    return getAllEntities(true);
  }

  /**
   * @param includeFiltered if true then filtered entities are included
   * @return all entities in this table model
   */
  public List<Entity> getAllEntities(final boolean includeFiltered) {
    final List<Entity> ret = new ArrayList<Entity>(visibleEntities);
    if (includeFiltered)
      ret.addAll(filteredEntities);

    return ret;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return getEntityID();
  }

  /**
   * Queries for the data used to populate this EntityTableModel when it is refreshed
   * @param criteria a criteria
   * @return entities selected from the database according the the query criteria.
   * @throws UserException in case of an exception
   * @throws DbException in case of a database exception
   */
  protected List<Entity> performQuery(final ICriteria criteria) throws DbException, UserException {
    if (isQueryFilteredByMaster() && criteria == null && !isShowAllWhenNotFiltered())
      return new ArrayList<Entity>();

    try {
      return getEntityDb().selectMany(new EntityCriteria(getEntityID(), criteria,
              EntityRepository.getOrderByClause(getEntityID())));
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  /**
   * @return a list containing the properties for which this EntityTableModel
   * should provide PropertySearchModels, the default implementation returns all
   * table column based properties which are visible in this table model
   */
  protected List<Property> getSearchableProperties() {
    final List<Property> ret = new ArrayList<Property>();
    for (final Property property : EntityRepository.getProperties(getEntityID(), false))
      if (property.isDatabaseProperty())
        ret.add(property);

    return ret;
  }

  /**
   * Initializes a EntityTableSearchModel based on the properties constituting this EntityTableModel
   * @return a EntityTableSearchModel for this EntityTableModel
   */
  protected EntityTableSearchModel initializeSearchModel() {
    return new EntityTableSearchModel(getEntityID(), tableColumnProperties, getSearchableProperties(), getDbProvider());
  }

  /**
   * @return a ICriteria object used to filter the result when this
   * table models data is queried, the default implementation returns
   * the result retrieved via the <code>getSearchCriteria()</code> method
   * found in the underlying EntityTableSearchModel
   * @see org.jminor.framework.client.model.EntityTableSearchModel#getSearchCriteria()
   */
  protected ICriteria getQueryCriteria() {
    return tableSearchModel.getSearchCriteria();
  }

  /**
   * Removes all elements from this table model
   */
  protected void removeAll() {
    filteredEntities.clear();
    final int size = getRowCount();
    if (size > 0) {
      visibleEntities.clear();
      fireTableRowsDeleted(0, size - 1);
    }
  }

  /**
   * @return a list of Properties that should be used as basis for this table models column model
   */
  protected List<Property> initializeColumnProperties() {
    return EntityRepository.getVisiblePropertyList(getEntityID());
  }

  /**
   * Returns an Iterator which iterates through the selected entities
   * @return the iterator used when generating reports
   * @see #getJRDataSource()
   */
  protected Iterator<Entity> initializeReportIterator() {
    return getSelectedEntities().iterator();
  }

  protected void bindEvents() {
    tableSearchModel.evtFilterStateChanged.addListener(new ActionListener() {
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
    tableSorter.evtBeforeSort.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        isSorting = true;
        selectedPrimaryKeys = getPrimaryKeysOfSelectedEntities();
      }
    });
    tableSorter.evtAfterSort.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        setSelectedByPrimaryKeys(selectedPrimaryKeys);
        isSorting = false;
      }
    });
  }

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
        filteredEntities.add(entity);
    }
    fireTableDataChanged();
  }

  private int getColumnIndex(final String masterEntityID) {
    for (int i = 0; i < tableColumnProperties.size(); i++)
      if (tableColumnProperties.get(i) instanceof Property.ForeignKeyProperty
              && ((Property.ForeignKeyProperty) tableColumnProperties.get(i)).referenceEntityID.equals(masterEntityID))
        return i;

    return -1;
  }

  private int modelIndexOf(final Entity entity) {
    return visibleEntities.indexOf(entity);
  }

  private int viewIndexOf(final Entity entity) {
    return tableSorter.viewIndex(modelIndexOf(entity));
  }

  private static Class getValueClass(final Type type, final Object value) {
    if (type == Type.INT)
      return Integer.class;
    if (type == Type.DOUBLE)
      return Double.class;
    if (type == Type.BOOLEAN)
      return Type.Boolean.class;
    if (type == Type.DATE || type == Type.TIMESTAMP)
      return Date.class;
    if (type == Type.CHAR)
      return Character.class;
    if (type == Type.ENTITY)
      return Entity.class;

    return value == null ? Object.class : value.getClass();
  }
}