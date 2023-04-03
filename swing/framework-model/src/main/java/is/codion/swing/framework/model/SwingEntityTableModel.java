/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.UserPreferences;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnSummaryModel.SummaryValueProvider;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
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
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.DefaultFilterModelFactory;
import is.codion.framework.model.EntityEditEvents;
import is.codion.framework.model.EntityModel;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.component.table.DefaultFilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableColumn;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SortOrder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static is.codion.framework.model.EntityTableConditionModel.entityTableConditionModel;
import static is.codion.swing.common.model.component.table.FilteredTableColumn.filteredTableColumn;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;

/**
 * A TableModel implementation for displaying and working with entities.
 */
public class SwingEntityTableModel extends DefaultFilteredTableModel<Entity, Attribute<?>>
        implements EntityTableModel<SwingEntityEditModel> {

  private static final Logger LOG = LoggerFactory.getLogger(SwingEntityTableModel.class);

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(SwingEntityTableModel.class.getName());

  private static final NumberFormat STATUS_MESSAGE_NUMBER_FORMAT = NumberFormat.getIntegerInstance();

  private final SwingEntityEditModel editModel;
  private final EntityTableConditionModel tableConditionModel;
  private final State queryConditionRequiredState = State.state();

  /**
   * Caches java.awt.Color instances parsed from hex strings via {@link #getColor(Object)}
   */
  private final ConcurrentHashMap<String, Color> colorCache = new ConcurrentHashMap<>();
  private final Value<String> statusMessageValue = Value.value("", "");
  private final State conditionChangedState = State.state();
  private final EventDataListener<Map<Key, Entity>> updateListener = new UpdateListener();

  /**
   * the condition active during the last refresh
   */
  private Condition refreshCondition;

  /**
   * the maximum number of records to fetch via the underlying query, -1 meaning all records should be fetched
   */
  private int limit = -1;

  /**
   * Specifies whether the values of hidden columns are included in the underlying query
   */
  private boolean queryHiddenColumns = EntityTableModel.QUERY_HIDDEN_COLUMNS.get();

  /**
   * If true then items deleted via the edit model are removed from this table model
   */
  private boolean removeDeletedEntities = true;

  /**
   * The action to perform when entities are inserted via the edit model
   */
  private InsertAction insertAction = InsertAction.ADD_TOP;

  /**
   * Specifies whether multiple entities can be updated at a time
   */
  private boolean batchUpdateEnabled = true;

  /**
   * Specifies whether this table model is editable.
   * @see #isCellEditable(int, int)
   * @see #setValueAt(Object, int, int)
   */
  private boolean editable = false;

  /**
   * Specifies whether to use the current sort order as the query order by clause
   */
  private boolean orderQueryBySortOrder = ORDER_QUERY_BY_SORT_ORDER.get();
  private boolean listenToEditEvents = true;

  /**
   * Instantiates a new SwingEntityTableModel.
   * @param entityType the entityType
   * @param connectionProvider the connection provider
   */
  public SwingEntityTableModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    this(new SwingEntityEditModel(entityType, connectionProvider));
  }

  /**
   * Instantiates a new SwingEntityTableModel.
   * @param entityType the entityType
   * @param connectionProvider the connection provider
   * @param tableConditionModel the table condition model
   */
  public SwingEntityTableModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                               EntityTableConditionModel tableConditionModel) {
    this(new SwingEntityEditModel(entityType, connectionProvider), tableConditionModel);
  }

  /**
   * Instantiates a new SwingEntityTableModel.
   * @param editModel the edit model
   */
  public SwingEntityTableModel(SwingEntityEditModel editModel) {
    this(editModel, entityTableConditionModel(editModel.entityType(), editModel.connectionProvider(),
            new DefaultFilterModelFactory(), new SwingConditionModelFactory(editModel.connectionProvider())));
  }

  /**
   * Instantiates a new SwingEntityTableModel.
   * @param editModel the edit model
   * @param tableConditionModel the table condition model
   */
  public SwingEntityTableModel(SwingEntityEditModel editModel, EntityTableConditionModel tableConditionModel) {
    super(createColumns(requireNonNull(editModel, "editModel").entities().definition(editModel.entityType())),
            new EntityColumnValueProvider(editModel.entities()),
            requireNonNull(tableConditionModel, "tableConditionModel").filterModels().values());
    if (!tableConditionModel.entityType().equals(editModel.entityType())) {
      throw new IllegalArgumentException("Entity type mismatch, conditionModel: " + tableConditionModel.entityType()
              + ", tableModel: " + editModel.entityType());
    }
    this.tableConditionModel = tableConditionModel;
    this.refreshCondition = tableConditionModel.condition();
    this.editModel = editModel;
    addEditEventListeners();
    bindEventsInternal();
    applyPreferences();
  }

  @Override
  public final Entities entities() {
    return editModel.connectionProvider().entities();
  }

  @Override
  public final EntityDefinition entityDefinition() {
    return editModel.entityDefinition();
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + editModel.entityType();
  }

  @Override
  public final int getLimit() {
    return limit;
  }

  @Override
  public final void setLimit(int limit) {
    this.limit = limit;
  }

  @Override
  public final boolean isQueryHiddenColumns() {
    return queryHiddenColumns;
  }

  @Override
  public final void setQueryHiddenColumns(boolean queryHiddenColumns) {
    this.queryHiddenColumns = queryHiddenColumns;
  }

  @Override
  public final boolean isOrderQueryBySortOrder() {
    return orderQueryBySortOrder;
  }

  @Override
  public final void setOrderQueryBySortOrder(boolean orderQueryBySortOrder) {
    this.orderQueryBySortOrder = orderQueryBySortOrder;
  }

  @Override
  public final State queryConditionRequiredState() {
    return queryConditionRequiredState;
  }

  @Override
  public final InsertAction getInsertAction() {
    return insertAction;
  }

  @Override
  public final void setInsertAction(InsertAction insertAction) {
    this.insertAction = requireNonNull(insertAction, "insertAction");
  }

  @Override
  public final boolean isRemoveDeletedEntities() {
    return removeDeletedEntities;
  }

  @Override
  public final void setRemoveDeletedEntities(boolean removeDeletedEntities) {
    this.removeDeletedEntities = removeDeletedEntities;
  }

  @Override
  public final boolean isListenToEditEvents() {
    return listenToEditEvents;
  }

  @Override
  public final void setListenToEditEvents(boolean listenToEditEvents) {
    this.listenToEditEvents = listenToEditEvents;
    if (listenToEditEvents) {
      addEditEventListeners();
    }
    else {
      removeEditEventListeners();
    }
  }

  @Override
  public final EntityType entityType() {
    return editModel.entityType();
  }

  @Override
  public final EntityTableConditionModel tableConditionModel() {
    return tableConditionModel;
  }

  @Override
  public final SwingEntityEditModel editModel() {
    return editModel;
  }

  @Override
  public final EntityConnectionProvider connectionProvider() {
    return editModel.connectionProvider();
  }

  @Override
  public final boolean isEditable() {
    return editable;
  }

  @Override
  public final void setEditable(boolean editable) {
    this.editable = editable;
  }

  @Override
  public final boolean isBatchUpdateEnabled() {
    return batchUpdateEnabled;
  }

  @Override
  public final void setBatchUpdateEnabled(boolean batchUpdateEnabled) {
    this.batchUpdateEnabled = batchUpdateEnabled;
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

  /**
   * Returns true if the cell at <code>rowIndex</code> and <code>modelColumnIndex</code> is editable.
   * @param rowIndex the row to edit
   * @param modelColumnIndex the model index of the column to edit
   * @return true if the cell is editable
   * @see #setValueAt(Object, int, int)
   */
  @Override
  public boolean isCellEditable(int rowIndex, int modelColumnIndex) {
    if (!editable || isReadOnly() || !isUpdateEnabled()) {
      return false;
    }
    Attribute<?> attribute = columnModel().columnIdentifier(modelColumnIndex);
    if (attribute instanceof ForeignKey) {
      return entityDefinition().isUpdatable((ForeignKey) attribute);
    }

    Property<?> property = entityDefinition().property(attribute);

    return property instanceof ColumnProperty && ((ColumnProperty<?>) property).isUpdatable();
  }

  /**
   * Sets the value in the given cell and updates the underlying Entity.
   * @param value the new value
   * @param rowIndex the row whose value is to be changed
   * @param modelColumnIndex the model index of the column to be changed
   */
  @Override
  public final void setValueAt(Object value, int rowIndex, int modelColumnIndex) {
    if (!editable || isReadOnly() || !isUpdateEnabled()) {
      throw new IllegalStateException("This table model is readOnly or has disabled update");
    }
    Entity entity = itemAt(rowIndex).copy();

    Attribute<?> columnIdentifier = columnModel().columnIdentifier(modelColumnIndex);

    entity.put((Attribute<Object>) columnIdentifier, value);
    try {
      update(singletonList(entity));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Color backgroundColor(int row, Attribute<?> attribute) {
    Object color = entityDefinition().backgroundColorProvider().color(itemAt(row), attribute);

    return color == null ? null : getColor(color);
  }

  @Override
  public Color foregroundColor(int row, Attribute<?> attribute) {
    Object color = entityDefinition().foregroundColorProvider().color(itemAt(row), attribute);

    return color == null ? null : getColor(color);
  }

  @Override
  public final Entity entityByKey(Key primaryKey) {
    return visibleItems().stream()
            .filter(entity -> entity.primaryKey().equals(primaryKey))
            .findFirst()
            .orElse(null);
  }

  @Override
  public final int indexOf(Key primaryKey) {
    return indexOf(entityByKey(primaryKey));
  }

  @Override
  public final void addEntities(Collection<Entity> entities) {
    addEntitiesAt(visibleItemCount(), entities);
  }

  @Override
  public final void addEntitiesSorted(Collection<Entity> entities) {
    addEntitiesAtSorted(visibleItemCount(), entities);
  }

  @Override
  public final void addEntitiesAt(int index, Collection<Entity> entities) {
    addItemsAt(index, entities);
  }

  @Override
  public final void addEntitiesAtSorted(int index, Collection<Entity> entities) {
    addItemsAtSorted(index, entities);
  }

  @Override
  public final void replaceEntities(Collection<Entity> entities) {
    replaceEntitiesByKey(Entity.mapToPrimaryKey(entities));
  }

  @Override
  public final void refreshEntities(List<Key> keys) {
    try {
      replaceEntities(connectionProvider().connection().select(keys));
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean setForeignKeyConditionValues(ForeignKey foreignKey, Collection<Entity> foreignKeyValues) {
    requireNonNull(foreignKey, "foreignKey");
    requireNonNull(foreignKeyValues, "foreignKeyValues");
    entityDefinition().foreignKeyProperty(foreignKey);

    return tableConditionModel.setEqualConditionValues(foreignKey, foreignKeyValues);
  }

  @Override
  public final void replaceForeignKeyValues(ForeignKey foreignKey, Collection<Entity> foreignKeyValues) {
    requireNonNull(foreignKey, "foreignKey");
    requireNonNull(foreignKeyValues, "foreignKeyValues");
    boolean changed = false;
    for (Entity entity : items()) {
      for (Entity foreignKeyValue : foreignKeyValues) {
        Entity currentForeignKeyValue = entity.referencedEntity(foreignKey);
        if (currentForeignKeyValue != null && currentForeignKeyValue.equals(foreignKeyValue)) {
          currentForeignKeyValue.setAs(foreignKeyValue);
          changed = true;
        }
      }
    }
    if (changed) {
      fireTableChanged(new TableModelEvent(this, 0, getRowCount() - 1));
    }
  }

  @Override
  public final void selectByKey(Collection<Key> keys) {
    requireNonNull(keys, "keys");
    List<Key> keyList = new ArrayList<>(keys);
    List<Integer> indexes = new ArrayList<>();
    for (Entity visibleEntity : visibleItems()) {
      int index = keyList.indexOf(visibleEntity.primaryKey());
      if (index >= 0) {
        indexes.add(indexOf(visibleEntity));
        keyList.remove(index);
        if (keyList.isEmpty()) {
          break;
        }
      }
    }

    selectionModel().setSelectedIndexes(indexes);
  }

  @Override
  public final Collection<Entity> entitiesByKey(Collection<Key> keys) {
    requireNonNull(keys, "keys");
    return items().stream()
            .filter(entity -> keys.contains(entity.primaryKey()))
            .collect(toList());
  }

  @Override
  public final void deleteSelected() throws DatabaseException {
    if (!isDeleteEnabled()) {
      throw new IllegalStateException("Deleting is not enabled in this table model");
    }
    editModel.delete(selectionModel().getSelectedItems());
  }

  @Override
  public final void update(List<Entity> entities) throws ValidationException, DatabaseException {
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
  public final void setVisibleColumns(Attribute<?>... attributes) {
    columnModel().setVisibleColumns(attributes);
  }

  @Override
  public final void setVisibleColumns(List<Attribute<?>> attributes) {
    columnModel().setVisibleColumns(attributes);
  }

  @Override
  public final void savePreferences() {
    if (EntityModel.USE_CLIENT_PREFERENCES.get()) {
      try {
        UserPreferences.setUserPreference(userPreferencesKey(), createPreferences().toString());
      }
      catch (Exception e) {
        LOG.error("Error while saving preferences", e);
      }
    }
  }

  @Override
  public final StateObserver conditionChangedObserver() {
    return conditionChangedState.observer();
  }

  @Override
  public final void addSelectionListener(EventListener listener) {
    selectionModel().addSelectionListener(listener);
  }

  /**
   * @return an Observer for the table model status message, that is, the number of rows, number of selected rows etc
   */
  public final ValueObserver<String> statusMessageObserver() {
    return statusMessageValue.observer();
  }

  /**
   * Initializes default {@link TableColumn}s for all visible properties in the given entity type.
   * @param definition the entity definition
   * @return a list of TableColumns based on the given entity
   */
  public static List<FilteredTableColumn<Attribute<?>>> createColumns(EntityDefinition definition) {
    return createColumns(requireNonNull(definition).visibleProperties());
  }

  /**
   * Initializes default {@link TableColumn}s from the given properties.
   * @param properties the properties
   * @return a list of TableColumns based on the given properties
   */
  public static List<FilteredTableColumn<Attribute<?>>> createColumns(List<Property<?>> properties) {
    requireNonNull(properties);
    List<FilteredTableColumn<Attribute<?>>> columns = new ArrayList<>(properties.size());
    for (Property<?> property : properties) {
      FilteredTableColumn<Attribute<?>> column = filteredTableColumn(columns.size(), property.attribute());
      column.setHeaderValue(property.caption());
      if (property.preferredColumnWidth() > 0) {
        column.setPreferredWidth(property.preferredColumnWidth());
      }
      columns.add(column);
    }

    return columns;
  }

  @Override
  protected final <T extends Number> Optional<SummaryValueProvider<T>> createColumnValueProvider(Attribute<?> attribute) {
    if (attribute.isNumerical()) {
      return Optional.of(new DefaultSummaryValueProvider<>(attribute, this, entityDefinition().property(attribute).format()));
    }

    return Optional.empty();
  }

  /**
   * Queries for the data used to populate this EntityTableModel when it is refreshed,
   * using the order by clause returned by {@link #orderBy()}
   * @return entities selected from the database according the query condition.
   * @see #queryConditionRequiredState()
   * @see EntityTableConditionModel#condition()
   */
  @Override
  protected Collection<Entity> refreshItems() {
    if (queryConditionRequiredState.get() && !tableConditionModel().isConditionEnabled()) {
      return emptyList();
    }
    try {
      return editModel.connectionProvider().connection().select(tableConditionModel().condition()
              .selectBuilder()
              .selectAttributes(selectAttributes())
              .limit(limit)
              .orderBy(orderBy())
              .build());
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a {@link java.awt.Color} instance from the given Object.
   * {@link java.awt.Color} instances are returned as-is, but instances of
   * {@link java.lang.String} are assumed to be in HEX format (f.ex: #ffff00" or #00ff00)
   * and are parsed with {@link Color#decode(String)}. Colors parsed from Strings are cached.
   * Override to support other representations.
   * @param color the object representing the color.
   * @return a {@link java.awt.Color} instance based on the given Object
   * @throws IllegalArgumentException in case the representation is not supported
   * @throws NullPointerException in case color is null
   */
  protected Color getColor(Object color) {
    requireNonNull(color);
    if (color instanceof Color) {
      return (Color) color;
    }
    if (color instanceof String) {
      return colorCache.computeIfAbsent((String) color, Color::decode);
    }

    throw new IllegalArgumentException("Unsupported Color representation: " + color);
  }

  /**
   * The order by clause to use when selecting the data for this model.
   * If ordering by sort order is enabled a {@link OrderBy} clause is constructed
   * according to the sort order of column based attributes, otherwise the order by
   * clause defined for the underlying entity is returned.
   * @return the order by clause
   * @see #setOrderQueryBySortOrder(boolean)
   * @see EntityDefinition#orderBy()
   */
  protected OrderBy orderBy() {
    if (orderQueryBySortOrder && sortModel().isSortingEnabled()) {
      OrderBy orderBy = orderByFromSortModel();
      if (!orderBy.orderByAttributes().isEmpty()) {
        return orderBy;
      }
    }

    return entityDefinition().orderBy();
  }

  /**
   * Specifies the attributes to select when querying data. Return an empty list if all should be included.
   * This method should take the {@link #isQueryHiddenColumns()} setting into account.
   * @return the attributes to select when querying data, an empty Collection if all should be selected.
   * @see #isQueryHiddenColumns()
   */
  protected Collection<Attribute<?>> selectAttributes() {
    if (queryHiddenColumns || columnModel().hiddenColumns().isEmpty()) {
      return emptyList();
    }

    return entityDefinition().defaultSelectAttributes().stream()
            .filter(columnModel()::isColumnVisible)
            .collect(toList());
  }

  /**
   * Returns the key used to identify user preferences for this table model, that is column positions, widths and such.
   * The default implementation is:
   * <pre>
   * {@code
   * return getClass().getSimpleName() + "-" + entityType();
   * }
   * </pre>
   * Override in case this key is not unique.
   * @return the key used to identify user preferences for this table model
   */
  protected String userPreferencesKey() {
    return getClass().getSimpleName() + "-" + entityType();
  }

  /**
   * Clears any user preferences saved for this table model
   */
  final void clearPreferences() {
    UserPreferences.removeUserPreference(userPreferencesKey());
  }

  private void bindEventsInternal() {
    columnModel().addColumnHiddenListener(this::onColumnHidden);
    addRefreshListener(this::rememberCondition);
    tableConditionModel.addConditionChangedListener(condition ->
            conditionChangedState.set(!Objects.equals(refreshCondition, condition)));
    editModel.addAfterInsertListener(this::onInsert);
    editModel.addAfterUpdateListener(this::onUpdate);
    editModel.addAfterDeleteListener(this::onDelete);
    editModel.addRefreshListener(this::refresh);
    editModel.addEntityListener(this::onEntitySet);
    editModel.addRefreshingObserver(refreshingObserver());
    selectionModel().addSelectedItemListener(editModel::setEntity);
    addTableModelListener(this::onTableModelEvent);
    EventListener statusListener = () -> statusMessageValue.set(statusMessage());
    selectionModel().addSelectionListener(statusListener);
    addFilterListener(statusListener);
    addDataChangedListener(statusListener);
  }

  private void addEditEventListeners() {
    entityDefinition().foreignKeys().forEach(foreignKey -> EntityEditEvents.addUpdateListener(foreignKey.referencedType(), updateListener));
  }

  private void removeEditEventListeners() {
    entityDefinition().foreignKeys().forEach(foreignKey -> EntityEditEvents.removeUpdateListener(foreignKey.referencedType(), updateListener));
  }

  private void rememberCondition() {
    refreshCondition = tableConditionModel.condition();
    conditionChangedState.set(false);
  }

  private void onInsert(List<Entity> insertedEntities) {
    selectionModel().clearSelection();
    if (!insertAction.equals(InsertAction.DO_NOTHING)) {
      List<Entity> entitiesToAdd = insertedEntities.stream()
              .filter(entity -> entity.type().equals(entityType()))
              .collect(toList());
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

  private void onUpdate(Map<Key, Entity> updatedEntities) {
    replaceEntitiesByKey(new HashMap<>(updatedEntities));
  }

  private void onDelete(List<Entity> deletedEntities) {
    if (removeDeletedEntities) {
      removeItems(deletedEntities);
    }
  }

  private void onEntitySet(Entity entity) {
    if (entity == null && !selectionModel().isSelectionEmpty()) {
      selectionModel().clearSelection();
    }
  }

  private void onTableModelEvent(TableModelEvent tableModelEvent) {
    //if the selected record is updated via the table model, refresh the one in the edit model
    if (tableModelEvent.getType() == TableModelEvent.UPDATE && tableModelEvent.getFirstRow() == selectionModel().getSelectedIndex()) {
      editModel.setEntity(selectionModel().getSelectedItem());
    }
  }

  /**
   * Replace the entities identified by the Entity.Key map keys with their respective value.
   * Note that this does not trigger {@link #filterItems()}, that must be done explicitly.
   * @param entitiesByKey the entities to replace mapped to the corresponding primary key found in this table model
   */
  private void replaceEntitiesByKey(Map<Key, Entity> entitiesByKey) {
    for (Entity entity : items()) {
      Iterator<Map.Entry<Key, Entity>> mapIterator = entitiesByKey.entrySet().iterator();
      while (mapIterator.hasNext()) {
        Map.Entry<Key, Entity> entry = mapIterator.next();
        if (entity.primaryKey().equals(entry.getKey())) {
          mapIterator.remove();
          entity.setAs(entry.getValue());
          int index = indexOf(entity);
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

  private void onColumnHidden(Attribute<?> attribute) {
    //disable the condition and filter model for the column to be hidden, to prevent confusion
    ColumnConditionModel<?, ?> conditionModel = tableConditionModel.conditionModels().get(attribute);
    if (conditionModel != null && !conditionModel.isLocked()) {
      conditionModel.setEnabled(false);
    }
    ColumnConditionModel<?, ?> filterModel = tableConditionModel.filterModels().get(attribute);
    if (filterModel != null && !filterModel.isLocked()) {
      filterModel.setEnabled(false);
    }
  }

  private OrderBy orderByFromSortModel() {
    OrderBy.Builder builder = OrderBy.builder();
    sortModel().columnSortOrder().stream()
            .filter(columnSortOrder -> isColumnProperty(columnSortOrder.columnIdentifier()))
            .forEach(columnSortOrder -> {
              if (columnSortOrder.sortOrder() == SortOrder.ASCENDING) {
                builder.ascending(columnSortOrder.columnIdentifier());
              }
              else {
                builder.descending(columnSortOrder.columnIdentifier());
              }
            });

    return builder.build();
  }

  private boolean isColumnProperty(Attribute<?> attribute) {
    Property<?> property = entityDefinition().property(attribute);

    return property instanceof ColumnProperty;
  }

  private JSONObject createPreferences() throws Exception {
    JSONObject preferencesRoot = new JSONObject();
    preferencesRoot.put(ColumnPreferences.PREFERENCES_COLUMNS, createColumnPreferences());

    return preferencesRoot;
  }

  private JSONObject createColumnPreferences() throws Exception {
    JSONObject columnPreferencesRoot = new JSONObject();
    for (FilteredTableColumn<Attribute<?>> column : columnModel().columns()) {
      Attribute<?> attribute = column.getIdentifier();
      boolean visible = columnModel().isColumnVisible(attribute);
      ColumnPreferences columnPreferences = EntityTableModel.columnPreferences(attribute,
              visible ? columnModel().getColumnIndex(attribute) : -1, column.getWidth());
      columnPreferencesRoot.put(attribute.name(), toJSONObject(columnPreferences));
    }

    return columnPreferencesRoot;
  }

  private void applyPreferences() {
    if (EntityModel.USE_CLIENT_PREFERENCES.get()) {
      String preferencesString = UserPreferences.getUserPreference(userPreferencesKey(), "");
      try {
        if (!preferencesString.isEmpty()) {
          applyColumnPreferences(preferencesString);
        }
      }
      catch (Exception e) {
        LOG.error("Error while applying preferences: " + preferencesString, e);
      }
    }
  }

  private void applyColumnPreferences(String preferencesString) {
    JSONObject preferences = new JSONObject(preferencesString).getJSONObject(ColumnPreferences.PREFERENCES_COLUMNS);
    Map<Attribute<?>, ColumnPreferences> preferenceMap = createColumnPreferenceMap(columnModel().columns(), preferences);
    List<Attribute<?>> columnAttributesWithoutPreferences = new ArrayList<>();
    for (FilteredTableColumn<Attribute<?>> column : columnModel().columns()) {
      Attribute<?> attribute = column.getIdentifier();
      ColumnPreferences columnPreferences = preferenceMap.get(attribute);
      if (columnPreferences == null) {
        columnAttributesWithoutPreferences.add(attribute);
      }
      else {
        column.setPreferredWidth(columnPreferences.width());
      }
    }
    List<Attribute<?>> columnAttributes = preferenceMap.values().stream()
            .filter(ColumnPreferences::isVisible)
            .sorted(Comparator.comparingInt(ColumnPreferences::index))
            .map(ColumnPreferences::attribute)
            .collect(toList());
    columnAttributes.addAll(0, columnAttributesWithoutPreferences);
    setVisibleColumns(columnAttributes);
  }

  private String statusMessage() {
    int filteredItemCount = filteredItemCount();

    return STATUS_MESSAGE_NUMBER_FORMAT.format(getRowCount()) + " (" +
            STATUS_MESSAGE_NUMBER_FORMAT.format(selectionModel().selectionCount()) + " " +
            MESSAGES.getString("selected") + (filteredItemCount > 0 ? " - " +
            STATUS_MESSAGE_NUMBER_FORMAT.format(filteredItemCount) + " " + MESSAGES.getString("hidden") + ")" : ")");
  }

  private static Map<Attribute<?>, ColumnPreferences> createColumnPreferenceMap(Collection<FilteredTableColumn<Attribute<?>>> tableColumns, JSONObject preferences) {
    return tableColumns.stream()
            .map(tableColumn -> columnPreferences(tableColumn, preferences))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toMap(ColumnPreferences::attribute, Function.identity()));
  }

  private static Optional<ColumnPreferences> columnPreferences(FilteredTableColumn<Attribute<?>> tableColumn, JSONObject preferences) {
    Attribute<?> attribute = tableColumn.getIdentifier();
    try {
      return Optional.of(fromJSONObject(attribute, preferences.getJSONObject(attribute.name())));
    }
    catch (Exception e) {
      LOG.info("Attribute preferences not found: " + attribute, e);
      return Optional.empty();
    }
  }

  private static JSONObject toJSONObject(ColumnPreferences columnPreferences) {
    JSONObject columnObject = new JSONObject();
    columnObject.put(ColumnPreferences.PREFERENCES_COLUMN_WIDTH, columnPreferences.width());
    columnObject.put(ColumnPreferences.PREFERENCES_COLUMN_INDEX, columnPreferences.index());

    return columnObject;
  }

  private static ColumnPreferences fromJSONObject(Attribute<?> attribute, JSONObject jsonObject) {
    return EntityTableModel.columnPreferences(attribute,
            jsonObject.getInt(ColumnPreferences.PREFERENCES_COLUMN_INDEX),
            jsonObject.getInt(ColumnPreferences.PREFERENCES_COLUMN_WIDTH));
  }

  private final class UpdateListener implements EventDataListener<Map<Key, Entity>> {

    @Override
    public void onEvent(Map<Key, Entity> updated) {
      updated.values().stream()
              .collect(groupingBy(Entity::type, HashMap::new, toList()))
              .forEach((entityType, entities) ->
                      entityDefinition().foreignKeys(entityType).forEach(foreignKey ->
                              replaceForeignKeyValues(foreignKey, entities)));
    }
  }

  private static final class EntityColumnValueProvider implements ColumnValueProvider<Entity, Attribute<?>> {

    private final Entities entities;

    private EntityColumnValueProvider(Entities entities) {
      this.entities = entities;
    }

    @Override
    public Class<?> columnClass(Attribute<?> attribute) {
      return attribute.valueClass();
    }

    @Override
    public Comparator<?> comparator(Attribute<?> attribute) {
      if (attribute instanceof ForeignKey) {
        return entities.definition(((ForeignKey) attribute).referencedType()).comparator();
      }

      return entities.definition(attribute.entityType()).property(attribute).comparator();
    }

    @Override
    public Object value(Entity entity, Attribute<?> attribute) {
      return entity.get(attribute);
    }

    @Override
    public String string(Entity entity, Attribute<?> attribute) {
      return entity.toString(attribute);
    }

    @Override
    public <T> Comparable<T> comparable(Entity entity, Attribute<?> attribute) {
      if (entity.isNull(attribute)) {
        return null;
      }

      Object value = entity.get(attribute);
      if (value instanceof Entity) {
        return (Comparable<T>) value.toString();
      }

      return (Comparable<T>) value;
    }
  }
}