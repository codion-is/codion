/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.UserPreferences;
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
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.DefaultEntityTableConditionModel;
import is.codion.framework.model.EntityModel;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.javafx.framework.ui.EntityTableColumn;

import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A JavaFX implementation of {@link EntityTableModel}.
 */
public class FXEntityListModel extends ObservableEntityList implements EntityTableModel<FXEntityEditModel> {

  private static final Logger LOG = LoggerFactory.getLogger(FXEntityListModel.class);

  private final EntityTableConditionModel tableConditionModel;
  private final State queryConditionRequiredState = State.state();
  private final FXEntityEditModel editModel;

  private ObservableList<? extends TableColumn<Entity, ?>> columns;
  private ObservableList<TableColumn<Entity, ?>> columnSortOrder;
  private List<AttributeTableColumn<?>> initialColumns;

  private InsertAction insertAction = InsertAction.ADD_TOP;
  private boolean batchUpdateEnabled = true;
  private boolean removeEntitiesOnDelete = true;
  private boolean refreshOnForeignKeyConditionValuesSet = true;
  private boolean editable = false;
  private int limit = -1;
  private boolean queryHiddenColumns = QUERY_HIDDEN_COLUMNS.get();
  private boolean orderQueryBySortOrder = ORDER_QUERY_BY_SORT_ORDER.get();

  public FXEntityListModel(final EntityType entityType, final EntityConnectionProvider connectionProvider) {
    this(new FXEntityEditModel(entityType, connectionProvider));
  }


  public FXEntityListModel(final FXEntityEditModel editModel) {
    this(editModel, new DefaultEntityTableConditionModel(editModel.getEntityType(), editModel.getConnectionProvider(),
            null, new FXConditionModelFactory(editModel.getConnectionProvider())));
  }

  public FXEntityListModel(final FXEntityEditModel editModel, final EntityTableConditionModel tableConditionModel) {
    super(editModel.getEntityType(), editModel.getConnectionProvider());
    requireNonNull(tableConditionModel);
    if (!tableConditionModel.getEntityType().equals(getEntityType())) {
      throw new IllegalArgumentException("Entity ID mismatch, conditionModel: " + tableConditionModel.getEntityType()
              + ", tableModel: " + getEntityType());
    }
    if (getEntityDefinition().getVisibleProperties().isEmpty()) {
      throw new IllegalArgumentException("No visible properties defined for entity: " + getEntityType());
    }
    this.editModel = editModel;
    this.tableConditionModel = tableConditionModel;
    bindEvents();
  }

  @Override
  public final Entities getEntities() {
    return getConnectionProvider().getEntities();
  }

  @Override
  public final FXEntityEditModel getEditModel() {
    return editModel;
  }

