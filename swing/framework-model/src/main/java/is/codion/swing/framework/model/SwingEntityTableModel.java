/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.Conjunction;
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
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.Color;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

  private static final String COLUMN_PREFERENCES = "-columns";
  private static final String CONDITIONS_PREFERENCES = "-conditions";

  private final FilteredTableModel<Entity, Attribute<?>> tableModel;
  private final SwingEntityEditModel editModel;
  private final EntityTableConditionModel<Attribute<?>> conditionModel;
  private final State conditionRequired = State.state();
  private final State respondToEditEvents = State.state();
  private final State editable = State.state();
  private final Value<Integer> limit = Value.value();
  private final State queryHiddenColumns = State.state(EntityTableModel.QUERY_HIDDEN_COLUMNS.get());
  private final State orderQueryBySortOrder = State.state(ORDER_QUERY_BY_SORT_ORDER.get());
  private final State removeDeleted = State.state(true);
  private final Value<OnInsert> onInsert = Value.value(EntityTableModel.ON_INSERT.get(), EntityTableModel.ON_INSERT.get());

  /**
   * Caches java.awt.Color instances parsed from hex strings via {@link #getColor(Object)}
   */
  private final Map<String, Color> colorCache = new ConcurrentHashMap<>();
  private final State conditionChanged = State.state();
  private final Consumer<Map<Entity.Key, Entity>> updateListener = new UpdateListener();

  private Select refreshCondition;

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
    this.refreshCondition = createSelect(conditionModel);
    bindEvents();
    applyPreferences();
    respondToEditEvents.set(true);
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
  public final Value<Integer> limit() {
    return limit;
  }

  @Override
  public final State queryHiddenColumns() {
    return queryHiddenColumns;
  }

  @Override
  public final State orderQueryBySortOrder() {
    return orderQueryBySortOrder;
  }

  @Override
  public final State conditionRequired() {
    return conditionRequired;
  }

  @Override
  public final Value<OnInsert> onInsert() {
    return onInsert;
  }

  @Override
  public final State removeDeleted() {
    return removeDeleted;
  }

  @Override
  public final State respondToEditEvents() {
    return respondToEditEvents;
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
  public final State editable() {
    return editable;
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
    if (!editable.get() || editModel.readOnly().get() || !editModel.updateEnabled().get()) {
      return false;
    }
    Attribute<?> attribute = columnModel().columnIdentifier(modelColumnIndex);
    if (attribute instanceof ForeignKey) {
      return entityDefinition().foreignKeys().updatable((ForeignKey) attribute);
    }

    AttributeDefinition<?> attributeDefinition = entityDefinition().attributes().definition(attribute);

    return attributeDefinition instanceof ColumnDefinition && ((ColumnDefinition<?>) attributeDefinition).updatable();
  }

  /**
   * Sets the value in the given cell and updates the underlying Entity.
   * @param value the new value
   * @param rowIndex the row whose value is to be changed
   * @param modelColumnIndex the model index of the column to be changed
   */
  @Override
  public final void setValueAt(Object value, int rowIndex, int modelColumnIndex) {
    if (!editable.get() || editModel.readOnly().get() || !editModel.updateEnabled().get()) {
      throw new IllegalStateException("This table model is readOnly or has disabled update");
    }
    Entity entity = itemAt(rowIndex).copy();
    Attribute<?> columnIdentifier = columnModel().columnIdentifier(modelColumnIndex);
    entity.put((Attribute<Object>) columnIdentifier, value);
    try {
      editModel.update(singletonList(entity));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Color backgroundColor(int row, Attribute<?> attribute) {
    requireNonNull(attribute);
    Object color = entityDefinition().backgroundColorProvider().color(itemAt(row), attribute);

    return color == null ? null : getColor(color);
  }

  @Override
  public Color foregroundColor(int row, Attribute<?> attribute) {
    requireNonNull(attribute);
    Object color = entityDefinition().foregroundColorProvider().color(itemAt(row), attribute);

    return color == null ? null : getColor(color);
  }

  @Override
  public final Optional<Entity> find(Entity.Key primaryKey) {
    requireNonNull(primaryKey);
    return visibleItems().stream()
            .filter(entity -> entity.primaryKey().equals(primaryKey))
            .findFirst();
  }

  @Override
  public final int indexOf(Entity.Key primaryKey) {
    return find(primaryKey)
            .map(this::indexOf)
            .orElse(-1);
  }

  @Override
  public final void replace(Collection<Entity> entities) {
    replaceEntitiesByKey(Entity.mapToPrimaryKey(entities));
  }

  @Override
  public final void refresh(Collection<Entity.Key> keys) {
    try {
      replace(connectionProvider().connection().select(keys));
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public final void replace(ForeignKey foreignKey, Collection<Entity> foreignKeyValues) {
    requireNonNull(foreignKey, "foreignKey");
    requireNonNull(foreignKeyValues, "foreignKeyValues");
    entityDefinition().foreignKeys().definition(foreignKey);
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
  public final void select(Collection<Entity.Key> keys) {
    selectionModel().setSelectedItems(new SelectByKeyPredicate(requireNonNull(keys, "keys")));
  }

  @Override
  public final Collection<Entity> find(Collection<Entity.Key> keys) {
    requireNonNull(keys, "keys");
    return items().stream()
            .filter(entity -> keys.contains(entity.primaryKey()))
            .collect(toList());
  }

  @Override
  public final void deleteSelected() throws DatabaseException {
    editModel.delete(selectionModel().getSelectedItems());
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
        UserPreferences.setUserPreference(userPreferencesKey() + COLUMN_PREFERENCES,
                ColumnPreferences.toString(createColumnPreferences()));
      }
      catch (Exception e) {
        LOG.error("Error while saving column preferences", e);
      }
      try {
        UserPreferences.setUserPreference(userPreferencesKey() + CONDITIONS_PREFERENCES,
                ConditionPreferences.toString(createConditionPreferences()));
      }
      catch (Exception e) {
        LOG.error("Error while saving condition preferences", e);
      }
    }
  }

  @Override
  public final StateObserver conditionChanged() {
    return conditionChanged.observer();
  }

  @Override
  public final void addSelectionListener(Runnable listener) {
    selectionModel().addSelectionListener(listener);
  }

  @Override
  public final void filterItems() {
    tableModel.filterItems();
  }

  @Override
  public final Value<Predicate<Entity>> includeCondition() {
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
  public final int visibleCount() {
    return tableModel.visibleCount();
  }

  @Override
  public final int filteredCount() {
    return tableModel.filteredCount();
  }

  @Override
  public final boolean contains(Entity item) {
    return tableModel.contains(item);
  }

  @Override
  public final boolean visible(Entity item) {
    return tableModel.visible(item);
  }

  @Override
  public final boolean filtered(Entity item) {
    return tableModel.filtered(item);
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
  public final String getStringAt(int rowIndex, Attribute<?> columnIdentifier) {
    return tableModel.getStringAt(rowIndex, columnIdentifier);
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
  public final State mergeOnRefresh() {
    return tableModel.mergeOnRefresh();
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
   * @param entities the entities to display
   * @param connectionProvider the connection provider
   * @return a static {@link SwingEntityTableModel} instance containing the given entities
   * @throws IllegalArgumentException in case {@code entities} is empty
   */
  public static SwingEntityTableModel tableModel(Collection<Entity> entities, EntityConnectionProvider connectionProvider) {
    if (requireNonNull(entities).isEmpty()) {
      throw new IllegalArgumentException("One or more entities is required for a static table model");
    }

    SwingEntityTableModel tableModel = new SwingEntityTableModel(entities.iterator().next().entityType(), connectionProvider) {
      @Override
      protected Collection<Entity> refreshItems() {
        return entities;
      }
    };
    tableModel.refresh();

    return tableModel;
  }

  /**
   * Queries the data used to populate this EntityTableModel when it is refreshed.
   * This method should take into account the where and having conditions
   * ({@link EntityTableConditionModel#where(Conjunction)}, {@link EntityTableConditionModel#having(Conjunction)}),
   * order by clause ({@link #orderBy()}), the limit ({@link #limit()}) and select attributes
   * ({@link #attributes()}) when querying.
   * @return entities selected from the database according to the query condition.
   * @see #conditionRequired()
   * @see #conditionEnabled(EntityTableConditionModel)
   * @see EntityTableConditionModel#where(Conjunction)
   * @see EntityTableConditionModel#having(Conjunction)
   */
  protected Collection<Entity> refreshItems() {
    try {
      return queryItems();
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * It can be necessary to prevent the user from selecting too much data, when working with a large dataset.
   * This can be done by enabling the {@link #conditionRequired()}, which prevents a refresh as long as this
   * method returns {@code false}. This default implementation simply returns {@link EntityTableConditionModel#enabled()}.
   * Override for a more fine grained control, such as requiring a specific column condition to be enabled.
   * @param conditionModel the table condition model
   * @return true if enough conditions are enabled for a safe refresh
   * @see #conditionRequired()
   */
  protected boolean conditionEnabled(EntityTableConditionModel<Attribute<?>> conditionModel) {
    return conditionModel.enabled();
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
   * @see #orderQueryBySortOrder()
   * @see EntityDefinition#orderBy()
   */
  protected OrderBy orderBy() {
    if (orderQueryBySortOrder.get() && sortModel().sorted()) {
      OrderBy orderBy = orderByFromSortModel();
      if (!orderBy.orderByColumns().isEmpty()) {
        return orderBy;
      }
    }

    return entityDefinition().orderBy();
  }

  /**
   * Specifies the attributes to select when querying data. Return an empty list if all should be included.
   * This method should take the {@link #queryHiddenColumns()} setting into account.
   * @return the attributes to select when querying data, an empty Collection if all should be selected.
   * @see #queryHiddenColumns()
   */
  protected Collection<Attribute<?>> attributes() {
    if (queryHiddenColumns.get() || columnModel().hidden().isEmpty()) {
      return emptyList();
    }

    return entityDefinition().attributes().selected().stream()
            .filter(attribute -> columnModel().visible(attribute).get())
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
    String userPreferencesKey = userPreferencesKey();
    UserPreferences.removeUserPreference(userPreferencesKey + COLUMN_PREFERENCES);
    UserPreferences.removeUserPreference(userPreferencesKey + CONDITIONS_PREFERENCES);
  }

  private void bindEvents() {
    columnModel().addColumnHiddenListener(this::onColumnHidden);
    respondToEditEvents.addDataListener(new EditEventListener());
    conditionModel.addChangeListener(() -> onConditionChanged(createSelect(conditionModel)));
    editModel.addAfterInsertListener(this::onInsert);
    editModel.addAfterUpdateListener(this::onUpdate);
    editModel.addAfterDeleteListener(this::onDelete);
    editModel.addRefreshListener(this::refresh);
    editModel.addEntityListener(this::onEntitySet);
    selectionModel().addSelectedItemListener(editModel::set);
    addTableModelListener(this::onTableModelEvent);
  }

  private List<Entity> queryItems() throws DatabaseException {
    Select select = createSelect(conditionModel);
    if (conditionRequired.get() && !conditionEnabled(conditionModel)) {
      updateRefreshSelect(select);

      return emptyList();
    }
    List<Entity> items = editModel.connectionProvider().connection().select(where(select.where())
            .having(select.having())
            .attributes(attributes())
            .limit(limit().optional().orElse(-1))
            .orderBy(orderBy())
            .build());
    updateRefreshSelect(select);

    return items;
  }

  private void updateRefreshSelect(Select select) {
    refreshCondition = select;
    conditionChanged.set(false);
  }

  private void onInsert(Collection<Entity> insertedEntities) {
    Collection<Entity> entitiesToAdd = insertedEntities.stream()
            .filter(entity -> entity.entityType().equals(entityType()))
            .collect(toList());
    if (!onInsert.equalTo(OnInsert.DO_NOTHING) && !entitiesToAdd.isEmpty()) {
      if (!selectionModel().isSelectionEmpty()) {
        selectionModel().clearSelection();
      }
      switch (onInsert.get()) {
        case ADD_TOP:
          tableModel.addItemsAt(0, entitiesToAdd);
          break;
        case ADD_TOP_SORTED:
          tableModel.addItemsAtSorted(0, entitiesToAdd);
          break;
        case ADD_BOTTOM:
          tableModel.addItemsAt(visibleCount(), entitiesToAdd);
          break;
        case ADD_BOTTOM_SORTED:
          tableModel.addItemsAtSorted(visibleCount(), entitiesToAdd);
          break;
        default:
          break;
      }
    }
  }

  private void onUpdate(Map<Entity.Key, Entity> updatedEntities) {
    replaceEntitiesByKey(new HashMap<>(updatedEntities));
  }

  private void onDelete(Collection<Entity> deletedEntities) {
    if (removeDeleted.get()) {
      removeItems(deletedEntities);
    }
  }

  private void onEntitySet(Entity entity) {
    if (entity == null && !selectionModel().isSelectionEmpty()) {
      selectionModel().clearSelection();
    }
  }

  private void onTableModelEvent(TableModelEvent tableModelEvent) {
    //if the selected row is updated via the table model, refresh the one in the edit model
    if (tableModelEvent.getType() == TableModelEvent.UPDATE && tableModelEvent.getFirstRow() == selectionModel().getSelectedIndex()) {
      editModel.set(selectionModel().getSelectedItem());
    }
  }

  private void onConditionChanged(Select condition) {
    conditionChanged.set(!Objects.equals(refreshCondition, condition));
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
    return entityDefinition().attributes().definition(attribute) instanceof ColumnDefinition;
  }

  private Map<Attribute<?>, ColumnPreferences> createColumnPreferences() {
    Map<Attribute<?>, ColumnPreferences> columnPreferencesMap = new HashMap<>();
    for (FilteredTableColumn<Attribute<?>> column : columnModel().columns()) {
      Attribute<?> attribute = column.getIdentifier();
      int index = columnModel().visible(attribute).get() ? columnModel().getColumnIndex(attribute) : -1;
      columnPreferencesMap.put(attribute, columnPreferences(attribute, index, column.getWidth()));
    }

    return columnPreferencesMap;
  }

  private Map<Attribute<?>, ConditionPreferences> createConditionPreferences() {
    Map<Attribute<?>, ConditionPreferences> conditionPreferencesMap = new HashMap<>();
    for (FilteredTableColumn<Attribute<?>> column : columnModel().columns()) {
      Attribute<?> attribute = column.getIdentifier();
      ColumnConditionModel<?, ?> columnConditionModel = conditionModel.conditionModels().get(attribute);
      if (columnConditionModel != null) {
        conditionPreferencesMap.put(attribute, conditionPreferences(attribute,
                columnConditionModel.autoEnable().get(),
                columnConditionModel.caseSensitive().get(),
                columnConditionModel.automaticWildcard().get()));
      }
    }

    return conditionPreferencesMap;
  }

  private void applyPreferences() {
    if (EntityModel.USE_CLIENT_PREFERENCES.get()) {
      String columnPreferencesString = UserPreferences.getUserPreference(userPreferencesKey() + COLUMN_PREFERENCES, "");
      if (columnPreferencesString.isEmpty()) {//todo remove: see if a legacy one without "-columns" postfix exists
        columnPreferencesString = UserPreferences.getUserPreference(userPreferencesKey(), "");
      }
      if (!columnPreferencesString.isEmpty()) {
        applyColumnPreferences(columnPreferencesString);
      }
      String conditionPreferencesString = UserPreferences.getUserPreference(userPreferencesKey() + CONDITIONS_PREFERENCES, "");
      if (!conditionPreferencesString.isEmpty()) {
        applyConditionPreferences(conditionPreferencesString);
      }
    }
  }

  private void applyColumnPreferences(String preferencesString) {
    List<Attribute<?>> columnAttributes = columnModel().columns().stream()
            .map(FilteredTableColumn::getIdentifier)
            .collect(toList());
    try {
      ColumnPreferences.apply(this, columnAttributes, preferencesString, (attribute, columnWidth) ->
              columnModel().column(attribute).setPreferredWidth(columnWidth));
    }
    catch (Exception e) {
      LOG.error("Error while applying column preferences: " + preferencesString, e);
    }
  }

  private void applyConditionPreferences(String preferencesString) {
    List<Attribute<?>> columnAttributes = columnModel().columns().stream()
            .map(FilteredTableColumn::getIdentifier)
            .collect(toList());
    try {
      ConditionPreferences.apply(this, columnAttributes, preferencesString);
    }
    catch (Exception e) {
      LOG.error("Error while applying condition preferences: " + preferencesString, e);
    }
  }

  private static Select createSelect(EntityTableConditionModel<Attribute<?>> conditionModel) {
    return Select.where(conditionModel.where(Conjunction.AND))
            .having(conditionModel.having(Conjunction.AND))
            .build();
  }

  private final class UpdateListener implements Consumer<Map<Entity.Key, Entity>> {

    @Override
    public void accept(Map<Entity.Key, Entity> updated) {
      updated.values().stream()
              .collect(groupingBy(Entity::entityType, HashMap::new, toList()))
              .forEach((entityType, entities) ->
                      entityDefinition().foreignKeys().get(entityType).forEach(foreignKey ->
                              replace(foreignKey, entities)));
    }
  }

  private final class EditEventListener implements Consumer<Boolean> {

    @Override
    public void accept(Boolean listen) {
      if (listen) {
        addEditListeners();
      }
      else {
        removeEditListeners();
      }
    }

    private void addEditListeners() {
      entityDefinition().foreignKeys().get().forEach(foreignKey ->
              EntityEditEvents.addUpdateListener(foreignKey.referencedType(), updateListener));
    }

    private void removeEditListeners() {
      entityDefinition().foreignKeys().get().forEach(foreignKey ->
              EntityEditEvents.removeUpdateListener(foreignKey.referencedType(), updateListener));
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
            .itemValidator(new EntityItemValidator(entityDefinition.entityType()))
            .build();
  }

  private static final class EntityColumnValueProvider implements ColumnValueProvider<Entity, Attribute<?>> {

    @Override
    public Object value(Entity entity, Attribute<?> attribute) {
      return entity.get(attribute);
    }

    @Override
    public String string(Entity entity, Attribute<?> attribute) {
      return entity.string(attribute);
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
      if (requireNonNull(attribute).type().isEntity()) {
        return Optional.empty();
      }
      if (!Comparable.class.isAssignableFrom(attribute.type().valueClass())) {
        return Optional.empty();
      }

      AttributeDefinition<?> attributeDefinition = entityDefinition.attributes().definition(attribute);
      if (attributeDefinition.hidden()) {
        return Optional.empty();
      }

      return Optional.ofNullable(ColumnConditionModel.builder(attribute, attribute.type().valueClass())
              .operators(operators(attribute.type().valueClass()))
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
      AttributeDefinition<?> attributeDefinition = entityDefinition.attributes().definition(attribute);
      if (attribute.type().isNumerical() && attributeDefinition.items().isEmpty()) {
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

    private final EntityType entityType;

    private EntityItemValidator(EntityType entityType) {
      this.entityType = requireNonNull(entityType);
    }

    @Override
    public boolean test(Entity entity) {
      return entity.entityType().equals(entityType);
    }
  }
}