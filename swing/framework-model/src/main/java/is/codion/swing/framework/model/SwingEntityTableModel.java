/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.UserPreferences;
import is.codion.common.model.table.ColumnSummaryModel;
import is.codion.common.model.table.SortingDirective;
import is.codion.common.model.table.TableSortModel;
import is.codion.common.state.State;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.ValueListProperty;
import is.codion.framework.model.DefaultEntityTableConditionModel;
import is.codion.framework.model.DefaultFilterModelFactory;
import is.codion.framework.model.EntityModel;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.common.model.table.SwingFilteredTableColumnModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A TableModel implementation for displaying and working with entities.
 */
public class SwingEntityTableModel extends AbstractFilteredTableModel<Entity, Property<?>>
        implements EntityTableModel<SwingEntityEditModel> {

  private static final Logger LOG = LoggerFactory.getLogger(SwingEntityTableModel.class);

  /**
   * The entityType
   */
  private final EntityType<?> entityType;

  /**
   * The entity definition
   */
  private final EntityDefinition entityDefinition;

  /**
   * The EntityConnection provider
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * Event notifying that the edit model has been set.
   */
  private final Event<SwingEntityEditModel> editModelSetEvent = Event.event();

  /**
   * The edit model to use when updating/deleting entities
   */
  private SwingEntityEditModel editModel;

  /**
   * The condition model
   */
  private final EntityTableConditionModel tableConditionModel;

  /**
   * Fired each time this model is refreshed
   */
  private final Event<?> refreshEvent = Event.event();

  /**
   * If true then querying should be disabled if no condition is specified
   */
  private final State queryConditionRequiredState = State.state();

  /**
   * The maximum number of rows this table model accepts from a query.
   */
  private int queryRowCountLimit = -1;

  /**
   * the maximum number of records to fetch via the underlying query, -1 meaning all records should be fetched
   */
  private int fetchCount = -1;

  /**
   * If true then items deleted via the edit model are removed from this table model
   */
  private boolean removeEntitiesOnDelete = true;

  /**
   * The action to perform when entities are inserted via the associated edit model
   */
  private InsertAction insertAction = InsertAction.ADD_TOP;

  /**
   * Indicates if multiple entities can be updated at a time
   */
  private boolean batchUpdateEnabled = true;

  /**
   * Indicates if this table model should automatically refresh when foreign key condition values are set
   */
  private boolean refreshOnForeignKeyConditionValuesSet = true;

  /**
   * Is this table model editable.
   * @see #isCellEditable(int, int)
   * @see #setValueAt(Object, int, int)
   */
  private boolean editable = false;

  /**
   * Instantiates a new SwingEntityTableModel with default column and condition models.
   * @param entityType the entityType
   * @param connectionProvider the db provider
   */
  public SwingEntityTableModel(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider) {
    this(entityType, requireNonNull(connectionProvider), new SwingEntityTableSortModel(connectionProvider.getEntities(), entityType),
            new DefaultEntityTableConditionModel(entityType, connectionProvider,
                    new DefaultFilterModelFactory(), new SwingConditionModelFactory()));
  }

  /**
   * Instantiates a new DefaultEntityTableModel.
   * @param entityType the entityType
   * @param connectionProvider the connection provider
   * @param tableConditionModel the table condition model
   * @param sortModel the sort model
   * @throws NullPointerException in case conditionModel is null
   * @throws IllegalArgumentException if {@code tableConditionModel} entityType does not match the one supplied as parameter
   */
  public SwingEntityTableModel(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider,
                               final TableSortModel<Entity, Property<?>, TableColumn> sortModel,
                               final EntityTableConditionModel tableConditionModel) {
    super(sortModel, requireNonNull(tableConditionModel, "tableConditionModel").getFilterModels());
    if (!tableConditionModel.getEntityType().equals(requireNonNull(entityType, "entityType"))) {
      throw new IllegalArgumentException("Entity type mismatch, conditionModel: " + tableConditionModel.getEntityType()
              + ", tableModel: " + entityType);
    }
    this.entityType = entityType;
    this.entityDefinition = requireNonNull(connectionProvider).getEntities().getDefinition(entityType);
    this.connectionProvider = connectionProvider;
    this.tableConditionModel = tableConditionModel;
    bindEventsInternal();
    applyPreferences();
  }

  @Override
  public final Entities getEntities() {
    return connectionProvider.getEntities();
  }

  @Override
  public final EntityDefinition getEntityDefinition() {
    return entityDefinition;
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + entityType;
  }

  @Override
  public final void setEditModel(final SwingEntityEditModel editModel) {
    requireNonNull(editModel, "editModel");
    if (this.editModel != null) {
      throw new IllegalStateException("Edit model has already been set for table model: " + this);
    }
    if (!editModel.getEntityType().equals(entityType)) {
      throw new IllegalArgumentException("Entity type mismatch, editModel: " + editModel.getEntityType() + ", tableModel: " + entityType);
    }
    this.editModel = editModel;
    bindEditModelEvents();
    editModelSetEvent.onEvent(editModel);
  }

  @Override
  public final boolean hasEditModel() {
    return this.editModel != null;
  }

  @Override
  public final int getFetchCount() {
    return fetchCount;
  }

  @Override
  public final void setFetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
  }

  @Override
  public final int getQueryRowCountLimit() {
    return queryRowCountLimit;
  }

  @Override
  public final void setQueryRowCountLimit(final int queryRowCountLimit) {
    this.queryRowCountLimit = queryRowCountLimit;
  }

  @Override
  public final State getQueryConditionRequiredState() {
    return queryConditionRequiredState;
  }

  @Override
  public final InsertAction getInsertAction() {
    return insertAction;
  }

  @Override
  public final void setInsertAction(final InsertAction insertAction) {
    this.insertAction = requireNonNull(insertAction, "insertAction");
  }

  @Override
  public final boolean isRemoveEntitiesOnDelete() {
    return removeEntitiesOnDelete;
  }

  @Override
  public final void setRemoveEntitiesOnDelete(final boolean removeEntitiesOnDelete) {
    this.removeEntitiesOnDelete = removeEntitiesOnDelete;
  }

  @Override
  public final EntityType<?> getEntityType() {
    return entityType;
  }

  @Override
  public final EntityTableConditionModel getTableConditionModel() {
    return tableConditionModel;
  }

  @Override
  public final SwingEntityEditModel getEditModel() {
    if (editModel == null) {
      throw new IllegalStateException("No edit model has been set for table model: " + this);
    }
    return editModel;
  }

  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  @Override
  public final boolean isEditable() {
    return editable;
  }

  @Override
  public final void setEditable(final boolean editable) {
    this.editable = editable;
  }

  @Override
  public final boolean isBatchUpdateEnabled() {
    return batchUpdateEnabled;
  }

  @Override
  public final void setBatchUpdateEnabled(final boolean batchUpdateEnabled) {
    this.batchUpdateEnabled = batchUpdateEnabled;
  }

  @Override
  public final void setRefreshOnForeignKeyConditionValuesSet(final boolean refreshOnForeignKeyConditionValuesSet) {
    this.refreshOnForeignKeyConditionValuesSet = refreshOnForeignKeyConditionValuesSet;
  }

  @Override
  public final boolean isRefreshOnForeignKeyConditionValuesSet() {
    return refreshOnForeignKeyConditionValuesSet;
  }

  @Override
  public final boolean isDeleteEnabled() {
    return editModel != null && editModel.isDeleteEnabled();
  }

  @Override
  public final boolean isUpdateEnabled() {
    return editModel != null && editModel.isUpdateEnabled();
  }

  @Override
  public final boolean isReadOnly() {
    return editModel == null || editModel.isReadOnly();
  }

  @Override
  public final int getColumnIndex(final Attribute<?> attribute) {
    return getColumnModel().getColumnIndex(getEntityDefinition().getProperty(attribute));
  }

  /**
   * Returns true if the cell at <code>rowIndex</code> and <code>modelColumnIndex</code> is editable.
   * @param rowIndex the row whose value to be queried
   * @param modelColumnIndex the column whose value to be queried
   * @return true if the cell is editable
   * @see #setValueAt(Object, int, int)
   */
  @Override
  public boolean isCellEditable(final int rowIndex, final int modelColumnIndex) {
    if (!editable || isReadOnly() || !isUpdateEnabled()) {
      return false;
    }
    final Property<?> property = getColumnModel().getColumnIdentifier(modelColumnIndex);
    if (property instanceof ForeignKeyProperty) {
      return entityDefinition.isUpdatable(((ForeignKeyProperty) property).getAttribute());
    }

    return property instanceof ColumnProperty && ((ColumnProperty<?>) property).isUpdatable();
  }

  /**
   * Returns the value for the cell at <code>modelColumnIndex</code> and <code>rowIndex</code>.
   * @param rowIndex the row whose value is to be queried
   * @param modelColumnIndex the column whose value is to be queried
   * @return the value Object at the specified cell
   */
  @Override
  public final Object getValueAt(final int rowIndex, final int modelColumnIndex) {
    final Property<?> property = getColumnModel().getColumnIdentifier(modelColumnIndex);
    final Entity entity = getItemAt(rowIndex);

    return getValue(entity, property);
  }

  /**
   * Sets the value in the given cell and updates the underlying Entity.
   * @param value the new value
   * @param rowIndex the row whose value is to be changed
   * @param modelColumnIndex the model index of the column to be changed
   */
  @Override
  public final void setValueAt(final Object value, final int rowIndex, final int modelColumnIndex) {
    if (!editable || isReadOnly() || !isUpdateEnabled()) {
      throw new IllegalStateException("This table model is readOnly or has disabled update");
    }
    final Entity entity = getEntities().copyEntity(getItemAt(rowIndex));

    final Property<?> columnIdentifier = getColumnModel().getColumnIdentifier(modelColumnIndex);

    entity.put((Attribute<Object>) columnIdentifier.getAttribute(), value);
    try {
      update(singletonList(entity));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * A convenience method for setting the sorting directive for the given attribute
   * @param attribute the attribute
   * @param directive the directive
   * @see TableSortModel#setSortingDirective(Object, SortingDirective)
   */
  public final void setSortingDirective(final Attribute<?> attribute, final SortingDirective directive) {
    getSortModel().setSortingDirective(getEntityDefinition().getProperty(attribute), directive);
  }

  /**
   * A convenience method for setting the sorting directive for the given attribute
   * @param attribute the attribute
   * @param directive the directive
   * @see TableSortModel#addSortingDirective(Object, SortingDirective)
   */
  public final void addSortingDirective(final Attribute<?> attribute, final SortingDirective directive) {
    getSortModel().addSortingDirective(getEntityDefinition().getProperty(attribute), directive);
  }

  /**
   * A convenience method for retrieving the sorting directive for the given attribute
   * from the underlying {@link TableSortModel}.
   * @param attribute the attribute
   * @return the {@link TableSortModel.SortingState} associated with the given attribute
   */
  public final TableSortModel.SortingState getSortingState(final Attribute<?> attribute) {
    return getSortModel().getSortingState(getEntityDefinition().getProperty(attribute));
  }

  @Override
  public Color getBackgroundColor(final int row, final Attribute<?> attribute) {
    return (Color) getEntityDefinition().getColorProvider().getColor(getItemAt(row), attribute);
  }

  @Override
  public final Entity getEntityByKey(final Key primaryKey) {
    return getVisibleItems().stream().filter(entity -> entity.getPrimaryKey().equals(primaryKey)).findFirst().orElse(null);
  }

  @Override
  public final int indexOf(final Key primaryKey) {
    return indexOf(getEntityByKey(primaryKey));
  }

  @Override
  public void addEntities(final List<Entity> entities) {
    addEntitiesAt(getVisibleItemCount(), entities);
  }

  @Override
  public void addEntitiesSorted(final List<Entity> entities) {
    addEntitiesAtSorted(getVisibleItemCount(), entities);
  }

  @Override
  public void addEntitiesAt(final int index, final List<Entity> entities) {
    addItemsAt(index, entities);
  }

  @Override
  public void addEntitiesAtSorted(final int index, final List<Entity> entities) {
    addItemsAtSorted(index, entities);
  }

  @Override
  public final void replaceEntities(final List<Entity> entities) {
    replaceEntitiesByKey(Entities.mapToPrimaryKey(entities));
  }

  @Override
  public final void refreshEntities(final List<Key> keys) {
    try {
      replaceEntities(getConnectionProvider().getConnection().select(keys));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setForeignKeyConditionValues(final ForeignKey foreignKey, final Collection<Entity> foreignKeyValues) {
    requireNonNull(foreignKey, "foreignKey");
    getEntityDefinition().getForeignKeyProperty(foreignKey);
    if (tableConditionModel.setEqualConditionValues(foreignKey, foreignKeyValues) && refreshOnForeignKeyConditionValuesSet) {
      refresh();
    }
  }

  @Override
  public final void replaceForeignKeyValues(final EntityType<?> foreignKeyEntityType, final Collection<Entity> foreignKeyValues) {
    requireNonNull(foreignKeyValues, "foreignKeyValues");
    final List<ForeignKey> foreignKeys = getEntityDefinition().getForeignKeys(requireNonNull(foreignKeyEntityType, "foreignKeyEntityType"));
    boolean changed = false;
    for (final Entity entity : getItems()) {
      for (final ForeignKey foreignKey : foreignKeys) {
        for (final Entity foreignKeyValue : foreignKeyValues) {
          final Entity currentForeignKeyValue = entity.getForeignKey(foreignKey);
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

  @Override
  public final void setSelectedByKey(final Collection<Key> keys) {
    requireNonNull(keys, "keys");
    final List<Key> keyList = new ArrayList<>(keys);
    final List<Integer> indexes = new ArrayList<>();
    for (final Entity visibleEntity : getVisibleItems()) {
      final int index = keyList.indexOf(visibleEntity.getPrimaryKey());
      if (index >= 0) {
        indexes.add(indexOf(visibleEntity));
        keyList.remove(index);
        if (keyList.isEmpty()) {
          break;
        }
      }
    }

    getSelectionModel().setSelectedIndexes(indexes);
  }

  @Override
  public final Collection<Entity> getEntitiesByKey(final Collection<Key> keys) {
    requireNonNull(keys, "keys");
    return getItems().stream().filter(entity -> keys.contains(entity.getPrimaryKey())).collect(Collectors.toList());
  }

  @Override
  public final void deleteSelected() throws DatabaseException {
    if (!isDeleteEnabled()) {
      throw new IllegalStateException("Deleting is not enabled in this table model");
    }
    editModel.delete(getSelectionModel().getSelectedItems());
  }

  @Override
  public final void update(final List<Entity> entities) throws ValidationException, DatabaseException {
    requireNonNull(entities, "entities");
    if (!isUpdateEnabled()) {
      throw new IllegalStateException("Updating is not enabled in this table model");
    }
    if (entities.size() > 1 && !batchUpdateEnabled) {
      throw new IllegalStateException("Batch update of entities is not enabled!");
    }
    editModel.update(entities);
  }

  @Override
  public final <T extends Number> ColumnSummaryModel getColumnSummaryModel(final Attribute<T> attribute) {
    return getColumnSummaryModel(getEntityDefinition().getProperty(attribute));
  }

  @Override
  public final Iterator<Entity> getSelectedEntitiesIterator() {
    return getSelectionModel().getSelectedItems().iterator();
  }

  @Override
  public final void setColumns(final Attribute<?>... attributes) {
    getColumnModel().setColumns(getEntityDefinition().getProperties(asList(attributes)).toArray(new Property[0]));
  }

  @Override
  public final void savePreferences() {
    if (EntityModel.USE_CLIENT_PREFERENCES.get()) {
      try {
        UserPreferences.putUserPreference(getUserPreferencesKey(), createPreferences().toString());
      }
      catch (final Exception e) {
        LOG.error("Error while saving preferences", e);
      }
    }
  }

  @Override
  public final String getTableDataAsDelimitedString(final char delimiter) {
    final List<String> header = new ArrayList<>();
    final List<Attribute<?>> attributes = new ArrayList<>();
    final Enumeration<TableColumn> columnEnumeration = getColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final Property<?> property = (Property<?>) columnEnumeration.nextElement().getIdentifier();
      attributes.add(property.getAttribute());
      header.add(property.getCaption());
    }

    return Text.getDelimitedString(header, Entities.getStringValueList(attributes,
            getSelectionModel().isSelectionEmpty() ? getVisibleItems() : getSelectionModel().getSelectedItems()),
            String.valueOf(delimiter));
  }

  @Override
  public final void addEditModelSetListener(final EventDataListener<SwingEntityEditModel> listener) {
    editModelSetEvent.addDataListener(listener);
  }

  @Override
  public final void addSelectionChangedListener(final EventListener listener) {
    getSelectionModel().addSelectionChangedListener(listener);
  }

  @Override
  public final void addRefreshListener(final EventListener listener) {
    refreshEvent.addListener(listener);
  }

  @Override
  public final void removeRefreshListener(final EventListener listener) {
    refreshEvent.removeListener(listener);
  }

  @Override
  protected final <T extends Number> ColumnSummaryModel.ColumnValueProvider<T> createColumnValueProvider(final Property<?> property) {
    if (property.getAttribute().isNumerical()) {
      return new DefaultColumnValueProvider<>(property, this, property.getFormat());
    }

    return null;
  }

  @Override
  protected final void doRefresh() {
    try {
      LOG.debug("{} refreshing", this);
      checkQueryRowCount();
      final List<Entity> queryResult = performQuery();
      clear();
      addEntitiesSorted(queryResult);
      tableConditionModel.rememberCondition();
      refreshEvent.onEvent();
    }
    finally {
      LOG.debug("{} refreshing done", this);
    }
  }

  /**
   * Queries for the data used to populate this EntityTableModel when it is refreshed,
   * using the order by clause returned by {@link #getOrderBy()}
   * @return entities selected from the database according the the query condition.
   * @see #getQueryConditionRequiredState()
   * @see EntityTableConditionModel#getCondition()
   */
  protected List<Entity> performQuery() {
    if (queryConditionRequiredState.get() && !getTableConditionModel().isEnabled()) {
      return emptyList();
    }

    try {
      return connectionProvider.getConnection().select(getTableConditionModel().getCondition()
              .select().fetchCount(fetchCount).orderBy(getOrderBy()));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the number of rows {@link #performQuery()} would return on next invocation
   */
  protected int getQueryRowCount() {
    if (queryConditionRequiredState.get() && !getTableConditionModel().isEnabled()) {
      return 0;
    }

    try {
      return connectionProvider.getConnection().rowCount(getTableConditionModel().getCondition());
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return the value to display in a table cell for the given property of the given entity.
   * @param entity the entity
   * @param property the property
   * @return the value of the given property for the given entity for display
   * @throws NullPointerException in case entity or property is null
   */
  protected Object getValue(final Entity entity, final Property<?> property) {
    requireNonNull(entity, "entity");
    requireNonNull(property, "property");
    if (property instanceof ValueListProperty) {
      return entity.getAsString(property.getAttribute());
    }

    return entity.get(property.getAttribute());
  }

  @Override
  protected final String getSearchValueAt(final int rowIndex, final TableColumn column) {
    return getItemAt(rowIndex).getAsString(((Property<?>) column.getIdentifier()).getAttribute());
  }

  /**
   * The order by clause to use when selecting the data for this model,
   * by default the order by clause defined for the underlying entity
   * @return the order by clause
   * @see EntityDefinition#getOrderBy()
   */
  protected OrderBy getOrderBy() {
    return getEntityDefinition().getOrderBy();
  }

  /**
   * Returns the key used to identify user preferences for this table model, that is column positions, widths and such.
   * The default implementation is:
   * <pre>
   * {@code
   * return getClass().getSimpleName() + "-" + getEntityType();
   * }
   * </pre>
   * Override in case this key is not unique.
   * @return the key used to identify user preferences for this table model
   */
  protected String getUserPreferencesKey() {
    return getClass().getSimpleName() + "-" + getEntityType();
  }

  /**
   * Clears any user preferences saved for this table model
   */
  final void clearPreferences() {
    UserPreferences.removeUserPreference(getUserPreferencesKey());
  }

  private void bindEventsInternal() {
    getColumnModel().addColumnHiddenListener(this::onColumnHidden);
  }

  private void bindEditModelEvents() {
    editModel.addAfterInsertListener(this::onInsert);
    editModel.addAfterUpdateListener(this::onUpdate);
    editModel.addAfterDeleteListener(this::onDelete);
    editModel.addAfterRefreshListener(this::refresh);
    editModel.addEntitySetListener(this::onEntitySet);
    getSelectionModel().addSelectedItemListener(editModel::setEntity);
    addTableModelListener(this::onTableModelEvent);
  }

  private void checkQueryRowCount() {
    if (queryRowCountLimit >= 0 && getQueryRowCount() > queryRowCountLimit) {
      throw new IllegalStateException("Too many rows returned, add query condition");
    }
  }

  private void onInsert(final List<Entity> insertedEntities) {
    getSelectionModel().clearSelection();
    if (!insertAction.equals(InsertAction.DO_NOTHING)) {
      final List<Entity> entitiesToAdd = insertedEntities.stream().filter(entity ->
              entity.getEntityType().equals(getEntityType())).collect(Collectors.toList());
      switch (insertAction) {
        case ADD_TOP:
          addEntitiesAt(0, entitiesToAdd);
          break;
        case ADD_TOP_SORTED:
          addEntitiesAtSorted(0, entitiesToAdd);
          break;
        case ADD_BOTTOM:
          addEntities(entitiesToAdd);
          break;
      }
    }
  }

  private void onUpdate(final Map<Key, Entity> updatedEntities) {
    replaceEntitiesByKey(new HashMap<>(updatedEntities));
  }

  private void onDelete(final List<Entity> deletedEntities) {
    if (removeEntitiesOnDelete) {
      removeItems(deletedEntities);
    }
  }

  private void onEntitySet(final Entity entity) {
    if (entity == null && !getSelectionModel().isSelectionEmpty()) {
      getSelectionModel().clearSelection();
    }
  }

  private void onTableModelEvent(final TableModelEvent tableModelEvent) {
    //if the selected record is updated via the table model, refresh the one in the edit model
    if (tableModelEvent.getType() == TableModelEvent.UPDATE && tableModelEvent.getFirstRow() == getSelectionModel().getSelectedIndex()) {
      editModel.setEntity(getSelectionModel().getSelectedItem());
    }
  }

  /**
   * Replace the entities identified by the Entity.Key map keys with their respective value.
   * Note that this does not trigger {@link #filterContents()}, that must be done explicitly.
   * @param entitiesByKey the entities to replace mapped to the corresponding primary key found in this table model
   */
  private void replaceEntitiesByKey(final Map<Key, Entity> entitiesByKey) {
    for (final Entity entity : getItems()) {
      final Iterator<Map.Entry<Key, Entity>> mapIterator = entitiesByKey.entrySet().iterator();
      while (mapIterator.hasNext()) {
        final Map.Entry<Key, Entity> entry = mapIterator.next();
        if (entity.getPrimaryKey().equals(entry.getKey())) {
          mapIterator.remove();
          entity.setAs(entry.getValue());
          final int index = indexOf(entity);
          if (index >= 0) {
            fireTableRowsUpdated(index, index);
          }
        }
      }
      if (entitiesByKey.isEmpty()) {
        break;
      }
    }
  }

  private void onColumnHidden(final Property<?> property) {
    //disable the condition model for the column to be hidden, to prevent confusion
    tableConditionModel.disable(property.getAttribute());
  }

  private org.json.JSONObject createPreferences() throws Exception {
    final org.json.JSONObject preferencesRoot = new org.json.JSONObject();
    preferencesRoot.put(PREFERENCES_COLUMNS, createColumnPreferences());

    return preferencesRoot;
  }

  private org.json.JSONObject createColumnPreferences() throws Exception {
    final org.json.JSONObject columnPreferencesRoot = new org.json.JSONObject();
    for (final TableColumn column : getColumnModel().getAllColumns()) {
      final Property<?> property = (Property<?>) column.getIdentifier();
      final org.json.JSONObject columnObject = new org.json.JSONObject();
      final boolean visible = getColumnModel().isColumnVisible(property);
      columnObject.put(PREFERENCES_COLUMN_WIDTH, column.getWidth());
      columnObject.put(PREFERENCES_COLUMN_VISIBLE, visible);
      columnObject.put(PREFERENCES_COLUMN_INDEX, visible ? getColumnModel().getColumnIndex(property) : -1);
      columnPreferencesRoot.put(property.getAttribute().getName(), columnObject);
    }

    return columnPreferencesRoot;
  }

  private void applyPreferences() {
    if (EntityModel.USE_CLIENT_PREFERENCES.get()) {
      final String preferencesString = UserPreferences.getUserPreference(getUserPreferencesKey(), "");
      try {
        if (preferencesString.length() > 0) {
          applyColumnPreferences(new org.json.JSONObject(preferencesString).getJSONObject(PREFERENCES_COLUMNS));
        }
      }
      catch (final Exception e) {
        LOG.error("Error while applying preferences: " + preferencesString, e);
      }
    }
  }

  private void applyColumnPreferences(final org.json.JSONObject preferences) {
    final SwingFilteredTableColumnModel<Entity, Property<?>> columnModel = getColumnModel();
    for (final TableColumn column : Collections.list(columnModel.getColumns())) {
      final Property<?> property = (Property<?>) column.getIdentifier();
      if (columnModel.containsColumn(property)) {
        try {
          final org.json.JSONObject columnPreferences = preferences.getJSONObject(property.getAttribute().getName());
          column.setPreferredWidth(columnPreferences.getInt(PREFERENCES_COLUMN_WIDTH));
          if (columnPreferences.getBoolean(PREFERENCES_COLUMN_VISIBLE)) {
            final int index = Math.min(columnModel.getColumnCount() - 1, columnPreferences.getInt(PREFERENCES_COLUMN_INDEX));
            columnModel.moveColumn(getColumnModel().getColumnIndex(column.getIdentifier()), index);
          }
          else {
            columnModel.hideColumn((Property<?>) column.getIdentifier());
          }
        }
        catch (final Exception e) {
          LOG.info("Property preferences not found: " + property, e);
        }
      }
    }
  }
}