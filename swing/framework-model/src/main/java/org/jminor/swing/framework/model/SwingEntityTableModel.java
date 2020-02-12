/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.Text;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.event.EventListener;
import org.jminor.common.model.PreferencesUtil;
import org.jminor.common.model.table.ColumnSummaryModel;
import org.jminor.common.model.table.SortingDirective;
import org.jminor.common.model.table.TableSortModel;
import org.jminor.common.state.State;
import org.jminor.common.state.States;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.OrderBy;
import org.jminor.framework.domain.exception.ValidationException;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.ValueListProperty;
import org.jminor.framework.model.DefaultEntityTableConditionModel;
import org.jminor.framework.model.DefaultPropertyFilterModelProvider;
import org.jminor.framework.model.EntityModel;
import org.jminor.framework.model.EntityTableConditionModel;
import org.jminor.framework.model.EntityTableModel;
import org.jminor.swing.common.model.table.AbstractFilteredTableModel;
import org.jminor.swing.common.model.table.AbstractTableSortModel;
import org.jminor.swing.common.model.table.SwingFilteredTableColumnModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import static org.jminor.framework.db.condition.Conditions.entitySelectCondition;

/**
 * A TableModel implementation for displaying and working with entities.
 *
 * <pre>
 * String entityId = "some.entity";
 * String clientTypeId = "JavadocDemo";
 * User user = new User("scott", "tiger");
 *
 * EntityConnectionProvider connectionProvider = EntityConnectionProviders.createConnectionProvider(user, clientTypeId);
 *
 * SwingEntityTableModel tableModel = new SwingEntityTableModel(entityId, connectionProvider);
 *
 * SwingEntityEditModel editModel = ...;
 *
 * tableModel.setEditModel(editModel);
 *
 * EntityTablePanel panel = new EntityTablePanel(tableModel);
 * </pre>
 */
