/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventListener;
import is.codion.common.model.UserPreferences;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnSummaryModel;
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
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.DefaultEntityTableConditionModel;
import is.codion.framework.model.DefaultFilterModelFactory;
import is.codion.framework.model.EntityModel;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.common.model.table.SwingFilteredTableColumnModel;
import is.codion.swing.common.model.table.TableSortModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SortOrder;
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
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A TableModel implementation for displaying and working with entities.
 */
public class SwingEntityTableModel extends AbstractFilteredTableModel<Entity, Attribute<?>>
        implements EntityTableModel<SwingEntityEditModel> {

  private static final Logger LOG = LoggerFactory.getLogger(SwingEntityTableModel.class);

  /**
   * The edit model to use when updating/deleting entities
   */
  private final SwingEntityEditModel editModel;

  /**
   * The condition model
   */
  private final EntityTableConditionModel tableConditionModel;

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
  private int limit = -1;

  /**
   * Specifies whether the values of hidden columns are included in the underlying query
   */
  private boolean queryHiddenColumns = EntityTableModel.QUERY_HIDDEN_COLUMNS.get();

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
   * Specifies whether to use the current sort order as the query order by clause
   */
  private boolean orderQueryBySortOrder = ORDER_QUERY_BY_SORT_ORDER.get();

  /**
   * Instantiates a new SwingEntityTableModel with default edit, sort and condition models.
   * @param entityType the entityType
   * @param connectionProvider the db provider
   */
  public SwingEntityTableModel(final EntityType entityType, final EntityConnectionProvider connectionProvider) {
    this(new SwingEntityEditModel(entityType, connectionProvider));
  }

  /**
   * Instantiates a new DefaultEntityTableModel.
   * @param editModel the edit model
   * @throws NullPointerException in case editModel is null
   */
  public SwingEntityTableModel(final SwingEntityEditModel editModel) {
    this(requireNonNull(editModel, "editModel"), new SwingEntityTableSortModel(editModel.getEntities()));
  }

  /**
   * Instantiates a new DefaultEntityTableModel.
   * @param entityType the entityType
   * @param connectionProvider the db provider
   * @param sortModel the sort model
   * @throws NullPointerException in case editModel or sortModel is null
   */
  public SwingEntityTableModel(final EntityType entityType, final EntityConnectionProvider connectionProvider,
                               final TableSortModel<Entity, Attribute<?>> sortModel) {
    this(new SwingEntityEditModel(entityType, connectionProvider), sortModel);
  }

  /**
   * Instantiates a new DefaultEntityTableModel.
   * @param editModel the edit model
   * @param sortModel the sort model
   * @throws NullPointerException in case editModel or sortModel is null
   */
  public SwingEntityTableModel(final SwingEntityEditModel editModel, final TableSortModel<Entity, Attribute<?>> sortModel) {
    this(editModel, sortModel, new DefaultEntityTableConditionModel(editModel.getEntityType(), editModel.getConnectionProvider(),
            new DefaultFilterModelFactory(), new SwingConditionModelFactory(editModel.getConnectionProvider())));
  }

  /**
   * Instantiates a new DefaultEntityTableModel.
   * @param editModel the edit model
   * @param sortModel the sort model
   * @param tableConditionModel the table condition model
   * @throws NullPointerException in case editModel, sortModel or tableConditionModel is null
   */
  public SwingEntityTableModel(final SwingEntityEditModel editModel, final TableSortModel<Entity, Attribute<?>> sortModel,
                               final EntityTableConditionModel tableConditionModel) {
    super(new SwingFilteredTableColumnModel<>(createColumns(requireNonNull(editModel, "editModel")
                    .getConnectionProvider().getEntities().getDefinition(editModel.getEntityType()))),
            sortModel, requireNonNull(tableConditionModel, "tableConditionModel").getFilterModels().values());
    if (!tableConditionModel.getEntityType().equals(editModel.getEntityType())) {
      throw new IllegalArgumentException("Entity type mismatch, conditionModel: " + tableConditionModel.getEntityType()
              + ", tableModel: " + editModel.getEntityType());
    }
    this.tableConditionModel = tableConditionModel;
    this.editModel = editModel;
    bindEventsInternal();
    applyPreferences();
  }

  @Override
  public final Entities getEntities() {
    return editModel.getConnectionProvider().getEntities();
  }

  @Override
  public final EntityDefinition getEntityDefinition() {
    return editModel.getEntityDefinition();
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + editModel.getEntityType();
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
  public final int getQueryRowCountLimit() {
    return queryRowCountLimit;
  }

  @Override
  public final void setQueryRowCountLimit(final int queryRowCountLimit) {
    this.queryRowCountLimit = queryRowCountLimit;
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
  public final EntityType getEntityType() {
    return editModel.getEntityType();
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
    return editModel.getConnectionProvider();
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

  /**
   * Returns true if the cell at <code>rowIndex</code> and <code>modelColumnIndex</code> is editable.
   * @param rowIndex the row to edit
   * @param modelColumnIndex the model index of the column to edit
   * @return true if the cell is editable
   * @see #setValueAt(Object, int, int)
   */
  @Override
  public boolean isCellEditable(final int rowIndex, final int modelColumnIndex) {
    if (!editable || isReadOnly() || !isUpdateEnabled()) {
      return false;
    }
    final Attribute<?> attribute = getColumnModel().getColumnIdentifier(modelColumnIndex);
    if (attribute instanceof ForeignKey) {
      return getEntityDefinition().isUpdatable((ForeignKey) attribute);
    }

    final Property<?> property = getEntityDefinition().getProperty(attribute);

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
    final Attribute<?> attribute = getColumnModel().getColumnIdentifier(modelColumnIndex);
    final Entity entity = getItemAt(rowIndex);

    return getValue(entity, attribute);
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
    final Entity entity = getItemAt(rowIndex).copy();

    final Attribute<?> columnIdentifier = getColumnModel().getColumnIdentifier(modelColumnIndex);

    entity.put((Attribute<Object>) columnIdentifier, value);
    try {
      update(singletonList(entity));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Color getBackgroundColor(final int row, final Attribute<?> attribute) {
    return (Color) getEntityDefinition().getBackgroundColorProvider().getColor(getItemAt(row), attribute);
  }

  @Override
  public Color getForegroundColor(final int row, final Attribute<?> attribute) {
    return (Color) getEntityDefinition().getForegroundColorProvider().getColor(getItemAt(row), attribute);
  }

  @Override
  public final Entity getEntityByKey(final Key primaryKey) {
    return getVisibleItems().stream()
            .filter(entity -> entity.getPrimaryKey().equals(primaryKey))
            .findFirst()
            .orElse(null);
  }

  @Override
  public final int indexOf(final Key primaryKey) {
    return indexOf(getEntityByKey(primaryKey));
  }

  @Override
  public final void addEntities(final Collection<Entity> entities) {
    addEntitiesAt(getVisibleItemCount(), entities);
  }

  @Override
  public final void addEntitiesSorted(final Collection<Entity> entities) {
    addEntitiesAtSorted(getVisibleItemCount(), entities);
  }

  @Override
  public final void addEntitiesAt(final int index, final Collection<Entity> entities) {
    addItemsAt(index, entities);
  }

  @Override
  public final void addEntitiesAtSorted(final int index, final Collection<Entity> entities) {
    addItemsAtSorted(index, entities);
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
  public void setForeignKeyConditionValues(final ForeignKey foreignKey, final Collection<Entity> foreignKeyValues) {
    requireNonNull(foreignKey, "foreignKey");
    getEntityDefinition().getForeignKeyProperty(foreignKey);
    if (tableConditionModel.setEqualConditionValues(foreignKey, foreignKeyValues) && refreshOnForeignKeyConditionValuesSet) {
      refresh();
    }
  }

  @Override
  public final void replaceForeignKeyValues(final EntityType foreignKeyEntityType, final Collection<Entity> foreignKeyValues) {
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
    return getItems().stream()
            .filter(entity -> keys.contains(entity.getPrimaryKey()))
            .collect(toList());
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
  public final Iterator<Entity> getSelectedEntitiesIterator() {
    return getSelectionModel().getSelectedItems().iterator();
  }

  @Override
  public final void setColumns(final Attribute<?>... attributes) {
    getColumnModel().setColumns(attributes);
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
      final Attribute<?> attribute = (Attribute<?>) columnEnumeration.nextElement().getIdentifier();
      attributes.add(attribute);
      header.add(getEntityDefinition().getProperty(attribute).getCaption());
    }

    return Text.getDelimitedString(header, Entity.getStringValueList(attributes,
                    getSelectionModel().isSelectionEmpty() ? getVisibleItems() : getSelectionModel().getSelectedItems()),
            String.valueOf(delimiter));
  }

  @Override
  public final void addSelectionChangedListener(final EventListener listener) {
    getSelectionModel().addSelectionChangedListener(listener);
  }

  /**
   * Initializes default {@link TableColumn}s for all visible properties in the given entity type.
   * @param definition the entity definition
   * @return a list of TableColumns based on the given entity
   */
  public static List<TableColumn> createColumns(final EntityDefinition definition) {
    return createColumns(requireNonNull(definition).getVisibleProperties());
  }

  /**
   * Initializes default {@link TableColumn}s from the given properties.
   * @param properties the properties
   * @return a list of TableColumns based on the given properties
   */
  public static List<TableColumn> createColumns(final List<Property<?>> properties) {
    requireNonNull(properties);
    final List<TableColumn> columns = new ArrayList<>(properties.size());
    for (final Property<?> property : properties) {
      final TableColumn column = new TableColumn(columns.size());
      column.setIdentifier(property.getAttribute());
      column.setHeaderValue(property.getCaption());
      if (property.getPreferredColumnWidth() > 0) {
        column.setPreferredWidth(property.getPreferredColumnWidth());
      }
      columns.add(column);
    }

    return columns;
  }

  @Override
  protected final <T extends Number> Optional<ColumnSummaryModel.ColumnValueProvider<T>> createColumnValueProvider(final Attribute<?> attribute) {
    if (attribute.isNumerical()) {
      return Optional.of(new DefaultColumnValueProvider<>(attribute, this, getEntityDefinition().getProperty(attribute).getFormat()));
    }

    return Optional.empty();
  }

  /**
   * Queries for the data used to populate this EntityTableModel when it is refreshed,
   * using the order by clause returned by {@link #getOrderBy()}
   * @return entities selected from the database according the query condition.
   * @see #getQueryConditionRequiredState()
   * @see EntityTableConditionModel#getCondition()
   */
  @Override
  protected Collection<Entity> refreshItems() {
    if (queryConditionRequiredState.get() && !getTableConditionModel().isConditionEnabled()) {
      return emptyList();
    }
    checkQueryRowCount();
    try {
      return editModel.getConnectionProvider().getConnection().select(getTableConditionModel().getCondition()
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
   * @return the number of rows {@link #refreshItems()} would return on next invocation
   */
  protected int getQueryRowCount() {
    if (queryConditionRequiredState.get() && !getTableConditionModel().isConditionEnabled()) {
      return 0;
    }

    try {
      return editModel.getConnectionProvider().getConnection().rowCount(getTableConditionModel().getCondition());
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return the value to display in a table cell for the given attribute of the given entity.
   * @param entity the entity
   * @param attribute the attribute
   * @return the value of the given attribute for the given entity for display
   * @throws NullPointerException in case entity or attribute is null
   */
  protected Object getValue(final Entity entity, final Attribute<?> attribute) {
    requireNonNull(entity, "entity");
    requireNonNull(attribute, "attribute");
    final Property<?> property = getEntityDefinition().getProperty(attribute);
    if (property instanceof ItemProperty) {
      return entity.toString(property);
    }

    return entity.get(attribute);
  }

  @Override
  protected final String getSearchValueAt(final int rowIndex, final Attribute<?> columnIdentifier) {
    return getItemAt(rowIndex).toString(columnIdentifier);
  }

  /**
   * The order by clause to use when selecting the data for this model,
   * by default the order by clause defined for the underlying entity
   * @return the order by clause
   * @see EntityDefinition#getOrderBy()
   */
  protected OrderBy getOrderBy() {
    if (orderQueryBySortOrder && getSortModel().isSortingEnabled()) {
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
   * @return the attributes to select when querying data, an empty Collection if all should be selected.
   * @see #isQueryHiddenColumns()
   */
  protected Collection<Attribute<?>> getSelectAttributes() {
    if (queryHiddenColumns || getColumnModel().getHiddenColumns().isEmpty()) {
      return emptyList();
    }

    return getEntityDefinition().getDefaultSelectAttributes().stream()
            .filter(getColumnModel()::isColumnVisible)
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

  /**
   * Clears any user preferences saved for this table model
   */
  final void clearPreferences() {
    UserPreferences.removeUserPreference(getUserPreferencesKey());
  }

  private void bindEventsInternal() {
    getColumnModel().addColumnHiddenListener(this::onColumnHidden);
    addRefreshSuccessfulListener(tableConditionModel::rememberCondition);
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
      final List<Entity> entitiesToAdd = insertedEntities.stream()
              .filter(entity -> entity.getEntityType().equals(getEntityType()))
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

  private void onColumnHidden(final Attribute<?> attribute) {
    //disable the condition and filter model for the column to be hidden, to prevent confusion
    final ColumnConditionModel<?, ?> conditionModel = tableConditionModel.getConditionModels().get(attribute);
    if (conditionModel != null && !conditionModel.isLocked()) {
      conditionModel.setEnabled(false);
    }
    final ColumnConditionModel<?, ?> filterModel = tableConditionModel.getFilterModels().get(attribute);
    if (filterModel != null && !filterModel.isLocked()) {
      filterModel.setEnabled(false);
    }
  }

  private OrderBy getOrderByFromSortModel() {
    final OrderBy orderBy = OrderBy.orderBy();
    getSortModel().getColumnSortOrder().entrySet().stream()
            .filter(entry -> isColumnProperty(entry.getKey()))
            .forEach(entry -> {
              if (entry.getValue() == SortOrder.ASCENDING) {
                orderBy.ascending(entry.getKey());
              }
              else {
                orderBy.descending(entry.getKey());
              }
            });

    return orderBy;
  }

  private boolean isColumnProperty(final Attribute<?> attribute) {
    final Property<?> property = getEntityDefinition().getProperty(attribute);

    return property instanceof ColumnProperty;
  }

  private org.json.JSONObject createPreferences() throws Exception {
    final org.json.JSONObject preferencesRoot = new org.json.JSONObject();
    preferencesRoot.put(PREFERENCES_COLUMNS, createColumnPreferences());

    return preferencesRoot;
  }

  private org.json.JSONObject createColumnPreferences() throws Exception {
    final org.json.JSONObject columnPreferencesRoot = new org.json.JSONObject();
    for (final TableColumn column : getColumnModel().getAllColumns()) {
      final Attribute<?> attribute = (Attribute<?>) column.getIdentifier();
      final org.json.JSONObject columnObject = new org.json.JSONObject();
      final boolean visible = getColumnModel().isColumnVisible(attribute);
      columnObject.put(PREFERENCES_COLUMN_WIDTH, column.getWidth());
      columnObject.put(PREFERENCES_COLUMN_VISIBLE, visible);
      columnObject.put(PREFERENCES_COLUMN_INDEX, visible ? getColumnModel().getColumnIndex(attribute) : -1);
      columnPreferencesRoot.put(attribute.getName(), columnObject);
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
    final SwingFilteredTableColumnModel<Attribute<?>> columnModel = getColumnModel();
    for (final TableColumn column : Collections.list(columnModel.getColumns())) {
      final Attribute<?> attribute = (Attribute<?>) column.getIdentifier();
      if (columnModel.containsColumn(attribute)) {
        try {
          final org.json.JSONObject columnPreferences = preferences.getJSONObject(attribute.getName());
          column.setPreferredWidth(columnPreferences.getInt(PREFERENCES_COLUMN_WIDTH));
          if (columnPreferences.getBoolean(PREFERENCES_COLUMN_VISIBLE)) {
            final int index = Math.min(columnModel.getColumnCount() - 1, columnPreferences.getInt(PREFERENCES_COLUMN_INDEX));
            columnModel.moveColumn(getColumnModel().getColumnIndex(column.getIdentifier()), index);
          }
          else {
            columnModel.hideColumn((Attribute<?>) column.getIdentifier());
          }
        }
        catch (final Exception e) {
          LOG.info("Property preferences not found: " + attribute, e);
        }
      }
    }
  }
}