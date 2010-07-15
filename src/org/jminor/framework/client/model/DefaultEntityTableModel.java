/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DbException;
import org.jminor.common.model.AbstractFilteredTableModel;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.SortingDirective;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportDataWrapper;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.client.model.event.DeleteEvent;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
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
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A TableModel implementation for displaying and working with entities.
 */
public class DefaultEntityTableModel extends AbstractFilteredTableModel<Entity> implements EntityTableModel {

  private static final Logger LOG = Util.getLogger(DefaultEntityTableModel.class);

  private final Event evtRefreshStarted = new Event();
  private final Event evtRefreshDone = new Event();

  public static final Comparator COMPARABLE_COMPARATOR = new Comparator<Comparable>() {
    public int compare(Comparable o1, Comparable o2) {
      return (o1.compareTo(o2));
    }
  };
  public static final Comparator LEXICAL_COMPARATOR = new Comparator<Object>() {
    private final Collator collator = Collator.getInstance();
    public int compare(Object o1, Object o2) {
      return collator.compare(o1.toString(), o2.toString());
    }
  };

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

  /**
   * true while the model data is being refreshed
   */
  private boolean isRefreshing = false;

  private final State stAllowMultipleUpdate = new State(true);

  private final State stAllowDelete = new State(true);

  public DefaultEntityTableModel(final String entityID, final EntityDbProvider dbProvider) {
    this(entityID, dbProvider, new DefaultEntityTableColumnModel(entityID));
  }

  public DefaultEntityTableModel(final String entityID, final EntityDbProvider dbProvider,
                                 final EntityTableColumnModel columnModel) {
    this(entityID, dbProvider, new DefaultEntityTableSearchModel(entityID, dbProvider, columnModel.getColumnProperties(), false));
  }

  public DefaultEntityTableModel(final String entityID, final EntityDbProvider dbProvider,
                                 final EntityTableSearchModel searchModel) {
    super(new DefaultEntityTableColumnModel(entityID, searchModel.getProperties()));
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.searchModel = searchModel;
    bindEventsInternal();
    bindEvents();
  }

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

  public List<Property> getTableColumnProperties() {
    final List<Property> propertyList = new ArrayList<Property>(getColumnModel().getColumnCount());
    final Enumeration<TableColumn> columnEnumeration = getColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements()) {
      propertyList.add((Property) columnEnumeration.nextElement().getIdentifier());
    }

