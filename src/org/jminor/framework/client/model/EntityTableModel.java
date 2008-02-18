/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import org.apache.log4j.Logger;
import org.jminor.common.db.CriteriaSet;
import org.jminor.common.db.DbException;
import org.jminor.common.db.ICriteria;
import org.jminor.common.db.TableStatus;
import org.jminor.common.model.Event;
import org.jminor.common.model.IRefreshable;
import org.jminor.common.model.IntArray;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.TableSorter;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityCriteria;
import org.jminor.framework.model.EntityKey;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.EntityUtil;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.PropertyCriteria;
import org.jminor.framework.model.Type;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class EntityTableModel extends AbstractTableModel implements IRefreshable, JRDataSource {

  private static final Logger log = Util.getLogger(EntityTableModel.class);

  /**
   * The default query range
   */
  public static final int DEFAULT_QUERY_RANGE = 1000;

  /**
   * Fired when the model is about to be refreshed
   */
  public final Event evtRefreshStarted = new Event("EntityTableModel.evtRefreshStarted");

  /**
   * Fired when the model has been refreshed, N.B. this event
   * is fired even if the refresh results in an exception
   */
  public final Event evtRefreshDone = new Event("EntityTableModel.evtRefreshDone");

  /**
   * Fired when the model is about to be filtered
   */
  public final Event evtFilteringStarted = new Event("EntityTableModel.evtFilteringStarted");

  /**
   * Fired when the model has been filtered
   */
  public final Event evtFilteringDone = new Event("EntityTableModel.evtFilteringDone");

  /**
   * Fired after the table data has changed
   */
  public final Event evtTableDataChanged = new Event("EntityTableModel.evtTableDataChanged");

  /**
   * Fired when the minimum (topmost) selected index changes (minSelectionIndex property in ListSelectionModel)
   */
  public final Event evtSelectedIndexChanged = new Event("EntityTableModel.evtSelectedIndexChanged");

  /**
   * Fired when the selection is changing
   */
  public final Event evtSelectionChangedAdjusting = new Event("EntityTableModel.evtSelectionChangedAdjusting");

  /**
   * Fired after the selection has changed
   */
  public final Event evtSelectionChanged = new Event("EntityTableModel.evtSelectionChanged");

  /**
   * Fired when the simple search string has changed
   * @see #setSimpleSearchString(String)
   */
  public final Event evtSimpleSearchStringChanged = new Event("EntityTableModel.evtSimpleSearchStringChanged");

  /**
   * Active when the selection is empty
   */
  public final State stSelectionEmpty = new State("EntityTableModel.stSelectionEmpty", true);

  /**
   * Active when the data does not represent the state of the property search models
   */
  public final State stDataDirty = new State("EntityTableModel.stDataDirty", false);

  /**
   * If set to true, then a full refresh is forced when <code>refresh()</code> is called,
   * regardless if the table data has changed or not
   */
  private boolean forceRefresh = false;

  /**
   * true while the model is refreshing
   */
  private boolean isRefreshing = false;

  /**
   * true while the model is filtering
   */
  private boolean isFiltering = false;

  /**
   * true while the model is sorting
   */
  private boolean isSorting = false;

  /**
   * False if the table should ignore the query range when refreshing
   */
  private boolean queryRangeEnabled = FrameworkSettings.get().useQueryRange;

  /**
   * Represents the range of records to select from the underlying table
   */
  private final PropertyCriteria queryRangeCriteria =
          new PropertyCriteria(new Property("row_num", Type.INT), SearchType.INSIDE, 1, DEFAULT_QUERY_RANGE);

  /**
   * Holds the status of the underlying table
   */
  private final TableStatus tableStatus = new TableStatus();

  /**
   * The IEntityDb connection provider
   */
  private final IEntityDbProvider dbConnectionProvider;

  /**
   * The entity ID
   */
  private final String entityID;

  /**
   * Holds visible entities
   */
  private final List<Entity> visibleEntities = new ArrayList<Entity>();

  /**
   * Holds entities that are filtered and hidden
   */
  private final List<Entity> filteredEntities = new ArrayList<Entity>();

  /**
   * Holds the selected items while sorting
   */
  private List<EntityKey> selectedPrimaryKeys;

  /**
   * Holds the topmost (minimum) selected index
   */
  private int minSelectedIndex = -1;

  private final DefaultListSelectionModel selectionModel = new DefaultListSelectionModel() {
    public void fireValueChanged(int min, int max, boolean isAdjusting) {
      super.fireValueChanged(min, max, isAdjusting);
      stSelectionEmpty.setActive(isSelectionEmpty());
      final int minSelIndex = getMinSelectionIndex();
      if (minSelectedIndex != minSelIndex) {
        minSelectedIndex = minSelIndex;
        evtSelectedIndexChanged.fire();
      }
      if (isAdjusting || updatingSelection)
        evtSelectionChangedAdjusting.fire();
      else
        evtSelectionChanged.fire();
    }
  };

  private final List<Property> tableColumnProperties;
  private final TableSorter tableSorter;
  private final List<PropertyFilterModel> propertyFilterModels;
  private final List<PropertySearchModel> propertySearchModels;
  private final Map<Property, EntityComboBoxModel> propertySearchComboBoxModels =
          new HashMap<Property, EntityComboBoxModel>();

  //reporting
  private Iterator<Entity> reportPrintIterator;
  private Entity currentReportRecord;

  private String simpleSearchString = "";
  private long searchStateOnRefresh;
  private boolean filterQueryByMaster = false;
  private boolean showAllWhenNotFiltered = false;
  private boolean updatingSelection = false;

  /**
   * Initializes a new EntityTableModel
   * @param dbProvider a IEntityDbProvider instance
   * @param entityID the ID of the entity this table model should represent
   */
  public EntityTableModel(final IEntityDbProvider dbProvider, final String entityID) {
    this.dbConnectionProvider = dbProvider;
    this.entityID = entityID;
    this.tableColumnProperties = initColumnProperties();
    this.propertySearchModels = initPropertySearchModels(getSearchableProperties());
    this.searchStateOnRefresh = getSearchModelState();
    this.tableSorter = new TableSorter(this);
    this.propertyFilterModels = initPropertyFilters();
    bindEvents();
  }

  /**
   * @return Value for property 'tableColumnProperties'.
   */
  public List<Property> getTableColumnProperties() {
    return tableColumnProperties;
  }

  /**
   * @return Value for property 'filterQueryByMaster'.
   */
  public boolean isFilterQueryByMaster() {
    return filterQueryByMaster;
  }

  /**
   * @param filterQueryByMaster Value to set for property 'filterQueryByMaster'.
   */
  public void setFilterQueryByMaster(final boolean filterQueryByMaster) {
    this.filterQueryByMaster = filterQueryByMaster;
  }

  /**
   * @return Value for property 'showAllWhenNotFiltered'.
   */
  public boolean isShowAllWhenNotFiltered() {
    return showAllWhenNotFiltered;
  }

  /**
   * @param showAllWhenNotFiltered Value to set for property 'showAllWhenNotFiltered'.
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
   * @param propertyID the ID of the property to sort by
   * @param status the sorting status, use TableSorter.DESCENDING, .NOT_SORTED, .ASCENDING
   */
  public void setSortingStatus(final String propertyID, final int status) {
    int idx = 0;
    int columnIndex = -1;
    for (final Property property : EntityRepository.get().getVisibleProperties(getEntityID())) {
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
   * @return Value for property 'dbConnectionProvider'.
   */
  public IEntityDbProvider getDbConnectionProvider() {
    return dbConnectionProvider;
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
   * @return Value for property 'tableStatus'.
   */
  public TableStatus getTableStatus() {
    return tableStatus;
  }

  /**
   * @return Value for property 'refreshing'.
   */
  public boolean isRefreshing() {
    return isRefreshing;
  }

  /**
   * @return Value for property 'simpleSearchString'.
   */
  public String getSimpleSearchString() {
    return simpleSearchString;
  }

  /**
   * @param value Value to set for property 'simpleSearchString'.
   * @see #evtSimpleSearchStringChanged
   */
  public void setSimpleSearchString(final String value) {
    simpleSearchString = value;
    evtSimpleSearchStringChanged.fire();
  }

  /**
   * @return an initialized JRDataSource
   */
  public JRDataSource getJRDataSource() {
    reportPrintIterator = initReportPrintIterator();

    return this;
  }

  /**
   * Implementing JRDataSource.getFieldValue(JRField jrField)
   * Gets the field value for the current position.
   * @return an object containing the field value. The object type must be the field object type.
   */
  public Object getFieldValue(final JRField jrField) throws JRException {
    return currentReportRecord.getTableValue(EntityRepository.get().getProperty(getEntityID(), jrField.getName()));
  }

  /**
   * Implementing JRDataSource.next()
   * Tries to position the cursor on the next element in the data source.
   * @return true if there is a next record, false otherwise
   * @throws JRException if any error occurs while trying to move to the next element
   */
  public boolean next() throws JRException {
    if (reportPrintIterator.hasNext()) {
      currentReportRecord = reportPrintIterator.next();
      return true;
    }
    else {
      reportPrintIterator = null;
      currentReportRecord = null;
      return false;
    }
  }

  /** {@inheritDoc} */
  public boolean isCellEditable(final int row, final int column) {
    return false;
  }

  /** {@inheritDoc} */
  public Class getColumnClass(final int columnIndex) {
    return EntityUtil.getValueClass(tableColumnProperties.get(columnIndex).getPropertyType(),
            getEntityAtViewIndex(0).getValue(tableColumnProperties.get(columnIndex).propertyID));
  }

  /** {@inheritDoc} */
  public synchronized int getColumnCount() {
    return tableColumnProperties.size();
  }

  /** {@inheritDoc} */
  public synchronized int getRowCount() {
    return visibleEntities.size();
  }

  /** {@inheritDoc} */
  public synchronized Object getValueAt(final int rowIndex, final int columnIndex) {
    return visibleEntities.get(rowIndex).getTableValue(tableColumnProperties.get(columnIndex));
  }

  /**
   * @param row the row for which to retrieve the background color
   * @return the background color for this row, specified by the row entity
   * @see org.jminor.framework.model.EntityProxy
   */
  public Color getRowBackgroundColor(final int row) {
    final Entity rowEntity = getEntityAtViewIndex(row);

    return EntityRepository.get().getEntityProxy(rowEntity.getEntityID()).getBackgroundColor(rowEntity);
  }

  public Collection<Object> getValues(final Property property, final boolean selectedOnly) {
    return Arrays.asList(EntityUtil.getPropertyValue(property.propertyID,
            (selectedOnly && !stSelectionEmpty.isActive()) ? getSelectedEntities() : visibleEntities, false));
  }

  public Entity getEntityByPrimaryKey(final EntityKey primaryKey) {
    for (final Entity entity : visibleEntities) {
      if (entity.getPrimaryKey().equals(primaryKey))
        return entity;
    }

    return null;
  }

  public int getViewIndexByPrimaryKey(final EntityKey primaryKey) {
    return viewIndexOf(getEntityByPrimaryKey(primaryKey));
  }

  public String getStatusMessage() {
    final int filteredCount = getFilteredCount();

    return new StringBuffer(Integer.toString(getRowCount())).append(" (").append(
            Integer.toString(getSelectedModelIndexes().length)).append(" ").append(
            FrameworkMessages.get(FrameworkMessages.SELECTED)).append(
            filteredCount > 0 ? ", " + filteredCount + " "
                    + FrameworkMessages.get(FrameworkMessages.HIDDEN) + ")" : ")").append(
            queryRangeEnabled ? getRangeDescription() : "").toString();
  }

  public int[] getSelectedViewIndexes() {
    final IntArray ret = new IntArray();
    final int min = selectionModel.getMinSelectionIndex();
    final int max = selectionModel.getMaxSelectionIndex();
    for (int i = min; i <= max; i++)
      if (selectionModel.isSelectedIndex(i))
        ret.addInt(i);

    return ret.toIntArray();
  }

  public int[] getSelectedModelIndexes() {
    final IntArray ret = new IntArray();
    final int min = selectionModel.getMinSelectionIndex();
    final int max = selectionModel.getMaxSelectionIndex();
    if (min >= 0 && max >= 0) {
      for (int i = min; i <= max; i++)
        if (selectionModel.isSelectedIndex(i))
          ret.addInt(tableSorter.modelIndex(i));
    }

    return ret.toIntArray();
  }

  /**
   * @param val Value to set for property 'forceRefresh'.
   */
  public void setForceRefresh(boolean val) {
    this.forceRefresh = val;
  }

  /**
   * Forces a refresh of this table model, disregarding the status of the underlying table
   * @throws UserException in case of an exception
   * @see #evtRefreshStarted
   * @see #evtRefreshDone
   */
  public void forceRefresh() throws UserException {
    try {
      setForceRefresh(true);
      refresh();
    }
    finally {
      setForceRefresh(false);
    }
  }

  /**
   * Refreshes this table model
   * @throws UserException in case of an exception
   * @see #evtRefreshStarted
   * @see #evtRefreshDone
   */
  public synchronized void refresh() throws UserException {
    if (isRefreshing)
      return;

    try {
      log.trace(this + " refreshing" + (forceRefresh ? " (forced)" : ""));
      isRefreshing = true;
      evtRefreshStarted.fire();
      if (FrameworkSettings.get().useSmartRefresh && !isRefreshRequired()) {
        log.trace(this + " refresh not required");
        return;
      }
      final List<EntityKey> selectedPrimaryKeys = getPrimaryKeysOfSelectedEntities();
      removeAll();
      addEntities(getAllEntitiesFromDb(), false);
      setSelectedByPrimaryKeys(selectedPrimaryKeys);
    }
    catch (UserException ue) {
      throw ue;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
    finally {
      isRefreshing = false;
      evtRefreshDone.fire();
      log.trace(this + " refreshing done" + (forceRefresh ? " (forced)" : ""));
    }
  }

  public void refreshSearchComboBoxModels() {
    try {
      for (final EntityComboBoxModel model : propertySearchComboBoxModels.values())
        model.refresh();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void clearSearchComboBoxModels() {
    for (final EntityComboBoxModel model : propertySearchComboBoxModels.values())
      model.clear();
  }

  public void clearSearchState() {
    for (final AbstractSearchModel searchModel : propertySearchModels)
      searchModel.clear();
  }

  public void addEntitiesByPrimaryKeys(final List<EntityKey> primaryKeys, boolean atFrontOfList)
          throws UserException, DbException {
    try {
      addEntities(getDbConnectionProvider().getEntityDb().selectMany(primaryKeys), atFrontOfList);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  public synchronized void replaceEntities(final List<Entity> entities) {
    replaceEntities(entities.toArray(new Entity[entities.size()]));
  }

  public synchronized void replaceEntities(final Entity... entities) {
    for (int i = 0; i < visibleEntities.size(); i++) {
      final Entity entity = visibleEntities.get(i);
      for (final Entity newEntity : entities)
        if (entity.getPrimaryKey().equals(newEntity.getPrimaryKey())) {
          entity.setAs(newEntity);
          final int idx = visibleEntities.indexOf(entity);
          fireTableRowsUpdated(idx, idx);
        }
    }

    for (final Entity entity : filteredEntities) {
      for (final Entity newEntity : entities)
        if (entity.getPrimaryKey().equals(newEntity.getPrimaryKey()))
          entity.setAs(newEntity);
    }
  }

  public IntArray removeEntitiesByPrimaryKey(final List<EntityKey> primaryKeys) {
    final IntArray ret = new IntArray(primaryKeys.size());
    removeEntities(getEntitiesByPrimaryKeys(primaryKeys, ret));

    return ret;
  }

  public void removeEntities(final List<Entity> entities) {
    for (final Entity entity : entities)
      removeEntity(entity);
  }

  public synchronized void removeEntity(final Entity entity) {
    int idx = visibleEntities.indexOf(entity);
    if (idx >= 0) {
      visibleEntities.remove(idx);
      fireTableRowsDeleted(idx, idx);
    }
    else {
      idx = filteredEntities.indexOf(entity);
      if (idx >= 0)
        filteredEntities.remove(idx);
    }
  }

  /**
   * @param idx the index
   * @return the Entity at index <code>idx</code>
   */
  public synchronized Entity getEntityAtViewIndex(final int idx) {
    if (idx >= 0 && idx < visibleEntities.size())
      return visibleEntities.get(tableSorter.modelIndex(idx));

    throw new ArrayIndexOutOfBoundsException("No visible entity found at index: "
            + idx + ", size: " + visibleEntities.size());
  }

  /**
   * Filters this table model
   * @see #evtFilteringStarted
   * @see #evtFilteringDone
   */
  public synchronized void filterTable() {
    if (propertyFilterModels == null)
      return;

    try {
      isFiltering = true;
      evtFilteringStarted.fire();
      final List<EntityKey> selectedPrimaryKeys = getPrimaryKeysOfSelectedEntities();
      visibleEntities.addAll(filteredEntities);
      filteredEntities.clear();
      for (final ListIterator<Entity> iterator = visibleEntities.listIterator(); iterator.hasNext();) {
        final Entity entity = iterator.next();
        if (!include(entity)) {
          filteredEntities.add(entity);
          iterator.remove();
        }
      }
      fireTableChanged(new TableModelEvent(this, 0, Integer.MAX_VALUE, -1, 2));
      setSelectedByPrimaryKeys(selectedPrimaryKeys);
    }
    finally {
      isFiltering = false;
      evtFilteringDone.fire();
    }
  }

  public void filterByReference(final List<Entity> referenceEntities, final String entityID)
          throws UserException {
    if (filterQueryByMaster)
      setExactSearchValue(entityID, referenceEntities);
    else
      setExactFilterValue((referenceEntities == null || referenceEntities.size() == 0)
              ? null : referenceEntities.get(0).toString(), getColumnIndex(entityID));
  }

  public void setExactSearchValue(final String entityID, final List<Entity> referenceEntities) throws UserException {
    final PropertySearchModel searchModel = getPropertySearchModel(
            EntityRepository.get().getEntityProperty(getEntityID(), entityID).propertyID);
    if (searchModel != null) {
      searchModel.initialize();
      searchModel.setSearchEnabled(referenceEntities != null && referenceEntities.size() > 0);
      searchModel.setUpperBound((Object) null);//because the upperBound is a reference to the active entity and changes accordingly
      searchModel.setUpperBound(referenceEntities != null && referenceEntities.size() == 0 ? null : referenceEntities);//this then failes to register a changed upper bound
      forceRefresh();
    }
  }

  public List<PropertySearchModel> getPropertySearchModels() {
    return propertySearchModels;
  }

  public PropertySearchModel getPropertySearchModel(final String propertyID) {
    for (final PropertySearchModel searchModel : propertySearchModels)
      if (searchModel.getProperty().propertyID.equals(propertyID))
        return searchModel;

    return null;
  }

  public boolean isSearchEnabled(final int columnIdx) {
    final PropertySearchModel model =
            getPropertySearchModel(EntityRepository.get().getPropertyAtViewIndex(getEntityID(), columnIdx).propertyID);

    return model != null && model.stSearchEnabled.isActive();
  }

  public boolean isFilterEnabled(final int columnIndex) {
    return getPropertyFilterModel(columnIndex).isSearchEnabled();
  }

  public void setExactFilterValue(final Comparable value, final int columnIndex) {
    if (columnIndex >= 0)
      getPropertyFilterModel(columnIndex).setExactValue(value);
  }

  /**
   * Returns the property filter at index <code>idx</code>
   * @param idx the property index
   * @return the property filter
   */
  public PropertyFilterModel getPropertyFilterModel(final int idx) {
    return propertyFilterModels.get(idx);
  }

  public PropertyFilterModel getPropertyFilterModel(final String propertyID) {
    for (final AbstractSearchModel filter : propertyFilterModels) {
      if (filter.getColumnName().equals(propertyID))
        return (PropertyFilterModel) filter;
    }

    return null;
  }

  public List<EntityKey> getPrimaryKeysOfSelectedEntities() {
    return EntityUtil.getPrimaryKeys(getSelectedEntities());
  }

  public List<Entity> getSelectedEntities() {
    final int[] selectedModelIndexes = getSelectedModelIndexes();
    final ArrayList<Entity> ret = new ArrayList<Entity>();
    for (final int modelIndex : selectedModelIndexes)
      ret.add(visibleEntities.get(modelIndex));

    return ret;
  }

  public void setSelectedEntities(final List<Entity> entities) {
    final IntArray indexArray = new IntArray();
    for (final Entity entity : entities) {
      final int idx = viewIndexOf(entity);
      if (idx >= 0)
        indexArray.addInt(idx);
    }

    setSelectedItemIndexes(indexArray.toIntArray());
  }

  /**
   * @return the selected entity, null if none is selected
   */
  public synchronized Entity getSelectedEntity() {
    final int idx = selectionModel.getMinSelectionIndex();
    if (idx >= 0 && idx < visibleEntities.size())
      return getEntityAtViewIndex(idx);
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
      final int idx = primaryKeys.indexOf(visibleEntity.getPrimaryKey());
      if (idx >= 0) {
        indexArray.addInt(viewIndexOf(visibleEntity));
        primaryKeys.remove(idx);
      }
    }

    setSelectedItemIndexes(indexArray.toIntArray());
  }

  /**
   * Clears the selection and selects the entity at index <code>idx</code>
   * @param idx the index
   */
  public void setSelectedItemIdx(final int idx) {
    selectionModel.setSelectionInterval(idx, idx);
  }

  /**
   * Selects the entity at index <code>idx</code>
   * @param idx the index
   */
  public void addSelectedItemIdx(final int idx) {
    selectionModel.addSelectionInterval(idx, idx);
  }

  public void setSelectedItemIndexes(final int[] indexes) {
    selectionModel.clearSelection();
    addSelectedItemIndexes(indexes);
  }

  public void addSelectedItemIndexes(final int[] indexes) {
    try {
      updatingSelection = true;
      for (int i = 0; i < indexes.length-1; i++)
        selectionModel.addSelectionInterval(indexes[i], indexes[i]);
    }
    finally {
      updatingSelection = false;
      if (indexes.length > 0)
        selectionModel.addSelectionInterval(indexes[indexes.length-1], indexes[indexes.length-1]);
    }
  }

  public int getSelectedIndex() {
    return minSelectedIndex;
  }

  /**
   * Finds entities according to the values in <code>keys</code>
   * @param keys the primary key values to use as condition
   * @return the entities having the primary key values as in <code>keys</code>
   */
  public List<Entity> getEntitiesByPrimaryKeys(final List<EntityKey> keys) {
    return getEntitiesByPrimaryKeys(keys, null);
  }

  /**
   * Finds entities according to the values in <code>keys</code>
   * @param keys the primary key values to use as condition
   * @param indexes this IntArray will contain the indexes of the returned entities
   * @return the entities having the primary key values as in <code>keys</code>
   */
  public List<Entity> getEntitiesByPrimaryKeys(final List<EntityKey> keys, final IntArray indexes) {
    final List<Entity> ret = new ArrayList<Entity>();
    final List<Entity> allEntities = new ArrayList<Entity>(visibleEntities.size() + filteredEntities.size());
    allEntities.addAll(visibleEntities);
    allEntities.addAll(filteredEntities);
    for (int i = 0; i < allEntities.size(); i++) {
      final Entity entity = allEntities.get(i);
      for (final EntityKey key : keys) {
        if (entity.getPrimaryKey().equals(key)) {
          ret.add(entity);
          if (indexes != null && i < visibleEntities.size())
            indexes.addInt(i);
          break;
        }
      }
    }

    return ret;
  }

  /**
   * Finds entities according to the values of <code>propertyValues</code>
   * @param propertyValues the property values to use as condition
   * @return the entities having the exact same property values as in <code>properties</properties>
   */
  public Entity[] getEntitiesByPropertyValues(final HashMap<String, Object> propertyValues) {
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
   * @return a HashMap containing a all entities which depend on the selected entities,
   * where the keys are Class objects and the value is an array of entities of that type
   * @throws DbException in case of a database exception
   * @throws UserException in case of a user exception
   */
  public final Map<String, List<Entity>> getSelectionDependencies() throws DbException, UserException {
    try {
      return getDbConnectionProvider().getEntityDb().getDependentEntities(getSelectedEntities());
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  /**
   * @return the property filters this table model uses
   */
  public List<PropertyFilterModel> getPropertyFilterModels() {
    return propertyFilterModels;
  }

  /**
   * @return the number of currently filtered items
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
    final List<Entity> ret = new ArrayList<Entity>(visibleEntities);
    ret.addAll(filteredEntities);

    return ret;
  }

  /**
   * Removes all elements from this table model
   */
  public void clear() {
    removeAll();
    tableStatus.setNull();
  }

  /**
   * @return Value for property 'queryRangeEnabled'.
   */
  public boolean isQueryRangeEnabled() {
    return queryRangeEnabled;
  }

  /**
   * @param enabled Value to set for property 'queryRangeEnabled'.
   */
  public void setQueryRangeEnabled(boolean enabled) {
    this.queryRangeEnabled = enabled;
  }

  /**
   * Sets the query queryRange from value
   * @param from the new query range from value
   */
  public void setQueryRangeFrom(final int from) {
    queryRangeCriteria.setValues(from, getQueryRangeTo());
  }

  /**
   * @return Value for property 'queryRangeFrom'.
   */
  public int getQueryRangeFrom() {
    return (Integer) queryRangeCriteria.getValues().get(0);
  }

  /**
   * @param to Value to set for property 'queryRangeTo'.
   */
  public void setQueryRangeTo(final int to) {
    queryRangeCriteria.setValues(getQueryRangeFrom(), to);
  }

  /**
   * @return Value for property 'queryRangeTo'.
   */
  public int getQueryRangeTo() {
    return Math.min((Integer) queryRangeCriteria.getValues().get(1), getRowCount());
  }

  /** {@inheritDoc} */
  public String toString() {
    return getEntityID();
  }

  /**
   * @return entities selected from the database according the the query criteria.
   * @throws UserException in case of an exception
   * @throws DbException in case of a database exception
   */
  protected List<Entity> getAllEntitiesFromDb() throws DbException, UserException {
    final ICriteria propertyCriteria = getSearchCriteria();
    if (filterQueryByMaster && propertyCriteria == null && !showAllWhenNotFiltered)
      return new ArrayList<Entity>();

    try {
      if (propertyCriteria != null || isQueryRangeEnabled()) {
        final CriteriaSet set = new CriteriaSet(CriteriaSet.Conjunction.AND);
        if (propertyCriteria != null)
          set.addCriteria(propertyCriteria);
        if (isQueryRangeEnabled())
          set.addCriteria(getQueryRangeCriteria());
        final EntityCriteria criteria = new EntityCriteria(getEntityID(), set, isQueryRangeEnabled());
        criteria.setTableHasAuditColumns(tableStatus.tableHasAuditColumns());

        return getDbConnectionProvider().getEntityDb().selectMany(criteria, true);
      }
      else
        return getDbConnectionProvider().getEntityDb().selectAll(getEntityID(), true);
    }
    catch (DbException dbe) {
      throw dbe;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  protected List<Property> getSearchableProperties() {
    final List<Property> ret = new ArrayList<Property>();
    for (final Property property : EntityRepository.get().getProperties(getEntityID(), false)) {
      if (property.isDatabaseProperty())
        ret.add(property);
    }

    return ret;
  }

  protected ICriteria getSearchCriteria() {
    final CriteriaSet ret = new CriteriaSet(CriteriaSet.Conjunction.AND);
    for (final AbstractSearchModel criteria : propertySearchModels)
      if (criteria.stSearchEnabled.isActive())
        ret.addCriteria(((PropertySearchModel) criteria).getPropertyCriteria());

    return ret.getCriteriaCount() > 0 ? ret : null;
  }

  @SuppressWarnings({"UnusedDeclaration"})
  protected boolean includeSearchComboBoxModel(final Property property) {
    return true;
  }

  protected PropertyCriteria getQueryRangeCriteria() {
    return queryRangeCriteria;
  }

  protected boolean isRefreshRequired() throws UserException {
    final TableStatus currentTableStatus;
    try {
      currentTableStatus = getDbConnectionProvider().getEntityDb().getTableStatus(
              getEntityID(), tableStatus.tableHasAuditColumns());
      tableStatus.setTableHasAuditColumns(currentTableStatus.tableHasAuditColumns());
      if (forceRefresh || currentTableStatus.isNull() || !currentTableStatus.equals(tableStatus)) {
        setCurrentTableStatus(currentTableStatus);
        return true;
      }

      return false;
    }
    catch (Exception e) {
      throw new UserException(e);
    }
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
   * @return table filters initialized according to the model
   */
  protected List<PropertyFilterModel> initPropertyFilters() {
    final List<PropertyFilterModel> filters = new ArrayList<PropertyFilterModel>(tableColumnProperties.size());
    int i = 0;
    for (final Property property : tableColumnProperties)
      filters.add(new PropertyFilterModel(property, i++));

    return filters;
  }

  protected List<Property> initColumnProperties() {
    final Collection<Property> properties = EntityRepository.get().getVisibleProperties(getEntityID());

    return Arrays.asList(properties.toArray(new Property[properties.size()]));
  }

  protected void bindEvents() {
    final List<PropertyFilterModel> filterModels = getPropertyFilterModels();
    for (final AbstractSearchModel filterModel : filterModels) {
      filterModel.evtSearchStateChanged.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          filterTable();
        }
      });
    }

    final ActionListener dataStateListener = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        stDataDirty.setActive(searchStateOnRefresh != getSearchModelState());
      }
    };
    evtSimpleSearchStringChanged.addListener(dataStateListener);
    for (final AbstractSearchModel searchModel : getPropertySearchModels())
      searchModel.evtSearchStateChanged.addListener(dataStateListener);

    evtRefreshDone.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        searchStateOnRefresh = getSearchModelState();
        stDataDirty.setActive(false);
      }
    });

    addTableModelListener(new TableModelListener() {
      public void tableChanged(TableModelEvent e) {
        if (e.getType() == TableModelEvent.DELETE || e.getType() == TableModelEvent.INSERT
                || e.getType() == TableModelEvent.UPDATE)
          evtTableDataChanged.fire();
      }
    });

    tableSorter.evtBeforeSort.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        isSorting = true;
        selectedPrimaryKeys = getPrimaryKeysOfSelectedEntities();
      }
    });

    tableSorter.evtAfterSort.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        setSelectedByPrimaryKeys(selectedPrimaryKeys);
        isSorting = false;
      }
    });
  }

  protected boolean include(final Entity entity) {
    for (final AbstractSearchModel columnFilter : propertyFilterModels)
      if (columnFilter.stSearchEnabled.isActive() && !columnFilter.include(entity))
        return false;

    return true;
  }

  protected Entity getCurrentReportRecord() {
    return currentReportRecord;
  }

  protected void setCurrentTableStatus(final TableStatus currentTableStatus) {
    tableStatus.setLastChange(currentTableStatus.getLastChange());
    tableStatus.setRecordCount(currentTableStatus.getRecordCount());
  }

  protected synchronized void addEntities(final List<Entity> entities, final boolean atFront) {
    for (final Entity entity : entities) {
      if (include(entity)) {
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

  private List<PropertySearchModel> initPropertySearchModels(final List<Property> properties) {
    final List<PropertySearchModel> ret = new ArrayList<PropertySearchModel>();
    for (final Property property : properties) {
      if (property instanceof Property.EntityProperty && includeSearchComboBoxModel(property))
        propertySearchComboBoxModels.put(property, new EntityComboBoxModel(getDbConnectionProvider(),
                ((Property.EntityProperty) property).referenceEntityID, false, "", true));

      ret.add(new PropertySearchModel(property, propertySearchComboBoxModels.get(property)));
    }

    return ret;
  }

  private long getSearchModelState() {
    long ret = simpleSearchString.hashCode();
    for (final AbstractSearchModel model : getPropertySearchModels())
      ret += ((PropertySearchModel) model).hashCode(model.stSearchEnabled.isActive());

    return ret;
  }

  private String getRangeDescription() {
    if (!queryRangeEnabled || (visibleEntities.size() == 0 && filteredEntities.size() == 0))
      return "";

    return " " + FrameworkMessages.get(FrameworkMessages.RANGE) + " " + getQueryRangeFrom() + " "
            + FrameworkMessages.get(FrameworkMessages.TO) + " " + getQueryRangeTo()
            + (tableStatus.getRecordCount() < 0 ? "" : (" "
            + FrameworkMessages.get(FrameworkMessages.OF) + " "
            + tableStatus.getRecordCount()));
  }

  private int getColumnIndex(final String masterEntityID) {
    for (int i = 0; i < tableColumnProperties.size(); i++)
      if (tableColumnProperties.get(i) instanceof Property.EntityProperty
              && ((Property.EntityProperty) tableColumnProperties.get(i)).referenceEntityID.equals(masterEntityID))
        return i;

    return -1;
  }

  private Iterator<Entity> initReportPrintIterator() {
    return getSelectedEntities().iterator();
  }

  private synchronized int modelIndexOf(final Entity entity) {
    return visibleEntities.indexOf(entity);
  }

  private synchronized int viewIndexOf(final Entity entity) {
    return tableSorter.viewIndex(modelIndexOf(entity));
  }
}