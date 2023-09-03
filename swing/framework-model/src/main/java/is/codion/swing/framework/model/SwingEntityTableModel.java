/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.Operator;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.UserPreferences;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnSummaryModel.SummaryValueProvider;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.model.table.TableSummaryModel;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ItemColumnDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.EntityConditionModelFactory;
import is.codion.framework.model.EntityEditEvents;
import is.codion.framework.model.EntityModel;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.framework.model.EntityTableModel.ColumnPreferences.ConditionPreferences;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableSearchModel;
import is.codion.swing.common.model.component.table.FilteredTableSelectionModel;
import is.codion.swing.common.model.component.table.FilteredTableSortModel;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.Color;
import java.text.Format;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.model.EntityTableConditionModel.entityTableConditionModel;
import static is.codion.framework.model.EntityTableModel.ColumnPreferences.ConditionPreferences.conditionPreferences;
import static is.codion.framework.model.EntityTableModel.ColumnPreferences.columnPreferences;
import static is.codion.swing.common.model.component.table.FilteredTableModel.summaryValueProvider;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * A TableModel implementation for displaying and working with entities.
 */
public class SwingEntityTableModel implements EntityTableModel<SwingEntityEditModel>, FilteredTableModel<Entity, Attribute<?>> {

  private static final Logger LOG = LoggerFactory.getLogger(SwingEntityTableModel.class);

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(SwingEntityTableModel.class.getName());

  private static final NumberFormat STATUS_MESSAGE_NUMBER_FORMAT = NumberFormat.getIntegerInstance();

  private final FilteredTableModel<Entity, Attribute<?>> tableModel;
  private final SwingEntityEditModel editModel;
  private final EntityTableConditionModel<Attribute<?>> conditionModel;
  private final State conditionRequiredState = State.state();

  /**
   * Caches java.awt.Color instances parsed from hex strings via {@link #getColor(Object)}
   */
  private final Map<String, Color> colorCache = new ConcurrentHashMap<>();
  private final Value<String> statusMessageValue = Value.value("", "");
  private final State conditionChangedState = State.state();
  private final Consumer<Map<Entity.Key, Entity>> updateListener = new UpdateListener();