public class SwingEntityTableModel extends AbstractFilteredTableModel<Entity, Property>
        implements EntityTableModel<SwingEntityEditModel> {

  private static final Logger LOG = LoggerFactory.getLogger(SwingEntityTableModel.class);

  /**
   * The entity ID
   */
  private final String entityId;

  /**
   * The EntityConnection provider
   */
  private final EntityConnectionProvider connectionProvider;

  /**
   * The edit model to use when updating/deleting entities
   */
  private SwingEntityEditModel editModel;

  /**
   * The condition model
   */
  private final EntityTableConditionModel conditionModel;

  /**
   * If true then querying should be disabled if no condition is specified
   */
  private final State queryConditionRequiredState = States.state();

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
   * Instantiates a new DefaultEntityTableModel with default column and condition models.
   * @param entityId the entity ID
   * @param connectionProvider the db provider
   */
  public SwingEntityTableModel(final String entityId, final EntityConnectionProvider connectionProvider) {
    this(entityId, connectionProvider, new DefaultEntityTableSortModel(connectionProvider.getDomain(), entityId),
            new DefaultEntityTableConditionModel(entityId, connectionProvider,
                    new DefaultPropertyFilterModelProvider(), new SwingPropertyConditionModelProvider()));
  }

  /**
   * Instantiates a new DefaultEntityTableModel.
   * @param entityId the entity ID
   * @param connectionProvider the db provider
   * @param conditionModel the condition model
   * @param sortModel the sort model
   * @throws NullPointerException in case conditionModel is null
   * @throws IllegalArgumentException if {@code conditionModel} entityId does not match the one supplied as parameter
   */
  public SwingEntityTableModel(final String entityId, final EntityConnectionProvider connectionProvider,
                               final TableSortModel<Entity, Property, TableColumn> sortModel,
                               final EntityTableConditionModel conditionModel) {
    super(sortModel, requireNonNull(conditionModel, "conditionModel").getPropertyFilterModels());
    if (!conditionModel.getEntityId().equals(entityId)) {
      throw new IllegalArgumentException("Entity ID mismatch, conditionModel: " + conditionModel.getEntityId()
              + ", tableModel: " + entityId);
    }
    this.entityId = entityId;
    this.connectionProvider = connectionProvider;
    this.conditionModel = conditionModel;
    bindEvents();
    applyPreferences();
  }

  /** {@inheritDoc} */
  @Override
  public final Domain getDomain() {
    return connectionProvider.getDomain();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityDefinition getEntityDefinition() {
    return getDomain().getDefinition(entityId);
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + entityId;
  }

  /** {@inheritDoc} */
  @Override
  public final void setEditModel(final SwingEntityEditModel editModel) {
    requireNonNull(editModel, "editModel");
    if (this.editModel != null) {
      throw new IllegalStateException("Edit model has already been set for table model: " + this);
    }
    if (!editModel.getEntityId().equals(entityId)) {
      throw new IllegalArgumentException("Entity ID mismatch, editModel: " + editModel.getEntityId() + ", tableModel: " + entityId);
    }
    this.editModel = editModel;
    bindEditModelEvents();
    onSetEditModel(editModel);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean hasEditModel() {
    return this.editModel != null;
  }

  /** {@inheritDoc} */
  @Override
  public final int getFetchCount() {
    return fetchCount;
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityTableModel setFetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final State getQueryConditionRequiredState() {
    return queryConditionRequiredState;
  }

  /** {@inheritDoc} */
  @Override
  public final InsertAction getInsertAction() {
    return insertAction;
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityTableModel setInsertAction(final InsertAction insertAction) {
    requireNonNull(insertAction, "insertAction");
    this.insertAction = insertAction;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isRemoveEntitiesOnDelete() {
    return removeEntitiesOnDelete;
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityTableModel setRemoveEntitiesOnDelete(final boolean value) {
    this.removeEntitiesOnDelete = value;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityId() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableConditionModel getConditionModel() {
    return conditionModel;
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityEditModel getEditModel() {
    if (editModel == null) {
      throw new IllegalStateException("No edit model has been set for table model: " + this);
    }
    return editModel;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isEditable() {
    return editable;
  }

  /** {@inheritDoc} */
  @Override
  public final void setEditable(final boolean editable) {
    this.editable = editable;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isBatchUpdateEnabled() {
    return batchUpdateEnabled;
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityTableModel setBatchUpdateEnabled(final boolean batchUpdateEnabled) {
    this.batchUpdateEnabled = batchUpdateEnabled;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final SwingEntityTableModel setRefreshOnForeignKeyConditionValuesSet(final boolean value) {
    this.refreshOnForeignKeyConditionValuesSet = value;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isRefreshOnForeignKeyConditionValuesSet() {
    return refreshOnForeignKeyConditionValuesSet;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isDeleteEnabled() {
    return editModel != null && editModel.isDeleteEnabled();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isUpdateEnabled() {
    return editModel != null && editModel.isUpdateEnabled();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isReadOnly() {
    return editModel == null || editModel.isReadOnly();
  }

  /** {@inheritDoc} */
  @Override
  public final int getPropertyColumnIndex(final String propertyId) {
    return getColumnModel().getColumnIndex(getEntityDefinition().getProperty(propertyId));
  }

  /**
   * Returns true if the cell at <code>rowIndex</code> and <code>modelColumnIndex</code> is editable.
   * @param rowIndex the row whose value to be queried
   * @param modelColumnIndex the column whose value to be queried
   * @return true if the cell is editable
   * @see #setValueAt
   */
  @Override
  public boolean isCellEditable(final int rowIndex, final int modelColumnIndex) {
    return editable && !isReadOnly() && isUpdateEnabled() &&
            !getColumnModel().getColumnIdentifier(modelColumnIndex).isReadOnly();
  }

  /**
   * Returns the value for the cell at <code>modelColumnIndex</code> and <code>rowIndex</code>.
   * @param rowIndex the row whose value is to be queried
   * @param modelColumnIndex the column whose value is to be queried
   * @return the value Object at the specified cell
   */
  @Override
  public final Object getValueAt(final int rowIndex, final int modelColumnIndex) {
    final Property property = getColumnModel().getColumnIdentifier(modelColumnIndex);
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
    final Entity entity = getDomain().copyEntity(getItemAt(rowIndex));

    entity.put(getColumnModel().getColumnIdentifier(modelColumnIndex), value);
    try {
      update(singletonList(entity));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * A convenience method for setting the sorting directive for the given property
   * @param propertyId the property ID
   * @param directive the directive
   * @param addColumnToSort if true then the column is added to the sorting state
   * @see TableSortModel#setSortingDirective(Object, SortingDirective, boolean)
   */
  public final void setSortingDirective(final String propertyId, final SortingDirective directive, final boolean addColumnToSort) {
    getSortModel().setSortingDirective(getEntityDefinition().getProperty(propertyId), directive, addColumnToSort);
  }

  /** {@inheritDoc} */
  @Override
  public Color getPropertyBackgroundColor(final int row, final Property property) {
    return (Color) getItemAt(row).getColor(property);
  }

  /** {@inheritDoc} */
  @Override
  public final Entity getEntityByKey(final Entity.Key primaryKey) {
    return getVisibleItems().stream().filter(entity -> entity.getKey().equals(primaryKey)).findFirst().orElse(null);
  }

  /** {@inheritDoc} */
  @Override
  public final int indexOf(final Entity.Key primaryKey) {
    return indexOf(getEntityByKey(primaryKey));
  }

  /** {@inheritDoc} */
  @Override
  public final void addEntities(final List<Entity> entities, final boolean atTop, final boolean sortAfterAdding) {
    addItems(entities, atTop, sortAfterAdding);
  }

  /** {@inheritDoc} */
  @Override
  public final void replaceEntities(final Collection<Entity> entities) {
    replaceEntitiesByKey(Entities.mapToKey(entities));
  }

  /** {@inheritDoc} */
  @Override
  public final void refreshEntities(final List<Entity.Key> keys) {
    try {
      replaceEntities(getConnectionProvider().getConnection().select(keys));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setForeignKeyConditionValues(final ForeignKeyProperty foreignKeyProperty, final Collection<Entity> foreignKeyValues) {
    requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    if (conditionModel.setConditionValues(foreignKeyProperty.getPropertyId(), foreignKeyValues) && refreshOnForeignKeyConditionValuesSet) {
      refresh();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void replaceForeignKeyValues(final String foreignKeyEntityId, final Collection<Entity> foreignKeyValues) {
    requireNonNull(foreignKeyValues, "foreignKeyValues");
    final List<ForeignKeyProperty> foreignKeyProperties =
            getEntityDefinition().getForeignKeyReferences(requireNonNull(foreignKeyEntityId, "foreignKeyEntityId"));
    boolean changed = false;
    for (final Entity entity : getAllItems()) {
      for (final ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
        for (final Entity foreignKeyValue : foreignKeyValues) {
          final Entity currentForeignKeyValue = entity.getForeignKey(foreignKeyProperty.getPropertyId());
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

  /** {@inheritDoc} */
  @Override
  public final void setSelectedByKey(final Collection<Entity.Key> keys) {
    requireNonNull(keys, "keys");
    final List<Entity.Key> keyList = new ArrayList<>(keys);
    final List<Integer> indexes = new ArrayList<>();
    for (final Entity visibleEntity : getVisibleItems()) {
      final int index = keyList.indexOf(visibleEntity.getKey());
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

  /** {@inheritDoc} */
  @Override
  public final Collection<Entity> getEntitiesByKey(final Collection<Entity.Key> keys) {
    requireNonNull(keys, "keys");
    return getAllItems().stream().filter(entity -> keys.contains(entity.getKey())).collect(Collectors.toList());
  }

  /** {@inheritDoc} */
  @Override
  public final void deleteSelected() throws DatabaseException {
    if (!isDeleteEnabled()) {
      throw new IllegalStateException("Deleting is not enabled in this table model");
    }
    editModel.delete(getSelectionModel().getSelectedItems());
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public final ColumnSummaryModel getColumnSummaryModel(final String propertyId) {
    return getColumnSummaryModel(getEntityDefinition().getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public final Iterator<Entity> getSelectedEntitiesIterator() {
    return getSelectionModel().getSelectedItems().iterator();
  }

  /** {@inheritDoc} */
  @Override
  public final void setColumns(final String... propertyIds) {
    getColumnModel().setColumns(getEntityDefinition().getProperties(asList(propertyIds)).toArray(new Property[0]));
  }

  /** {@inheritDoc} */
  @Override
  public final void savePreferences() {
    if (EntityModel.USE_CLIENT_PREFERENCES.get()) {
      try {
        PreferencesUtil.putUserPreference(getUserPreferencesKey(), createPreferences().toString());
      }
      catch (final Exception e) {
        LOG.error("Error while saving preferences", e);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final String getTableDataAsDelimitedString(final char delimiter) {
    final List<String> header = new ArrayList<>();
    final List<Property> properties = new ArrayList<>();
    final Enumeration<TableColumn> columnEnumeration = getColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final Property property = (Property) columnEnumeration.nextElement().getIdentifier();
      properties.add(property);
      header.add(property.getCaption());
    }

    return Text.getDelimitedString(header, Entities.getStringValueList(properties,
            getSelectionModel().isSelectionEmpty() ? getVisibleItems() : getSelectionModel().getSelectedItems()),
            String.valueOf(delimiter));
  }

  /** {@inheritDoc} */
  @Override
  public final void addSelectionChangedListener(final EventListener listener) {
    getSelectionModel().addSelectionChangedListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  protected final ColumnSummaryModel.ColumnValueProvider createColumnValueProvider(final Property property) {
    return new DefaultColumnValueProvider(property, this, property.getFormat());
  }

  /** {@inheritDoc} */
  @Override
  protected final void doRefresh() {
    try {
      LOG.debug("{} refreshing", this);
      final List<Entity> queryResult = performQuery();
      clear();
      addItems(queryResult, true, true);
      conditionModel.rememberCurrentConditionState();
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
    if (!getConditionModel().isEnabled() && queryConditionRequiredState.get()) {
      return emptyList();
    }

    try {
      return connectionProvider.getConnection().select(entitySelectCondition(entityId,
              getConditionModel().getCondition()).setFetchCount(fetchCount).setOrderBy(getOrderBy()));
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
  protected Object getValue(final Entity entity, final Property property) {
    requireNonNull(entity, "entity");
    requireNonNull(property, "property");
    if (property instanceof ValueListProperty) {
      return entity.getAsString(property);
    }

    return entity.get(property);
  }

  /** {@inheritDoc} */
  @Override
  protected final String getSearchValueAt(final int rowIndex, final TableColumn column) {
    return getItemAt(rowIndex).getAsString((Property) column.getIdentifier());
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
   * Called after a delete has been performed via the associated edit model.
   * @param deletedEntities the deleted entities
   */
  protected void onDelete(final List<Entity> deletedEntities) {/*Provided for subclasses*/}

  /**
   * Override to handle the edit model being set.
   * @param editModel the edit model that was just set, never null
   * @see #setEditModel(SwingEntityEditModel)
   */
  protected void onSetEditModel(final SwingEntityEditModel editModel) {/*Provided for subclasses*/}

  /**
   * Returns the key used to identify user preferences for this table model, that is column positions, widths and such.
   * The default implementation is:
   * <pre>
   * {@code
   * return getClass().getSimpleName() + "-" + getEntityId();
   * }
   * </pre>
   * Override in case this key is not unique.
   * @return the key used to identify user preferences for this table model
   */
  protected String getUserPreferencesKey() {
    return getClass().getSimpleName() + "-" + getEntityId();
  }

  /**
   * Clears any user preferences saved for this table model
   */
  final void clearPreferences() {
    PreferencesUtil.removeUserPreference(getUserPreferencesKey());
  }

  private void bindEvents() {
    getColumnModel().addColumnHiddenListener(this::onColumnHidden);
    conditionModel.addSimpleConditionListener(this::refresh);
  }

  private void bindEditModelEvents() {
    editModel.addAfterInsertListener(this::onInsert);
    editModel.addAfterUpdateListener(this::onUpdate);
    editModel.addAfterDeleteListener(this::onDeleteInternal);
    editModel.addAfterRefreshListener(this::refresh);
    editModel.addEntitySetListener(data -> {
      if (data == null && !getSelectionModel().isSelectionEmpty()) {
        getSelectionModel().clearSelection();
      }
    });
    getSelectionModel().addSelectedIndexListener(selected -> {
      final Entity itemToSelect = getSelectionModel().isSelectionEmpty() ? null : getSelectionModel().getSelectedItem();
      editModel.setEntity(itemToSelect);
    });

    addTableModelListener(e -> {
      //if the selected record is being updated via the table model refresh the one in the edit model
      if (e.getType() == TableModelEvent.UPDATE && e.getFirstRow() == getSelectionModel().getSelectedIndex()) {
        editModel.setEntity(getSelectionModel().getSelectedItem());
      }
    });
  }

  private void onInsert(final List<Entity> insertedEntities) {
    getSelectionModel().clearSelection();
    if (!insertAction.equals(InsertAction.DO_NOTHING)) {
      final List<Entity> entitiesToAdd = insertedEntities.stream().filter(entity ->
              entity.getEntityId().equals(getEntityId())).collect(Collectors.toList());
      switch (insertAction) {
        case ADD_TOP:
          addEntities(entitiesToAdd, true, false);
          break;
        case ADD_BOTTOM:
          addEntities(entitiesToAdd, false, false);
          break;
        case ADD_TOP_SORTED:
          addEntities(entitiesToAdd, true, true);
          break;
      }
    }
  }

  private void onUpdate(final Map<Entity.Key, Entity> updatedEntities) {
    replaceEntitiesByKey(new HashMap<>(updatedEntities));
  }

  /**
   * Replace the entities identified by the Entity.Key map keys with their respective value.
   * Note that this does not trigger {@link #filterContents()}, that must be done explicitly.
   * @param entitiesByKey the entities to replace mapped to the corresponding primary key found in this table model
   */
  private void replaceEntitiesByKey(final Map<Entity.Key, Entity> entitiesByKey) {
    for (final Entity entity : getAllItems()) {
      final Iterator<Map.Entry<Entity.Key, Entity>> mapIterator = entitiesByKey.entrySet().iterator();
      while (mapIterator.hasNext()) {
        final Map.Entry<Entity.Key, Entity> entry = mapIterator.next();
        if (entity.getKey().equals(entry.getKey())) {
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

  private void onDeleteInternal(final List<Entity> deletedEntities) {
    if (removeEntitiesOnDelete) {
      removeItems(deletedEntities);
    }
    onDelete(deletedEntities);
  }

  private void onColumnHidden(final Property property) {
    //disable the condition model for the column to be hidden, to prevent confusion
    conditionModel.setEnabled(property.getPropertyId(), false);
  }

  private org.json.JSONObject createPreferences() throws Exception {
    final org.json.JSONObject preferencesRoot = new org.json.JSONObject();
    preferencesRoot.put(PREFERENCES_COLUMNS, createColumnPreferences());

    return preferencesRoot;
  }

  private org.json.JSONObject createColumnPreferences() throws Exception {
    final org.json.JSONObject columnPreferencesRoot = new org.json.JSONObject();
    for (final TableColumn column : getColumnModel().getAllColumns()) {
      final Property property = (Property) column.getIdentifier();
      final org.json.JSONObject columnObject = new org.json.JSONObject();
      final boolean visible = getColumnModel().isColumnVisible(property);
      columnObject.put(PREFERENCES_COLUMN_WIDTH, column.getWidth());
      columnObject.put(PREFERENCES_COLUMN_VISIBLE, visible);
      columnObject.put(PREFERENCES_COLUMN_INDEX, visible ? getColumnModel().getColumnIndex(property) : -1);
      columnPreferencesRoot.put(property.getPropertyId(), columnObject);
    }

    return columnPreferencesRoot;
  }

  private void applyPreferences() {
    if (EntityModel.USE_CLIENT_PREFERENCES.get()) {
      final String preferencesString = PreferencesUtil.getUserPreference(getUserPreferencesKey(), "");
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
    final SwingFilteredTableColumnModel<Entity, Property> columnModel = getColumnModel();
    for (final TableColumn column : Collections.list(columnModel.getColumns())) {
      final Property property = (Property) column.getIdentifier();
      if (columnModel.containsColumn(property)) {
        try {
          final org.json.JSONObject columnPreferences = preferences.getJSONObject(property.getPropertyId());
          column.setPreferredWidth(columnPreferences.getInt(PREFERENCES_COLUMN_WIDTH));
          if (columnPreferences.getBoolean(PREFERENCES_COLUMN_VISIBLE)) {
            final int index = Math.min(columnModel.getColumnCount() - 1, columnPreferences.getInt(PREFERENCES_COLUMN_INDEX));
            columnModel.moveColumn(getColumnModel().getColumnIndex(column.getIdentifier()), index);
          }
          else {
            columnModel.hideColumn((Property) column.getIdentifier());
          }
        }
        catch (final Exception e) {
          LOG.info("Property preferences not found: " + property, e);
        }
      }
    }
  }

  /**
   * A default sort model implementation based on Entity
   */
  public static class DefaultEntityTableSortModel extends AbstractTableSortModel<Entity, Property> {

    private final EntityDefinition.Provider definitionProvider;

    /**
     * Instantiates a new DefaultEntityTableSortModel
     * @param definitionProvider the domain entity definition provider
     * @param entityId the entity ID
     */
    public DefaultEntityTableSortModel(final EntityDefinition.Provider definitionProvider, final String entityId) {
      super(initializeColumns(definitionProvider.getDefinition(entityId).getVisibleProperties()));
      this.definitionProvider = definitionProvider;
    }

    /** {@inheritDoc} */
    @Override
    public final Class getColumnClass(final Property property) {
      return property.getTypeClass();
    }

    /** {@inheritDoc} */
    @Override
    protected Comparator initializeColumnComparator(final Property property) {
      if (property instanceof ForeignKeyProperty) {
        return definitionProvider.getDefinition(((ForeignKeyProperty) property).getForeignEntityId()).getComparator();
      }

      return super.initializeColumnComparator(property);
    }

    /** {@inheritDoc} */
    @Override
    protected final Comparable getComparable(final Entity row, final Property property) {
      return (Comparable) row.get(property);
    }

    private static List<TableColumn> initializeColumns(final List<Property> visibleProperties) {
      final List<TableColumn> columns = new ArrayList<>(visibleProperties.size());
      for (final Property property : visibleProperties) {
        final TableColumn column = new TableColumn(columns.size());
        column.setIdentifier(property);
        column.setHeaderValue(property.getCaption());
        if (property.getPreferredColumnWidth() > 0) {
          column.setPreferredWidth(property.getPreferredColumnWidth());
        }
        columns.add(column);
      }

      return columns;
    }
  }
}