/*
 * Copyright (c) 2015 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.model.UserPreferences;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
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
import is.codion.framework.model.EntityConditionModelFactory;
import is.codion.framework.model.EntityEditEvents;
import is.codion.framework.model.EntityModel;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.framework.model.EntityTableModel.ColumnPreferences.ConditionPreferences;
import is.codion.javafx.framework.ui.EntityTableColumn;

import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.paint.Color;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static is.codion.framework.model.EntityTableConditionModel.entityTableConditionModel;
import static is.codion.framework.model.EntityTableModel.ColumnPreferences.ConditionPreferences.conditionPreferences;
import static is.codion.framework.model.EntityTableModel.ColumnPreferences.columnPreferences;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static javafx.collections.FXCollections.emptyObservableList;

/**
 * A JavaFX implementation of {@link EntityTableModel}.
 */
public class FXEntityListModel extends EntityObservableList implements EntityTableModel<FXEntityEditModel> {

  private static final Logger LOG = LoggerFactory.getLogger(FXEntityListModel.class);

  private final EntityTableConditionModel<Attribute<?>> conditionModel;
  private final State queryConditionRequiredState = State.state();
  private final FXEntityEditModel editModel;
  private final State conditionChangedState = State.state();
  private final EventDataListener<Map<Key, Entity>> updateListener = new UpdateListener();

  private ObservableList<? extends TableColumn<Entity, ?>> columns = emptyObservableList();
  private ObservableList<TableColumn<Entity, ?>> columnSortOrder = emptyObservableList();
  private List<AttributeTableColumn<?>> initialColumns = emptyList();

  private Condition refreshCondition;
  private InsertAction insertAction = InsertAction.ADD_TOP;
  private boolean batchUpdateEnabled = true;
  private boolean removeDeletedEntities = true;
  private boolean editable = false;
  private int limit = -1;
  private boolean queryHiddenColumns = QUERY_HIDDEN_COLUMNS.get();
  private boolean orderQueryBySortOrder = ORDER_QUERY_BY_SORT_ORDER.get();
  private boolean listenToEditEvents = true;