  /**
   * the condition used during the last successful refresh
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
  private boolean multipleEntityUpdateEnabled = true;

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
   * @param columnFactory the table column factory
   */
  public SwingEntityTableModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                               ColumnFactory<Attribute<?>> columnFactory) {
    this(new SwingEntityEditModel(entityType, connectionProvider), columnFactory);
  }

  /**
   * Instantiates a new SwingEntityTableModel.
   * @param entityType the entityType
   * @param connectionProvider the connection provider
   * @param conditionModelFactory the table condition model factory
   */
  public SwingEntityTableModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                               EntityConditionModelFactory conditionModelFactory) {
    this(new SwingEntityEditModel(entityType, connectionProvider), conditionModelFactory);
  }

  /**
   * Instantiates a new SwingEntityTableModel.
   * @param entityType the entityType
   * @param connectionProvider the connection provider
   * @param columnFactory the table column factory
   * @param conditionModelFactory the table condition model factory
   */
  public SwingEntityTableModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                               ColumnFactory<Attribute<?>> columnFactory,
                               EntityConditionModelFactory conditionModelFactory) {
    this(new SwingEntityEditModel(entityType, connectionProvider), columnFactory, conditionModelFactory);
  }

  /**
   * Instantiates a new SwingEntityTableModel.
   * @param editModel the edit model
   */
  public SwingEntityTableModel(SwingEntityEditModel editModel) {
    this(editModel, new SwingEntityColumnFactory(requireNonNull(editModel).entityDefinition()));
  }

  /**
   * Instantiates a new SwingEntityTableModel.
   * @param editModel the edit model
   * @param columnFactory the table column factory
   */
  public SwingEntityTableModel(SwingEntityEditModel editModel, ColumnFactory<Attribute<?>> columnFactory) {
    this(editModel, columnFactory, new SwingEntityConditionModelFactory(requireNonNull(editModel).connectionProvider()));
  }

  /**
   * Instantiates a new SwingEntityTableModel.
   * @param editModel the edit model
   * @param conditionModelFactory the table condition model factory
   */
  public SwingEntityTableModel(SwingEntityEditModel editModel, EntityConditionModelFactory conditionModelFactory) {
    this(editModel, new SwingEntityColumnFactory(requireNonNull(editModel).entityDefinition()), conditionModelFactory);
  }

  /**
   * Instantiates a new SwingEntityTableModel.
   * @param editModel the edit model
   * @param columnFactory the table column factory
   * @param conditionModelFactory the table condition model factory
   */
  public SwingEntityTableModel(SwingEntityEditModel editModel,
                               ColumnFactory<Attribute<?>> columnFactory,
                               EntityConditionModelFactory conditionModelFactory) {
    this.editModel = requireNonNull(editModel);
    this.tableModel = createTableModel(editModel.entityDefinition(), requireNonNull(columnFactory));
    this.conditionModel = entityTableConditionModel(editModel.entityType(), editModel.connectionProvider(), requireNonNull(conditionModelFactory));
    this.refreshCondition = conditionModel.condition();
    addEditListeners();
    bindEvents();
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
  public final State conditionRequiredState() {
    return conditionRequiredState;
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
      addEditListeners();
    }
    else {
      removeEditListeners();
    }
  }

  @Override
  public final EntityType entityType() {
    return editModel.entityType();
  }

  @Override
  public final EntityTableConditionModel<Attribute<?>> conditionModel() {
    return conditionModel;
  }

  @Override
  public final <C extends SwingEntityEditModel> C editModel() {
    return (C) editModel;
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
  public final boolean isMultipleEntityUpdateEnabled() {
    return multipleEntityUpdateEnabled;
  }

  @Override
  public final void setMultipleEntityUpdateEnabled(boolean multipleEntityUpdateEnabled) {
    this.multipleEntityUpdateEnabled = multipleEntityUpdateEnabled;
  }

  @Override
  public final boolean isDeleteEnabled() {
    return editModel != null && editModel.deleteEnabled().get();
  }

  @Override
  public final boolean isUpdateEnabled() {
    return editModel != null && editModel.updateEnabled().get();
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

    AttributeDefinition<?> attributeDefinition = entityDefinition().attributeDefinition(attribute);

    return attributeDefinition instanceof ColumnDefinition && ((ColumnDefinition<?>) attributeDefinition).isUpdatable();
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
  public final Entity entityByKey(Entity.Key primaryKey) {
    return visibleItems().stream()
            .filter(entity -> entity.primaryKey().equals(primaryKey))
            .findFirst()
            .orElse(null);
  }

  @Override
  public final int indexOf(Entity.Key primaryKey) {
    return indexOf(entityByKey(primaryKey));
  }

  @Override
  public final void replaceEntities(Collection<Entity> entities) {
    replaceEntitiesByKey(Entity.mapToPrimaryKey(entities));
  }

  @Override
  public final void refreshEntities(Collection<Entity.Key> keys) {
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
    entityDefinition().foreignKeyDefinition(foreignKey);

    return conditionModel.setEqualConditionValues(foreignKey, foreignKeyValues);
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
      fireTableRowsUpdated(0, getRowCount() - 1);
    }
  }

  @Override
  public final void selectEntitiesByKey(Collection<Entity.Key> keys) {
    selectionModel().setSelectedItems(new SelectByKeyPredicate(requireNonNull(keys, "keys")));
  }

  @Override
  public final Collection<Entity> entitiesByKey(Collection<Entity.Key> keys) {
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
  public final void update(Collection<? extends Entity> entities) throws ValidationException, DatabaseException {
    requireNonNull(entities, "entities");
    if (!isUpdateEnabled()) {
      throw new IllegalStateException("Updating is not enabled in this table model");
    }
    if (entities.size() > 1 && !multipleEntityUpdateEnabled) {
      throw new IllegalStateException("Batch update of entities is not enabled");
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
  public final void addSelectionListener(Runnable listener) {
    selectionModel().addSelectionListener(listener);
  }

  /**
   * @return an Observer for the table model status message, that is, the number of rows, number of selected rows etc
   */
  public final ValueObserver<String> statusMessageObserver() {
    return statusMessageValue.observer();
  }

  @Override
  public final void filterItems() {
    tableModel.filterItems();
  }

  @Override
  public Value<Predicate<Entity>> includeCondition() {
    return tableModel.includeCondition();
  }

  @Override
  public final Collection<Entity> items() {
    return tableModel.items();
  }

  @Override
  public final List<Entity> visibleItems() {
    return tableModel.visibleItems();
  }

  @Override
  public final Collection<Entity> filteredItems() {
    return tableModel.filteredItems();
  }

  @Override
  public final int visibleItemCount() {
    return tableModel.visibleItemCount();
  }

  @Override
  public final int filteredItemCount() {
    return tableModel.filteredItemCount();
  }

  @Override
  public final boolean containsItem(Entity item) {
    return tableModel.containsItem(item);
  }

  @Override
  public final boolean isVisible(Entity item) {
    return tableModel.isVisible(item);
  }

  @Override
  public final boolean isFiltered(Entity item) {
    return tableModel.isFiltered(item);
  }

  @Override
  public final Refresher<Entity> refresher() {
    return tableModel.refresher();
  }

  @Override
  public final void refresh() {
    tableModel.refresh();
  }

  @Override
  public final void refreshThen(Consumer<Collection<Entity>> afterRefresh) {
    tableModel.refreshThen(afterRefresh);
  }

  @Override
  public final void clear() {
    tableModel.clear();
  }

  @Override
  public final int getRowCount() {
    return tableModel.getRowCount();
  }

  @Override
  public final int indexOf(Entity item) {
    return tableModel.indexOf(item);
  }

  @Override
  public final Entity itemAt(int rowIndex) {
    return tableModel.itemAt(rowIndex);
  }

  @Override
  public final Object getValueAt(int rowIndex, int columnIndex) {
    return tableModel.getValueAt(rowIndex, columnIndex);
  }

  @Override
  public final String getStringValueAt(int rowIndex, Attribute<?> columnIdentifier) {
    return tableModel.getStringValueAt(rowIndex, columnIdentifier);
  }

  @Override
  public final void addItems(Collection<Entity> items) {
    tableModel.addItems(items);
  }

  @Override
  public final void addItemsSorted(Collection<Entity> items) {
    tableModel.addItemsSorted(items);
  }

  @Override
  public final void addItemsAt(int index, Collection<Entity> items) {
    tableModel.addItemsAt(index, items);
  }

  @Override
  public final void addItemsAtSorted(int index, Collection<Entity> items) {
    tableModel.addItemsAtSorted(index, items);
  }

  @Override
  public final void addItem(Entity item) {
    tableModel.addItem(item);
  }

  @Override
  public final void addItemAt(int index, Entity item) {
    tableModel.addItemAt(index, item);
  }

  @Override
  public final void addItemSorted(Entity item) {
    tableModel.addItemSorted(item);
  }

  @Override
  public final void setItemAt(int index, Entity item) {
    tableModel.setItemAt(index, item);
  }

  @Override
  public final void removeItems(Collection<Entity> items) {
    tableModel.removeItems(items);
  }

  @Override
  public final void removeItem(Entity item) {
    tableModel.removeItem(item);
  }

  @Override
  public final Entity removeItemAt(int index) {
    return tableModel.removeItemAt(index);
  }

  @Override
  public final List<Entity> removeItems(int fromIndex, int toIndex) {
    return tableModel.removeItems(fromIndex, toIndex);
  }

  @Override
  public final void fireTableDataChanged() {
    tableModel.fireTableDataChanged();
  }

  @Override
  public void fireTableRowsUpdated(int fromIndex, int toIndex) {
    tableModel.fireTableRowsUpdated(fromIndex, toIndex);
  }

  @Override
  public final FilteredTableColumnModel<Attribute<?>> columnModel() {
    return tableModel.columnModel();
  }

  @Override
  public final <T> Collection<T> values(Attribute<?> columnIdentifier) {
    return tableModel.values(columnIdentifier);
  }

  @Override
  public final Class<?> getColumnClass(Attribute<?> columnIdentifier) {
    return tableModel.getColumnClass(columnIdentifier);
  }

  @Override
  public final <T> Collection<T> selectedValues(Attribute<?> columnIdentifier) {
    return tableModel.selectedValues(columnIdentifier);
  }

  @Override
  public final String rowsAsDelimitedString(char delimiter) {
    return tableModel.rowsAsDelimitedString(delimiter);
  }

  @Override
  public final boolean isMergeOnRefresh() {
    return tableModel.isMergeOnRefresh();
  }

  @Override
  public final void setMergeOnRefresh(boolean mergeOnRefresh) {
    tableModel.setMergeOnRefresh(mergeOnRefresh);
  }

  @Override
  public final void sortItems() {
    tableModel.sortItems();
  }

  @Override
  public final FilteredTableSelectionModel<Entity> selectionModel() {
    return tableModel.selectionModel();
  }

  @Override
  public final FilteredTableSortModel<Entity, Attribute<?>> sortModel() {
    return tableModel.sortModel();
  }

  @Override
  public final FilteredTableSearchModel searchModel() {
    return tableModel.searchModel();
  }

  @Override
  public final TableConditionModel<Attribute<?>> filterModel() {
    return tableModel.filterModel();
  }

  @Override
  public final TableSummaryModel<Attribute<?>> summaryModel() {
    return tableModel.summaryModel();
  }

  @Override
  public final int getColumnCount() {
    return tableModel.getColumnCount();
  }

  @Override
  public final String getColumnName(int columnIndex) {
    return tableModel.getColumnName(columnIndex);
  }

  @Override
  public final Class<?> getColumnClass(int columnIndex) {
    return tableModel.getColumnClass(columnIndex);
  }

  @Override
  public final void addDataChangedListener(Runnable listener) {
    tableModel.addDataChangedListener(listener);
  }

  @Override
  public final void removeDataChangedListener(Runnable listener) {
    tableModel.removeDataChangedListener(listener);
  }

  @Override
  public final void addClearListener(Runnable listener) {
    tableModel.addClearListener(listener);
  }

  @Override
  public final void removeClearListener(Runnable listener) {
    tableModel.removeClearListener(listener);
  }

  @Override
  public final void addRowsRemovedListener(Consumer<RemovedRows> listener) {
    tableModel.addRowsRemovedListener(listener);
  }

  @Override
  public final void removeRowsRemovedListener(Consumer<RemovedRows> listener) {
    tableModel.removeRowsRemovedListener(listener);
  }

  @Override
  public final void addTableModelListener(TableModelListener listener) {
    tableModel.addTableModelListener(listener);
  }

  @Override
  public final void removeTableModelListener(TableModelListener listener) {
    tableModel.removeTableModelListener(listener);
  }

  /**
   * Queries the data used to populate this EntityTableModel when it is refreshed.
   * This method should take into account the condition ({EntityTableConditionModel#condition()}),
   * order by clause ({@link #orderBy()}), the limit ({@link #getLimit()}) and select attributes
   * ({@link #attributes()}) when querying.
   * @return entities selected from the database according the query condition.
   * @see #conditionRequiredState()
   * @see #isConditionEnabled()
   * @see EntityTableConditionModel#condition()
   */
  protected Collection<Entity> refreshItems() {
    if (conditionRequiredState.get() && !isConditionEnabled()) {
      updateRefreshCondition(conditionModel.condition());

      return emptyList();
    }
    try {
      return queryItems(conditionModel.condition());
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * It can be necessary to prevent the user from selecting too much data, when working with a large dataset.
   * This can be done by enabling the {@link #conditionRequiredState()}, which prevents a refresh as long as this
   * method returns {@code false}. This default implementation simply returns {@link EntityTableConditionModel#isEnabled()}.
   * Override for a more fine grained control, such as requiring a specific column condition to be enabled.
   * @return true if enough conditions are enabled for a safe refresh
   * @see #conditionRequiredState()
   */
  protected boolean isConditionEnabled() {
    return conditionModel.isEnabled();
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
    if (orderQueryBySortOrder && sortModel().isSorted()) {
      OrderBy orderBy = orderByFromSortModel();
      if (!orderBy.orderByColumns().isEmpty()) {
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
  protected Collection<Attribute<?>> attributes() {
    if (queryHiddenColumns || columnModel().hiddenColumns().isEmpty()) {
      return emptyList();
    }

    return entityDefinition().selectAttributes().stream()
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

  private void bindEvents() {
    columnModel().addColumnHiddenListener(this::onColumnHidden);
    conditionModel.addChangeListener(this::onConditionChanged);
    editModel.addAfterInsertListener(this::onInsert);
    editModel.addAfterUpdateListener(this::onUpdate);
    editModel.addAfterDeleteListener(this::onDelete);
    editModel.addRefreshListener(this::refresh);
    editModel.addEntityListener(this::onEntitySet);
    selectionModel().addSelectedItemListener(editModel::setEntity);
    addTableModelListener(this::onTableModelEvent);
    Runnable statusListener = () -> statusMessageValue.set(statusMessage());
    selectionModel().addSelectionListener(statusListener);
    addDataChangedListener(statusListener);
  }

  private void addEditListeners() {
    entityDefinition().foreignKeys().forEach(foreignKey ->
            EntityEditEvents.addUpdateListener(foreignKey.referencedType(), updateListener));
  }

  private void removeEditListeners() {
    entityDefinition().foreignKeys().forEach(foreignKey ->
            EntityEditEvents.removeUpdateListener(foreignKey.referencedType(), updateListener));
  }

  private List<Entity> queryItems(Condition condition) throws DatabaseException {
    List<Entity> items = editModel.connectionProvider().connection().select(where(condition)
              .attributes(attributes())
              .limit(getLimit())
              .orderBy(orderBy())
              .build());
    updateRefreshCondition(condition);

    return items;
  }

  private void updateRefreshCondition(Condition condition) {
    refreshCondition = condition;
    conditionChangedState.set(false);
  }

  private void onInsert(Collection<Entity> insertedEntities) {
    selectionModel().clearSelection();
    if (!insertAction.equals(InsertAction.DO_NOTHING)) {
      Collection<Entity> entitiesToAdd = insertedEntities.stream()
              .filter(entity -> entity.entityType().equals(entityType()))
              .collect(toList());
      switch (insertAction) {
        case ADD_TOP:
          tableModel.addItemsAt(0, entitiesToAdd);
          break;
        case ADD_BOTTOM:
          tableModel.addItemsAt(visibleItemCount(), entitiesToAdd);
          break;
        case ADD_TOP_SORTED:
          tableModel.addItemsAtSorted(0, entitiesToAdd);
          break;
      }
    }
  }

  private void onUpdate(Map<Entity.Key, Entity> updatedEntities) {
    replaceEntitiesByKey(new HashMap<>(updatedEntities));
  }

  private void onDelete(Collection<Entity> deletedEntities) {
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
  private void replaceEntitiesByKey(Map<Entity.Key, Entity> entitiesByKey) {
    for (Entity entity : items()) {
      Iterator<Map.Entry<Entity.Key, Entity>> iterator = entitiesByKey.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<Entity.Key, Entity> entry = iterator.next();
        if (entity.primaryKey().equals(entry.getKey())) {
          iterator.remove();
          entity.set(entry.getValue());
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

  private void onConditionChanged(Condition condition) {
    conditionChangedState.set(!Objects.equals(refreshCondition, condition));
  }

  private void onColumnHidden(Attribute<?> attribute) {
    //disable the condition and filter model for the column to be hidden, to prevent confusion
    ColumnConditionModel<?, ?> columnConditionModel = conditionModel.conditionModels().get(attribute);
    if (columnConditionModel != null && !columnConditionModel.locked().get()) {
      columnConditionModel.enabled().set(false);
    }
    ColumnConditionModel<?, ?> filterModel = filterModel().conditionModels().get(attribute);
    if (filterModel != null && !filterModel.locked().get()) {
      filterModel.enabled().set(false);
    }
  }

  private OrderBy orderByFromSortModel() {
    OrderBy.Builder builder = OrderBy.builder();
    sortModel().columnSortOrder().stream()
            .filter(columnSortOrder -> isColumn(columnSortOrder.columnIdentifier()))
            .forEach(columnSortOrder -> {
              switch (columnSortOrder.sortOrder()) {
                case ASCENDING:
                  builder.ascending((Column<?>) columnSortOrder.columnIdentifier());
                  break;
                case DESCENDING:
                  builder.descending((Column<?>) columnSortOrder.columnIdentifier());
                  break;
                default:
              }
            });

    return builder.build();
  }

  private boolean isColumn(Attribute<?> attribute) {
    return entityDefinition().attributeDefinition(attribute) instanceof ColumnDefinition;
  }

  private JSONObject createPreferences() throws Exception {
    JSONObject preferencesRoot = new JSONObject();
    preferencesRoot.put(ColumnPreferences.COLUMNS, createColumnPreferences());

    return preferencesRoot;
  }

  private JSONObject createColumnPreferences() {
    JSONObject columnPreferencesRoot = new JSONObject();
    for (FilteredTableColumn<Attribute<?>> column : columnModel().columns()) {
      Attribute<?> attribute = column.getIdentifier();
      int index = columnModel().isColumnVisible(attribute) ? columnModel().getColumnIndex(attribute) : -1;
      ColumnConditionModel<?, ?> columnConditionModel = conditionModel.conditionModels().get(attribute);
      ConditionPreferences conditionPreferences = columnConditionModel != null ?
              conditionPreferences(columnConditionModel.autoEnable().get(),
                      columnConditionModel.caseSensitive().get(),
                      columnConditionModel.automaticWildcard().get()) : null;
      ColumnPreferences columnPreferences = columnPreferences(attribute, index, column.getWidth(), conditionPreferences);
      columnPreferencesRoot.put(attribute.name(), columnPreferences.toJSONObject());
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
    List<Attribute<?>> columnAttributes = columnModel().columns().stream()
            .map(FilteredTableColumn::getIdentifier)
            .collect(toList());
    Map<Attribute<?>, ColumnPreferences> columnPreferences =
            ColumnPreferences.fromJSONObject(columnAttributes, new JSONObject(preferencesString).getJSONObject(ColumnPreferences.COLUMNS));
    ColumnPreferences.applyColumnPreferences(this, columnAttributes, columnPreferences, (attribute, columnWidth) ->
            columnModel().column(attribute).setPreferredWidth(columnWidth));
  }

  private String statusMessage() {
    int filteredItemCount = filteredItemCount();

    return STATUS_MESSAGE_NUMBER_FORMAT.format(getRowCount()) + " (" +
            STATUS_MESSAGE_NUMBER_FORMAT.format(selectionModel().selectionCount()) + " " +
            MESSAGES.getString("selected") + (filteredItemCount > 0 ? " - " +
            STATUS_MESSAGE_NUMBER_FORMAT.format(filteredItemCount) + " " + MESSAGES.getString("hidden") + ")" : ")");
  }

  private final class UpdateListener implements Consumer<Map<Entity.Key, Entity>> {

    @Override
    public void accept(Map<Entity.Key, Entity> updated) {
      updated.values().stream()
              .collect(groupingBy(Entity::entityType, HashMap::new, toList()))
              .forEach((entityType, entities) ->
                      entityDefinition().foreignKeys(entityType).forEach(foreignKey ->
                              replaceForeignKeyValues(foreignKey, entities)));
    }
  }

  private static final class SelectByKeyPredicate implements Predicate<Entity> {

    private final List<Entity.Key> keyList;

    private SelectByKeyPredicate(Collection<Entity.Key> keys) {
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

  private FilteredTableModel<Entity, Attribute<?>> createTableModel(EntityDefinition entityDefinition, ColumnFactory<Attribute<?>> columnFactory) {
    return FilteredTableModel.builder(columnFactory, new EntityColumnValueProvider())
            .filterModelFactory(new EntityFilterModelFactory(entityDefinition))
            .summaryValueProviderFactory(new EntitySummaryValueProviderFactory(entityDefinition, this))
            .itemSupplier(new EntityItemSupplier(this))
            .itemValidator(new EntityItemValidator(entityDefinition))
            .build();
  }

  private static final class EntityColumnValueProvider implements ColumnValueProvider<Entity, Attribute<?>> {

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
      Object value = entity.get(attribute);
      if (value instanceof Entity) {
        return (Comparable<T>) value.toString();
      }

      return (Comparable<T>) value;
    }
  }

  private static final class EntityFilterModelFactory implements ColumnConditionModel.Factory<Attribute<?>> {

    private final EntityDefinition entityDefinition;

    private EntityFilterModelFactory(EntityDefinition entityDefinition) {
      this.entityDefinition = requireNonNull(entityDefinition);
    }

    @Override
    public Optional<ColumnConditionModel<? extends Attribute<?>, ?>> createConditionModel(Attribute<?> attribute) {
      if (requireNonNull(attribute).isEntity()) {
        return Optional.empty();
      }
      if (!Comparable.class.isAssignableFrom(attribute.valueClass())) {
        return Optional.empty();
      }

      AttributeDefinition<?> attributeDefinition = entityDefinition.attributeDefinition(attribute);
      if (attributeDefinition.isHidden()) {
        return Optional.empty();
      }

      return Optional.ofNullable(ColumnConditionModel.builder(attribute, attribute.valueClass())
              .operators(operators(attribute.valueClass()))
              .format(attributeDefinition.format())
              .dateTimePattern(attributeDefinition.dateTimePattern())
              .build());
    }

    private static List<Operator> operators(Class<?> columnClass) {
      if (columnClass.equals(Boolean.class)) {
        return singletonList(Operator.EQUAL);
      }

      return Arrays.asList(Operator.values());
    }
  }

  private static final class EntitySummaryValueProviderFactory implements SummaryValueProvider.Factory<Attribute<?>> {

    private final EntityDefinition entityDefinition;
    private final FilteredTableModel<?, Attribute<?>> tableModel;

    private EntitySummaryValueProviderFactory(EntityDefinition entityDefinition, FilteredTableModel<?, Attribute<?>> tableModel) {
      this.entityDefinition = requireNonNull(entityDefinition);
      this.tableModel = requireNonNull(tableModel);
    }

    @Override
    public <T extends Number> Optional<SummaryValueProvider<T>> createSummaryValueProvider(Attribute<?> attribute, Format format) {
      AttributeDefinition<?> attributeDefinition = entityDefinition.attributeDefinition(attribute);
      if (attribute.isNumerical() && !(attributeDefinition instanceof ItemColumnDefinition)) {
        return Optional.of(summaryValueProvider(attribute, tableModel, format));
      }

      return Optional.empty();
    }
  }

  private static final class EntityItemSupplier implements Supplier<Collection<Entity>> {

    private final SwingEntityTableModel tableModel;

    private EntityItemSupplier(SwingEntityTableModel tableModel) {
      this.tableModel = requireNonNull(tableModel);
    }

    @Override
    public Collection<Entity> get() {
      return tableModel.refreshItems();
    }
  }

  private static final class EntityItemValidator implements Predicate<Entity> {

    private final EntityDefinition entityDefinition;

    private EntityItemValidator(EntityDefinition entityDefinition) {
      this.entityDefinition = requireNonNull(entityDefinition);
    }

    @Override
    public boolean test(Entity entity) {
      return entity.entityType().equals(entityDefinition.entityType());
    }
  }
}