  /**
   * Sets the columns for this {@link FXEntityListModel}.
   * @param columns the columns
   * @throws IllegalStateException if the columns have already been set
   */
  public final void setColumns(final ObservableList<? extends TableColumn<Entity, ?>> columns) {
    if (this.columns != null) {
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
  public final void setColumnSortOrder(final ObservableList<TableColumn<Entity, ?>> columnSortOrder) {
    if (this.columnSortOrder != null) {
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
  public final <T> EntityTableColumn<T> getTableColumn(final Attribute<T> attribute) {
    final Optional<? extends TableColumn<Entity, ?>> tableColumn = columns.stream()
            .filter((Predicate<TableColumn<Entity, ?>>) entityTableColumn ->
                    ((EntityTableColumn<?>) entityTableColumn).getAttribute().equals(attribute))
            .findFirst();

    if (tableColumn.isPresent()) {
      return (EntityTableColumn<T>) tableColumn.get();
    }

    throw new IllegalArgumentException("Column for attribute: " + attribute + " not found");
  }

  @Override
  public final EntityTableConditionModel getTableConditionModel() {
    return tableConditionModel;
  }

  @Override
  public final State getQueryConditionRequiredState() {
    return queryConditionRequiredState;
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
  public final void setForeignKeyConditionValues(final ForeignKey foreignKey, final Collection<Entity> entities) {
    getEntityDefinition().getForeignKeyProperty(foreignKey);
    if (tableConditionModel.setEqualConditionValues(foreignKey, entities) && refreshOnForeignKeyConditionValuesSet) {
      refresh();
    }
  }

  @Override
  public final void deleteSelected() throws DatabaseException {
    getEditModel().delete(getSelectionModel().getSelectedItems());
  }

  @Override
  public final int getRowCount() {
    return size();
  }

  @Override
  public final void replaceForeignKeyValues(final EntityType foreignKeyEntityType, final Collection<Entity> foreignKeyValues) {
    final List<ForeignKey> foreignKeys = getEntityDefinition().getForeignKeys(foreignKeyEntityType);
    for (final Entity entity : getItems()) {
      for (final ForeignKey foreignKey : foreignKeys) {
        for (final Entity foreignKeyValue : foreignKeyValues) {
          final Entity currentForeignKeyValue = entity.getForeignKey(foreignKey);
          if (Objects.equals(currentForeignKeyValue, foreignKeyValue)) {
            currentForeignKeyValue.setAs(foreignKeyValue);
          }
        }
      }
    }
  }

  @Override
  public void addEntities(final Collection<Entity> entities) {
    addEntitiesAt(getSize(), entities);
  }

  @Override
  public void addEntitiesSorted(final Collection<Entity> entities) {
    addEntitiesAtSorted(getSize(), entities);
  }

  @Override
  public void addEntitiesAt(final int index, final Collection<Entity> entities) {
    addAll(index, entities);
  }

  @Override
  public void addEntitiesAtSorted(final int index, final Collection<Entity> entities) {
    addAll(index, entities);
    sort(getSortedList().getComparator());
  }

  @Override
  public final void replaceEntities(final Collection<Entity> entities) {
    replaceEntitiesByKey(Entity.mapToPrimaryKey(entities));
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
  public final boolean isQueryHiddenColumns() {
    return queryHiddenColumns;
  }

  @Override
  public final void setQueryHiddenColumns(final boolean queryHiddenColumns) {
    this.queryHiddenColumns = queryHiddenColumns;
  }

  @Override
  public final boolean isOrderQueryBySortOrder() {
    return orderQueryBySortOrder;
  }

  @Override
  public final void setOrderQueryBySortOrder(final boolean orderQueryBySortOrder) {
    this.orderQueryBySortOrder = orderQueryBySortOrder;
  }

  @Override
  public final Color getBackgroundColor(final int row, final Attribute<?> attribute) {
    return (Color) getEntityDefinition().getBackgroundColorProvider().getColor(get(row), attribute);
  }

  @Override
  public final Color getForegroundColor(final int row, final Attribute<?> attribute) {
    return (Color) getEntityDefinition().getForegroundColorProvider().getColor(get(row), attribute);
  }

  @Override
  public final int getLimit() {
    return limit;
  }

  @Override
  public final void setLimit(final int limit) {
    this.limit = limit;
  }

  @Override
  public final void update(final List<Entity> entities) throws ValidationException, DatabaseException {
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
  public final boolean isRemoveEntitiesOnDelete() {
    return removeEntitiesOnDelete;
  }

  @Override
  public final void setRemoveEntitiesOnDelete(final boolean removeEntitiesOnDelete) {
    this.removeEntitiesOnDelete = removeEntitiesOnDelete;
  }

  @Override
  public final InsertAction getInsertAction() {
    return insertAction;
  }

  @Override
  public final void setInsertAction(final InsertAction insertAction) {
    this.insertAction = requireNonNull(insertAction);
  }

  @Override
  public final Collection<Entity> getEntitiesByKey(final Collection<Key> keys) {
    return getItems().stream()
            .filter(entity -> keys.stream()
                    .anyMatch(key -> entity.getPrimaryKey().equals(key)))
            .collect(toList());
  }

  @Override
  public final void setSelectedByKey(final Collection<Key> keys) {
    final List<Key> keyList = new ArrayList<>(keys);
    final List<Entity> toSelect = new ArrayList<>(keys.size());
    stream().filter(entity -> keyList.contains(entity.getPrimaryKey())).forEach(entity -> {
      toSelect.add(entity);
      keyList.remove(entity.getPrimaryKey());
    });
    getSelectionModel().setSelectedItems(toSelect);
  }

  @Override
  public final Entity getEntityByKey(final Key primaryKey) {
    return getFilteredList().stream()
            .filter(entity -> entity.getPrimaryKey().equals(primaryKey))
            .findFirst()
            .orElse(null);
  }

  @Override
  public final Iterator<Entity> getSelectedEntitiesIterator() {
    return getSelectionModel().getSelectedItems().iterator();
  }

  @Override
  public final int indexOf(final Key primaryKey) {
    return indexOf(getEntityByKey(primaryKey));
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
  public final void setColumns(final Attribute<?>... attributes) {
    final List<Attribute<?>> attributeList = asList(attributes);
    new ArrayList<>(columns).forEach(column -> {
      if (!attributeList.contains(((AttributeTableColumn<?>) column).getAttribute())) {
        columns.remove(column);
      }
    });
    columns.sort((col1, col2) -> {
      final Integer first = attributeList.indexOf(((AttributeTableColumn<?>) col1).getAttribute());
      final Integer second = attributeList.indexOf(((AttributeTableColumn<?>) col2).getAttribute());

      return first.compareTo(second);
    });
  }

  @Override
  public final String getTableDataAsDelimitedString(final char delimiter) {
    final List<String> header = new ArrayList<>();
    final List<Attribute<?>> attributes = new ArrayList<>();
    columns.forEach(entityTableColumn -> {
      final Attribute<?> attribute = ((AttributeTableColumn<?>) entityTableColumn).getAttribute();
      attributes.add(attribute);
      header.add(getEntityDefinition().getProperty(attribute).getCaption());
    });

    return Text.getDelimitedString(header, Entity.getStringValueList(attributes,
                    getSelectionModel().isSelectionEmpty() ? getVisibleItems() : getSelectionModel().getSelectedItems()),
            String.valueOf(delimiter));
  }

  /**
   * Queries for the data used to populate this EntityTableModel when it is refreshed,
   * using the order by clause returned by {@link #getOrderBy()}
   * @return entities selected from the database according the query condition.
   * @see #getQueryConditionRequiredState()
   * @see EntityTableConditionModel#getCondition()
   */
  @Override
  protected List<Entity> performQuery() {
    if (!tableConditionModel.isConditionEnabled() && queryConditionRequiredState.get()) {
      return emptyList();
    }

    checkQueryRowCount();
    try {
      return getConnectionProvider().getConnection().select(tableConditionModel.getCondition()
              .toSelectCondition()
              .selectAttributes(getSelectAttributes())
              .limit(limit)
              .orderBy(getOrderBy()));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the number of rows {@link #performQuery()} would return on next invocation
   */
  @Override
  protected int getQueryRowCount() {
    if (!tableConditionModel.isConditionEnabled() && queryConditionRequiredState.get()) {
      return 0;
    }

    try {
      return getConnectionProvider().getConnection().rowCount(tableConditionModel.getCondition());
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * The order by clause to use when selecting the data for this model,
   * by default the order by clause defined for the underlying entity
   * @return the order by clause
   * @see EntityDefinition#getOrderBy()
   */
  protected OrderBy getOrderBy() {
    if (orderQueryBySortOrder && columnSortOrder != null && !columnSortOrder.isEmpty()) {
      final OrderBy orderBy = getOrderByFromSortModel();
      if (!orderBy.getOrderByAttributes().isEmpty()) {
        return orderBy;
      }
    }

    return getEntityDefinition().getOrderBy();
  }

  /**
   * Specifies the attributes to select when querying data. Return an empty list if all should be included.
   * This method should take the {@link #isQueryHiddenColumns()} setting into account.
   * @return the attributes to select when querying data, an empty list if all should be selected.
   * @see #isQueryHiddenColumns()
   */
  protected Collection<Attribute<?>> getSelectAttributes() {
    if (queryHiddenColumns || initialColumns.size() == columns.size()) {
      return emptyList();
    }

    return getEntityDefinition().getDefaultSelectAttributes().stream()
            .filter(this::containsColumn)
            .collect(toList());
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

  @Override
  protected final void bindSelectionModelEvents() {
    super.bindSelectionModelEvents();
    getSelectionModel().addSelectedIndexListener(index -> {
      if (editModel != null) {
        editModel.setEntity(getSelectionModel().getSelectedItem());
      }
    });
  }

  private void onInsert(final List<Entity> insertedEntities) {
    getSelectionModel().clearSelection();
    if (!insertAction.equals(InsertAction.DO_NOTHING)) {
      final List<Entity> entitiesToAdd = insertedEntities.stream()
              .filter(entity -> entity.getEntityType().equals(getEntityType()))
              .collect(toList());
      switch (insertAction) {
        case ADD_TOP:
          addEntitiesAt(0, entitiesToAdd);
          break;
        case ADD_BOTTOM:
          addEntities(entitiesToAdd);
          break;
        case ADD_TOP_SORTED:
          addEntitiesAtSorted(0, entitiesToAdd);
          break;
      }
    }
  }

  private void onUpdate(final Map<Key, Entity> updatedEntities) {
    replaceEntitiesByKey(new HashMap<>(updatedEntities));
  }

  private void onDelete(final List<Entity> deletedEntities) {
    if (removeEntitiesOnDelete) {
      removeAll(deletedEntities);
    }
  }

  private boolean containsColumn(final Attribute<?> attribute) {
    return columns.stream()
            .map(EntityTableColumn.class::cast)
            .anyMatch(column -> column.getAttribute().equals(attribute));
  }

  /**
   * Replace the entities identified by the Entity.Key map keys with their respective value
   * @param entityMap the entities to replace mapped to the corresponding primary key found in this table model
   */
  private void replaceEntitiesByKey(final Map<Key, Entity> entityMap) {
    final List<Integer> selected = getSelectionModel().getSelectedIndexes();
    replaceAll(entity -> {
      final Entity toReplaceWith = entityMap.get(entity.getPrimaryKey());
      return toReplaceWith == null ? entity : toReplaceWith;
    });
    getSelectionModel().setSelectedIndexes(selected);
  }

  private OrderBy getOrderByFromSortModel() {
    final OrderBy orderBy = OrderBy.orderBy();
    columnSortOrder.stream()
            .map(EntityTableColumn.class::cast)
            .filter(column -> isColumnProperty(column.getAttribute()))
            .forEach(column -> {
              if (column.getSortType() == SortType.ASCENDING) {
                orderBy.ascending(column.getAttribute());
              }
              else {
                orderBy.descending(column.getAttribute());
              }
            });

    return orderBy;
  }

  private boolean isColumnProperty(final Attribute<?> attribute) {
    final Property<?> property = getEntityDefinition().getProperty(attribute);

    return property instanceof ColumnProperty;
  }

  private void applyPreferences() {
    if (EntityModel.USE_CLIENT_PREFERENCES.get()) {
      final String preferencesString = UserPreferences.getUserPreference(getUserPreferencesKey(), "");
      try {
        if (preferencesString.length() > 0) {
          final org.json.JSONObject preferences = new org.json.JSONObject(preferencesString).getJSONObject(PREFERENCES_COLUMNS);
          applyColumnPreferences(preferences);
          columns.sort(new ColumnOrder(preferences));
        }
      }
      catch (final Exception e) {
        LOG.error("Error while applying preferences: " + preferencesString, e);
      }
    }
  }

  private void applyColumnPreferences(final org.json.JSONObject preferences) {
    for (final AttributeTableColumn<?> column : initialColumns) {
      final Attribute<?> property = column.getAttribute();
      if (columns.contains(column)) {
        try {
          final org.json.JSONObject columnPreferences = preferences.getJSONObject(property.getName());
          column.setPrefWidth(columnPreferences.getInt(PREFERENCES_COLUMN_WIDTH));
          if (!columnPreferences.getBoolean(PREFERENCES_COLUMN_VISIBLE)) {
            columns.remove(column);
          }
        }
        catch (final Exception e) {
          LOG.info("Property preferences not found: " + property, e);
        }
      }
    }
  }

  private org.json.JSONObject createPreferences() throws Exception {
    final org.json.JSONObject preferencesRoot = new org.json.JSONObject();
    preferencesRoot.put(PREFERENCES_COLUMNS, createColumnPreferences());

    return preferencesRoot;
  }

  private org.json.JSONObject createColumnPreferences() throws Exception {
    final org.json.JSONObject columnPreferencesRoot = new org.json.JSONObject();
    for (final AttributeTableColumn<?> column : initialColumns) {
      final Attribute<?> property = column.getAttribute();
      final org.json.JSONObject columnObject = new org.json.JSONObject();
      final boolean visible = columns.contains(column);
      columnObject.put(PREFERENCES_COLUMN_WIDTH, column.getWidth());
      columnObject.put(PREFERENCES_COLUMN_VISIBLE, visible);
      columnObject.put(PREFERENCES_COLUMN_INDEX, visible ? columns.indexOf(column) : -1);
      columnPreferencesRoot.put(property.getName(), columnObject);
    }

    return columnPreferencesRoot;
  }

  private void bindEvents() {
    addRefreshListener(tableConditionModel::rememberCondition);
    editModel.addAfterInsertListener(this::onInsert);
    editModel.addAfterUpdateListener(this::onUpdate);
    editModel.addAfterDeleteListener(this::onDelete);
    editModel.addRefreshListener(this::refresh);
    editModel.addRefreshingObserver(getRefreshingObserver());
    editModel.addEntitySetListener(entity -> {
      if (entity == null && selectionModelHasBeenSet() && getSelectionModel().isSelectionNotEmpty()) {
        getSelectionModel().clearSelection();
      }
    });
  }

  private void checkQueryRowCount() {
    if (getQueryRowCountLimit() >= 0 && getQueryRowCount() > getQueryRowCountLimit()) {
      throw new IllegalStateException("Too many rows returned, add query condition");
    }
  }

  /**
   * A {@link TableColumn} based on a {@link Attribute} instance
   * @param <T> the column data type
   */
  public static class AttributeTableColumn<T> extends TableColumn<Entity, T> {

    private final Attribute<T> attribute;

    protected AttributeTableColumn(final Attribute<T> attribute, final String caption) {
      super(caption);
      this.attribute = attribute;
    }

    /**
     * @return the underlying property
     */
    public final Attribute<T> getAttribute() {
      return attribute;
    }

    @Override
    public final String toString() {
      return attribute.getName();
    }
  }

  private static final class ColumnOrder implements Comparator<TableColumn<Entity, ?>> {

    private final org.json.JSONObject preferences;

    private ColumnOrder(final org.json.JSONObject preferences) {
      this.preferences = preferences;
    }

    @Override
    public int compare(final TableColumn<Entity, ?> col1, final TableColumn<Entity, ?> col2) {
      try {
        final org.json.JSONObject columnOnePreferences = preferences.getJSONObject(((AttributeTableColumn<?>) col1).getAttribute().getName());
        final org.json.JSONObject columnTwoPreferences = preferences.getJSONObject(((AttributeTableColumn<?>) col2).getAttribute().getName());
        Integer firstIndex = columnOnePreferences.getInt(PREFERENCES_COLUMN_INDEX);
        if (firstIndex == null) {
          firstIndex = 0;
        }
        Integer secondIndex = columnTwoPreferences.getInt(PREFERENCES_COLUMN_INDEX);
        if (secondIndex == null) {
          secondIndex = 0;
        }

        return firstIndex.compareTo(secondIndex);
      }
      catch (final Exception e) {
        LOG.info("Property preferences not found", e);
      }

      return 0;
    }
  }
}