  public FXEntityListModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    this(new FXEntityEditModel(entityType, connectionProvider));
  }

  public FXEntityListModel(FXEntityEditModel editModel) {
    this(requireNonNull(editModel), new FXEntityConditionModelFactory(editModel.connectionProvider()));
  }

  public FXEntityListModel(FXEntityEditModel editModel, EntityConditionModelFactory conditionModelFactory) {
    super(requireNonNull(editModel).entityType(), editModel.connectionProvider());
    this.editModel = editModel;
    this.conditionModel = entityTableConditionModel(editModel.entityType(), editModel.connectionProvider(), requireNonNull(conditionModelFactory));
    this.refreshCondition = conditionModel.condition();
    addEditEventListeners();
    bindEvents();
  }

  @Override
  public final Entities entities() {
    return connectionProvider().entities();
  }

  @Override
  public final FXEntityEditModel editModel() {
    return editModel;
  }

  /**
   * Sets the columns for this {@link FXEntityListModel}.
   * @param columns the columns
   * @throws IllegalStateException if the columns have already been set
   */
  public final void setColumns(ObservableList<? extends TableColumn<Entity, ?>> columns) {
    if (!this.columns.equals(emptyObservableList())) {
      throw new IllegalStateException("Columns have already been set");
    }
    this.columns = columns;
    this.initialColumns = new ArrayList<>((Collection<AttributeTableColumn<?>>) columns);
    applyPreferences();
  }

  /**
   * Sets the column sort order
   * @param columnSortOrder the column sort order
   */
  public final void setColumnSortOrder(ObservableList<TableColumn<Entity, ?>> columnSortOrder) {
    if (!this.columnSortOrder.equals(emptyObservableList())) {
      throw new IllegalStateException("Column sort order has already been set");
    }
    this.columnSortOrder = columnSortOrder;
  }

  /**
   * @return the column sort order
   */
  public final ObservableList<TableColumn<Entity, ?>> getColumnSortOrder() {
    return columnSortOrder;
  }

  /**
   * Returns the table column for the given attribute
   * @param <T> the column value type
   * @param attribute the attribute
   * @return the column
   * @throws IllegalArgumentException in case the column was not found
   */
  public final <T> EntityTableColumn<T> tableColumn(Attribute<T> attribute) {
    Optional<? extends TableColumn<Entity, ?>> tableColumn = columns.stream()
            .filter((Predicate<TableColumn<Entity, ?>>) entityTableColumn ->
                    ((EntityTableColumn<?>) entityTableColumn).attribute().equals(attribute))
            .findFirst();

    if (tableColumn.isPresent()) {
      return (EntityTableColumn<T>) tableColumn.get();
    }

    throw new IllegalArgumentException("Column for attribute: " + attribute + " not found");
  }

  @Override
  public final EntityTableConditionModel<Attribute<?>> conditionModel() {
    return conditionModel;
  }

  @Override
  public final State queryConditionRequiredState() {
    return queryConditionRequiredState;
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
  public final boolean setForeignKeyConditionValues(ForeignKey foreignKey, Collection<Entity> foreignKeyValues) {
    requireNonNull(foreignKey, "foreignKey");
    requireNonNull(foreignKeyValues, "foreignKeyValues");
    entityDefinition().foreignKeyProperty(foreignKey);

    return conditionModel.setEqualConditionValues(foreignKey, foreignKeyValues);
  }

  @Override
  public final void deleteSelected() throws DatabaseException {
    editModel().delete(selectionModel().getSelectedItems());
  }

  @Override
  public final int getRowCount() {
    return size();
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
          entity.put(foreignKey, foreignKeyValue.immutable());
          changed = true;
        }
      }
    }
    if (changed) {
      fireValueChangedEvent();
    }
  }

  @Override
  public final void replaceEntities(Collection<Entity> entities) {
    replaceEntitiesByKey(Entity.mapToPrimaryKey(entities));
  }

  @Override
  public final void refreshEntities(Collection<Key> keys) {
    try {
      replaceEntities(connectionProvider().connection().select(keys));
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public final boolean isDeleteEnabled() {
    return editModel != null && editModel.isDeleteEnabled();
  }

  @Override
  public final boolean isReadOnly() {
    return editModel == null || editModel.isReadOnly();
  }

  @Override
  public final boolean isUpdateEnabled() {
    return editModel != null && editModel.isUpdateEnabled();
  }

  @Override
  public final boolean isMultipleEntityUpdateEnabled() {
    return batchUpdateEnabled;
  }

  @Override
  public final void setMultipleEntityUpdateEnabled(boolean multipleEntityUpdateEnabled) {
    this.batchUpdateEnabled = multipleEntityUpdateEnabled;
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
  public final Color backgroundColor(int row, Attribute<?> attribute) {
    return (Color) entityDefinition().backgroundColorProvider().color(get(row), attribute);
  }

  @Override
  public final Color foregroundColor(int row, Attribute<?> attribute) {
    return (Color) entityDefinition().foregroundColorProvider().color(get(row), attribute);
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
  public final void update(Collection<? extends Entity> entities) throws ValidationException, DatabaseException {
    requireNonNull(entities);
    if (!isUpdateEnabled()) {
      throw new IllegalStateException("Updating is not allowed via this table model");
    }
    if (entities.size() > 1 && !batchUpdateEnabled) {
      throw new IllegalStateException("Batch update of entities is not allowed!");
    }
    editModel.update(entities);
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
  public final InsertAction getInsertAction() {
    return insertAction;
  }

  @Override
  public final void setInsertAction(InsertAction insertAction) {
    this.insertAction = requireNonNull(insertAction);
  }

  @Override
  public final Collection<Entity> entitiesByKey(Collection<Key> keys) {
    return items().stream()
            .filter(entity -> keys.stream()
                    .anyMatch(key -> entity.primaryKey().equals(key)))
            .collect(toList());
  }

  @Override
  public final void selectEntitiesByKey(Collection<Key> keys) {
    selectionModel().setSelectedItems(new SelectByKeyPredicate(requireNonNull(keys, "keys")));
  }

  @Override
  public final Entity entityByKey(Key primaryKey) {
    return filteredList().stream()
            .filter(entity -> entity.primaryKey().equals(primaryKey))
            .findFirst()
            .orElse(null);
  }

  @Override
  public final int indexOf(Key primaryKey) {
    return indexOf(entityByKey(primaryKey));
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
  public final void setVisibleColumns(Attribute<?>... attributes) {
    setVisibleColumns(asList(attributes));
  }

  @Override
  public final void setVisibleColumns(List<Attribute<?>> attributes) {
    requireNonNull(attributes);
    new ArrayList<>(columns).forEach(column -> {
      if (!attributes.contains(((AttributeTableColumn<?>) column).attribute())) {
        columns.remove(column);
      }
    });
    columns.sort((col1, col2) -> {
      Integer first = attributes.indexOf(((AttributeTableColumn<?>) col1).attribute());
      Integer second = attributes.indexOf(((AttributeTableColumn<?>) col2).attribute());

      return first.compareTo(second);
    });
  }

  public final String tableDataAsDelimitedString(char delimiter) {
    List<String> header = new ArrayList<>();
    List<Attribute<?>> attributes = new ArrayList<>();
    columns.forEach(entityTableColumn -> {
      Attribute<?> attribute = ((AttributeTableColumn<?>) entityTableColumn).attribute();
      attributes.add(attribute);
      header.add(entityDefinition().property(attribute).caption());
    });

    return Text.delimitedString(header, Entity.valuesAsString(attributes,
                    selectionModel().isSelectionEmpty() ? visibleItems() : selectionModel().getSelectedItems()),
            String.valueOf(delimiter));
  }

  @Override
  public final StateObserver conditionChangedObserver() {
    return conditionChangedState.observer();
  }

  /**
   * Queries for the data used to populate this EntityTableModel when it is refreshed,
   * using the order by clause returned by {@link #orderBy()}
   * @return entities selected from the database according the query condition.
   * @see #queryConditionRequiredState()
   * @see EntityTableConditionModel#condition()
   */
  @Override
  protected List<Entity> performQuery() {
    if (!conditionModel.isEnabled() && queryConditionRequiredState.get()) {
      return emptyList();
    }

    try {
      return connectionProvider().connection().select(conditionModel.condition()
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
   * The order by clause to use when selecting the data for this model,
   * by default the order by clause defined for the underlying entity
   * @return the order by clause
   * @see EntityDefinition#orderBy()
   */
  protected OrderBy orderBy() {
    if (orderQueryBySortOrder && columnSortOrder != null && !columnSortOrder.isEmpty()) {
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
   * @return the attributes to select when querying data, an empty list if all should be selected.
   * @see #isQueryHiddenColumns()
   */
  protected Collection<Attribute<?>> selectAttributes() {
    if (queryHiddenColumns || initialColumns.size() == columns.size()) {
      return emptyList();
    }

    return entityDefinition().defaultSelectAttributes().stream()
            .filter(this::containsColumn)
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

  @Override
  protected final void bindSelectionModelEvents() {
    super.bindSelectionModelEvents();
    selectionModel().addSelectedIndexListener(index -> {
      if (editModel != null) {
        editModel.setEntity(selectionModel().getSelectedItem());
      }
    });
  }

  private void onInsert(Collection<Entity> insertedEntities) {
    selectionModel().clearSelection();
    if (!insertAction.equals(InsertAction.DO_NOTHING)) {
      Collection<Entity> entitiesToAdd = insertedEntities.stream()
              .filter(entity -> entity.type().equals(entityType()))
              .collect(toList());
      switch (insertAction) {
        case ADD_TOP:
          addAll(0, entitiesToAdd);
          break;
        case ADD_BOTTOM:
          addAll(entitiesToAdd);
          break;
        case ADD_TOP_SORTED:
          addAll(0, entitiesToAdd);
          sort(sortedList().getComparator());
          break;
      }
    }
  }

  private void onUpdate(Map<Key, Entity> updatedEntities) {
    replaceEntitiesByKey(new HashMap<>(updatedEntities));
  }

  private void onDelete(Collection<Entity> deletedEntities) {
    if (removeDeletedEntities) {
      removeAll(deletedEntities);
    }
  }

  private boolean containsColumn(Attribute<?> attribute) {
    return columns.stream()
            .map(EntityTableColumn.class::cast)
            .anyMatch(column -> column.attribute().equals(attribute));
  }

  /**
   * Replace the entities identified by the Entity.Key map keys with their respective value
   * @param entityMap the entities to replace mapped to the corresponding primary key found in this table model
   */
  private void replaceEntitiesByKey(Map<Key, Entity> entityMap) {
    List<Integer> selected = selectionModel().getSelectedIndexes();
    replaceAll(entity -> {
      Entity toReplaceWith = entityMap.get(entity.primaryKey());
      return toReplaceWith == null ? entity : toReplaceWith;
    });
    selectionModel().setSelectedIndexes(selected);
  }

  private OrderBy orderByFromSortModel() {
    OrderBy.Builder builder = OrderBy.builder();
    columnSortOrder.stream()
            .map(EntityTableColumn.class::cast)
            .filter(column -> isColumnProperty(column.attribute()))
            .forEach(column -> {
              if (column.getSortType() == SortType.ASCENDING) {
                builder.ascending(column.attribute());
              }
              else {
                builder.descending(column.attribute());
              }
            });

    return builder.build();
  }

  private boolean isColumnProperty(Attribute<?> attribute) {
    Property<?> property = entityDefinition().property(attribute);

    return property instanceof ColumnProperty;
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
    List<Attribute<?>> columnAttributes = initialColumns.stream()
            .map(AttributeTableColumn::attribute)
            .collect(toList());
    Map<Attribute<?>, ColumnPreferences> columnPreferences =
            ColumnPreferences.fromJSONObject(columnAttributes, new JSONObject(preferencesString).getJSONObject(ColumnPreferences.COLUMNS));
    ColumnPreferences.applyColumnPreferences(this, columnAttributes, columnPreferences, (attribute, columnWidth) ->
            tableColumn(attribute).setPrefWidth(columnWidth));
  }

  private JSONObject createPreferences() throws Exception {
    JSONObject preferencesRoot = new JSONObject();
    preferencesRoot.put(ColumnPreferences.COLUMNS, createColumnPreferences());

    return preferencesRoot;
  }

  private JSONObject createColumnPreferences() throws Exception {
    JSONObject columnPreferencesRoot = new JSONObject();
    for (AttributeTableColumn<?> column : initialColumns) {
      Attribute<?> attribute = column.attribute();
      int index = columns.indexOf(column);
      ColumnConditionModel<?, ?> columnConditionModel = conditionModel.conditionModels().get(attribute);
      ConditionPreferences conditionPreferences = columnConditionModel != null ?
              conditionPreferences(columnConditionModel.autoEnableState().get(),
                      columnConditionModel.caseSensitiveState().get(),
                      columnConditionModel.automaticWildcardValue().get()) : null;
      ColumnPreferences columnPreferences = columnPreferences(attribute, index, (int) column.getWidth(), conditionPreferences);
      columnPreferencesRoot.put(attribute.name(), columnPreferences.toJSONObject());
    }

    return columnPreferencesRoot;
  }

  private void bindEvents() {
    refresher().addRefreshListener(this::rememberCondition);
    conditionModel.addChangeListener(condition ->
            conditionChangedState.set(!Objects.equals(refreshCondition, condition)));
    editModel.addAfterInsertListener(this::onInsert);
    editModel.addAfterUpdateListener(this::onUpdate);
    editModel.addAfterDeleteListener(this::onDelete);
    editModel.addRefreshListener(this::refresh);
    editModel.addRefreshingObserver(refresher().refreshingObserver());
    editModel.addEntityListener(entity -> {
      if (entity == null && selectionModelHasBeenSet() && selectionModel().isSelectionNotEmpty()) {
        selectionModel().clearSelection();
      }
    });
  }

  private void addEditEventListeners() {
    entityDefinition().foreignKeys().forEach(foreignKey -> EntityEditEvents.addUpdateListener(foreignKey.referencedType(), updateListener));
  }

  private void removeEditEventListeners() {
    entityDefinition().foreignKeys().forEach(foreignKey -> EntityEditEvents.removeUpdateListener(foreignKey.referencedType(), updateListener));
  }

  private void rememberCondition() {
    refreshCondition = conditionModel.condition();
    conditionChangedState.set(false);
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

  /**
   * A {@link TableColumn} based on a {@link Attribute} instance
   * @param <T> the column data type
   */
  public static class AttributeTableColumn<T> extends TableColumn<Entity, T> {

    private final Attribute<T> attribute;

    protected AttributeTableColumn(Attribute<T> attribute, String caption) {
      super(caption);
      this.attribute = attribute;
    }

    /**
     * @return the underlying property
     */
    public final Attribute<T> attribute() {
      return attribute;
    }

    @Override
    public final String toString() {
      return attribute.name();
    }
  }

  private static final class SelectByKeyPredicate implements Predicate<Entity> {

    private final List<Key> keyList;

    private SelectByKeyPredicate(Collection<Key> keys) {
      this.keyList = new ArrayList<>(keys);
    }

    @Override
    public boolean test(Entity entity) {
      if (keyList.isEmpty()) {
        return false;
      }
      int index = keyList.indexOf(entity.primaryKey());
      if (index >= 0) {
        keyList.remove(index);
        return true;
      }

      return false;
    }
  }
}