    return propertyList;
  }

  public boolean isQueryConfigurationAllowed() {
    return queryConfigurationAllowed;
  }

  public EntityTableModel setQueryConfigurationAllowed(final boolean value) {
    this.queryConfigurationAllowed = value;
    return this;
  }

  public int getFetchCount() {
    return fetchCount;
  }

  public DefaultEntityTableModel setFetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
    return this;
  }

  public boolean isDetailModel() {
    return isDetailModel;
  }

  public void setDetailModel(final boolean detailModel) {
    this.isDetailModel = detailModel;
  }

  /**
   * @return whether to show all underlying entities when no criteria is applied.
   */
  public boolean isQueryCriteriaRequired() {
    return queryCriteriaRequired;
  }

  public EntityTableModel setQueryCriteriaRequired(final boolean value) {
    this.queryCriteriaRequired = value;
    return this;
  }

  public String getEntityID() {
    return entityID;
  }

  public EntityTableSearchModel getSearchModel() {
    return searchModel;
  }

  public EntityEditModel getEditModel() {
    if (editModel == null) {
      throw new RuntimeException("No edit model has been set for table model: " + this);
    }
    return editModel;
  }

  public void setSortingDirective(final String propertyID, final SortingDirective directive) {
    final int columnIndex = getColumnModel().getColumnIndex(EntityRepository.getProperty(entityID, propertyID));
    if (columnIndex == -1) {
      throw new RuntimeException("Column based on property '" + propertyID + " not found");
    }

    super.setSortingDirective(columnIndex, directive);
  }

  public int compare(final Entity objectOne, final Entity objectTwo, final int columnIndex, final SortingDirective directive) {
    final Property property = getColumnProperty(columnIndex);
    final Object valueOne = objectOne.getValue(property);
    final Object valueTwo = objectTwo.getValue(property);
    int comparison;
    // Define null less than everything, except null.
    if (valueOne == null && valueTwo == null) {
      comparison = 0;
    }
    else if (valueOne == null) {
      comparison = -1;
    }
    else if (valueTwo == null) {
      comparison = 1;
    }
    else {
      comparison = getComparator(columnIndex).compare(valueOne, valueTwo);
    }
    if (comparison != 0) {
      return directive == SortingDirective.DESCENDING ? -comparison : comparison;
    }

    return 0;
  }

  public EntityDbProvider getDbProvider() {
    return dbProvider;
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

  public boolean isMultipleUpdateAllowed() {
    return true;
  }

  public State stateAllowMultipleUpdate() {
    return stAllowMultipleUpdate.getLinkedState();
  }

  public boolean isDeleteAllowed() {
    return stAllowDelete.isActive();
  }

  public void setDeleteAllowed(final boolean value) {
    stAllowDelete.setActive(value);
  }

  public State stateAllowDelete() {
    return stAllowDelete.getLinkedState();
  }

  public boolean isReadOnly() {
    return editModel == null || EntityRepository.isReadOnly(entityID);
  }

  @Override
  public FilterCriteria<Entity> getFilterCriteria() {
    return searchModel;
  }

  /**
   * Returns an initialized ReportDataWrapper instance, the default implementation returns null.
   * @return an initialized ReportDataWrapper
   * @see #getSelectedEntitiesIterator()
   */
  public ReportDataWrapper getReportDataSource() {
    return null;
  }

  @Override
  public boolean isCellEditable(final int rowIndex, final int columnIndex) {
    return false;
  }

  @Override
  public Class<?> getColumnClass(final int columnIndex) {
    final Property columnProperty = (Property) getColumnModel().getColumn(convertColumnIndexToView(columnIndex)).getIdentifier();

    return columnProperty.getTypeClass();
  }

  public Object getValueAt(final int rowIndex, final int columnIndex) {
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

    return Entity.getProxy(rowEntity.getEntityID()).getBackgroundColor(rowEntity);
  }

  public Collection<Object> getValues(final Property property, final boolean selectedOnly) {
    return EntityUtil.getPropertyValues(property.getPropertyID(),
            selectedOnly ? getSelectedItems() : getVisibleItems(), false);
  }

  public Entity getEntityByPrimaryKey(final Entity.Key primaryKey) {
    for (final Entity entity : getVisibleItems()) {
      if (entity.getPrimaryKey().equals(primaryKey)) {
        return entity;
      }
    }

    return null;
  }

  public int indexOf(final Entity.Key primaryKey) {
    return indexOf(getEntityByPrimaryKey(primaryKey));
  }

  public String getStatusMessage() {
    final int filteredItemCount = getFilteredItemCount();

    return new StringBuilder(Integer.toString(getRowCount())).append(" (").append(
            Integer.toString(getSelectionCount())).append(" ").append(
            FrameworkMessages.get(FrameworkMessages.SELECTED)).append(
            filteredItemCount > 0 ? ", " + filteredItemCount + " "
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

  public void addEntitiesByPrimaryKeys(final List<Entity.Key> primaryKeys, boolean atFront) {
    try {
      addItems(dbProvider.getEntityDb().selectMany(primaryKeys), atFront);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void replaceEntities(final Collection<Entity> entities) {
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
    final List<Property.ForeignKeyProperty> properties = EntityRepository.getForeignKeyProperties(entityID, referencedEntityID);
    if (properties.size() > 0 && isDetailModel && searchModel.setSearchValues(properties.get(0).getPropertyID(), referenceEntities)) {
      refresh();
    }
  }

  public List<Entity.Key> getPrimaryKeysOfSelectedEntities() {
    return EntityUtil.getPrimaryKeys(getSelectedItems());
  }

  public void setSelectedByPrimaryKeys(final List<Entity.Key> keys) {
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

  public Collection<Entity> getEntitiesByPropertyValues(final Map<String, Object> values) {
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

  public void deleteSelected() throws CancelException, DbException {
    if (editModel == null) {
      throw new RuntimeException("No edit model has been set for table model: " + this);
    }
    editModel.delete(getSelectedItems());
  }

  public void update(final List<Entity> entities) throws CancelException, ValidationException, DbException {
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

  @Override
  public String toString() {
    return entityID;
  }

  public boolean include(final Entity item) {
    return searchModel.include(item);
  }

  public PropertySummaryModel getPropertySummaryModel(final String propertyID) {
    return getPropertySummaryModel(EntityRepository.getProperty(entityID, propertyID));
  }

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
                  return DefaultEntityTableModel.this.getValues(property, isValueSubset());
                }

                public boolean isValueSubset() {
                  return !stateSelectionEmpty().isActive();
                }
              }));
    }

    return propertySummaryModels.get(property.getPropertyID());
  }

  public Property getColumnProperty(final int columnIndex) {
    return (Property) getColumnModel().getColumn(columnIndex).getIdentifier();
  }

  @Override
  public EntityTableColumnModel getColumnModel() {
    return (EntityTableColumnModel) super.getColumnModel();
  }

  public Iterator<Entity> getSelectedEntitiesIterator() {
    return getSelectedItems().iterator();
  }

  public Event eventRefreshDone() {
    return evtRefreshDone;
  }

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
   * @see #getQueryCriteria()
   */
  protected List<Entity> performQuery(final Criteria<Property> criteria) {
    if (isDetailModel && criteria == null && queryCriteriaRequired) {
      return new ArrayList<Entity>();
    }

    try {
      return dbProvider.getEntityDb().selectMany(EntityCriteriaUtil.selectCriteria(entityID, criteria,
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

  protected Comparator getComparator(final int column) {
    final Class columnClass = getColumnClass(column);
    if (columnClass.equals(String.class)) {
      return LEXICAL_COMPARATOR;
    }
    if (Comparable.class.isAssignableFrom(columnClass)) {
      return COMPARABLE_COMPARATOR;
    }

    return LEXICAL_COMPARATOR;
  }

  /**
   * @return a Criteria object used to filter the result when this
   * table models data is queried, the default implementation returns
   * the result retrieved via the <code>getSearchCriteria()</code> method
   * found in the underlying EntityTableSearchModel
   * @see DefaultEntityTableSearchModel#getSearchCriteria()
   */
  protected Criteria<Property> getQueryCriteria() {
    return searchModel.getSearchCriteria();
  }

  protected void handleColumnHidden(final Property property) {
    //disable the search model for the column to be hidden, to prevent confusion
    searchModel.setSearchEnabled(property.getPropertyID(), false);
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
    searchModel.eventFilterStateChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        filterContents();
      }
    });
    evtRefreshDone.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        searchModel.setSearchModelState();
      }
    });
  }
}