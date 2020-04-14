/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.Text;
import org.jminor.common.UserPreferences;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.table.ColumnSummaryModel;
import org.jminor.common.state.State;
import org.jminor.common.state.States;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entities;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.EntityDefinition;
import org.jminor.framework.domain.entity.OrderBy;
import org.jminor.framework.domain.entity.exception.ValidationException;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.model.DefaultEntityTableConditionModel;
import org.jminor.framework.model.EntityModel;
import org.jminor.framework.model.EntityTableConditionModel;
import org.jminor.framework.model.EntityTableModel;
import org.jminor.javafx.framework.ui.EntityTableColumn;

import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
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
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.jminor.framework.db.condition.Conditions.selectCondition;

/**
 * A JavaFX implementation of {@link EntityTableModel}.
 */
public class FXEntityListModel extends ObservableEntityList implements EntityTableModel<FXEntityEditModel> {

  private static final Logger LOG = LoggerFactory.getLogger(FXEntityListModel.class);

  private final EntityTableConditionModel conditionModel;
  private final State queryConditionRequiredState = States.state();

  private FXEntityEditModel editModel;
  private ObservableList<? extends TableColumn<Entity, ?>> columns;
  private List<PropertyTableColumn> initialColumns;

  private InsertAction insertAction = InsertAction.ADD_TOP;
  private boolean batchUpdateEnabled = true;
  private boolean removeEntitiesOnDelete = true;
  private boolean refreshOnForeignKeyConditionValuesSet = true;
  private boolean editable = false;
  private int fetchCount = -1;

  /**
   * Instantiates a new {@link FXEntityListModel} based on the given entityId
   * @param entityId the entityId
   * @param connectionProvider the connection provider
   */
  public FXEntityListModel(final String entityId, final EntityConnectionProvider connectionProvider) {
    this(entityId, connectionProvider, new DefaultEntityTableConditionModel(entityId, connectionProvider,
            null, new FXConditionModelProvider()));
  }

  /**
   * Instantiates a new {@link FXEntityListModel} based on the given entityId
   * @param entityId the entityId
   * @param connectionProvider the connection provider
   * @param conditionModel the {@link EntityTableConditionModel} to use
   * @throws IllegalArgumentException in case the condition model is based on a different entity
   */
  public FXEntityListModel(final String entityId, final EntityConnectionProvider connectionProvider,
                           final EntityTableConditionModel conditionModel) {
    super(entityId, connectionProvider);
    requireNonNull(conditionModel);
    if (!conditionModel.getEntityId().equals(entityId)) {
      throw new IllegalArgumentException("Entity ID mismatch, conditionModel: " + conditionModel.getEntityId()
              + ", tableModel: " + entityId);
    }
    if (getEntityDefinition().getVisibleProperties().isEmpty()) {
      throw new IllegalArgumentException("No visible properties defined for entity: " + entityId);
    }
    this.conditionModel = conditionModel;
    bindEvents();
  }

  @Override
  public final Domain getDomain() {
    return getConnectionProvider().getDomain();
  }

  @Override
  public final EntityDefinition getEntityDefinition() {
    return getDomain().getDefinition(getEntityId());
  }

  @Override
  public final void setEditModel(final FXEntityEditModel editModel) {
    requireNonNull(editModel, "editModel");
    if (this.editModel != null) {
      throw new IllegalStateException("Edit model has already been set");
    }
    if (!editModel.getEntityId().equals(getEntityId())) {
      throw new IllegalArgumentException("Entity ID mismatch, editModel: " + editModel.getEntityId() + ", tableModel: " + getEntityId());
    }
    this.editModel = editModel;
    bindEditModelEvents();
    onSetEditModel(editModel);
  }

  @Override
  public final FXEntityEditModel getEditModel() {
    if (editModel == null) {
      throw new IllegalStateException("No edit model has been set for list: " + this);
    }
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
    this.initialColumns = new ArrayList<>((Collection<PropertyTableColumn>) columns);
    applyPreferences();
  }

  /**
   * Returns the table column for property with the given id
   * @param propertyId the propertyId
   * @return the column
   * @throws IllegalArgumentException in case the column was not found
   */
  public final EntityTableColumn getTableColumn(final String propertyId) {
    final Optional<? extends TableColumn<Entity, ?>> tableColumn = columns.stream()
            .filter((Predicate<TableColumn<Entity, ?>>) entityTableColumn ->
                    ((EntityTableColumn) entityTableColumn).getProperty().getPropertyId().equals(propertyId)).findFirst();

    if (tableColumn.isPresent()) {
      return (EntityTableColumn) tableColumn.get();
    }

    throw new IllegalArgumentException("Column for property with id: " + propertyId + " not found");
  }

  @Override
  public final EntityTableConditionModel getConditionModel() {
    return conditionModel;
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
  public final void setForeignKeyConditionValues(final ForeignKeyProperty foreignKeyProperty, final Collection<Entity> entities) {
    if (conditionModel.setConditionValues(foreignKeyProperty.getPropertyId(), entities) && refreshOnForeignKeyConditionValuesSet) {
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
  public final boolean hasEditModel() {
    return editModel != null;
  }

  @Override
  public final void replaceForeignKeyValues(final String foreignKeyEntityId, final Collection<Entity> foreignKeyValues) {
    final List<ForeignKeyProperty> foreignKeyProperties =
            getEntityDefinition().getForeignKeyReferences(foreignKeyEntityId);
    for (final Entity entity : getItems()) {
      for (final ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
        for (final Entity foreignKeyValue : foreignKeyValues) {
          final Entity currentForeignKeyValue = entity.getForeignKey(foreignKeyProperty.getPropertyId());
          if (Objects.equals(currentForeignKeyValue, foreignKeyValue)) {
            currentForeignKeyValue.setAs(foreignKeyValue);
          }
        }
      }
    }
  }

  @Override
  public final void addEntities(final List<Entity> entities, final boolean atTop, final boolean sortAfterAdding) {
    addAll(atTop ? 0 : getSize(), entities);
    if (sortAfterAdding) {
      sort(getSortedList().getComparator());
    }
  }

  @Override
  public final void replaceEntities(final Collection<Entity> entities) {
    replaceEntitiesByKey(Entities.mapToKey(entities));
  }

  @Override
  public final void refreshEntities(final List<Entity.Key> keys) {
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
  public final FXEntityListModel setBatchUpdateEnabled(final boolean batchUpdateEnabled) {
    this.batchUpdateEnabled = batchUpdateEnabled;
    return this;
  }

  @Override
  public final FXEntityListModel setRefreshOnForeignKeyConditionValuesSet(final boolean refreshOnForeignKeyConditionValuesSet) {
    this.refreshOnForeignKeyConditionValuesSet = refreshOnForeignKeyConditionValuesSet;
    return this;
  }

  @Override
  public final boolean isRefreshOnForeignKeyConditionValuesSet() {
    return refreshOnForeignKeyConditionValuesSet;
  }

  @Override
  public final ColumnSummaryModel getColumnSummaryModel(final String propertyId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final Color getPropertyBackgroundColor(final int row, final Property property) {
    return (Color) get(row).getColor(property);
  }

  @Override
  public final int getPropertyColumnIndex(final String propertyId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final int getFetchCount() {
    return fetchCount;
  }

  @Override
  public final FXEntityListModel setFetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
    return this;
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
  public final FXEntityListModel setRemoveEntitiesOnDelete(final boolean removeEntitiesOnDelete) {
    this.removeEntitiesOnDelete = removeEntitiesOnDelete;
    return this;
  }

  @Override
  public final InsertAction getInsertAction() {
    return insertAction;
  }

  @Override
  public final FXEntityListModel setInsertAction(final InsertAction insertAction) {
    requireNonNull(insertAction);
    this.insertAction = insertAction;
    return this;
  }

  @Override
  public final Collection<Entity> getEntitiesByKey(final Collection<Entity.Key> keys) {
    return getItems().stream().filter(entity -> keys.stream()
            .anyMatch(key -> entity.getKey().equals(key))).collect(Collectors.toList());
  }

  @Override
  public final void setSelectedByKey(final Collection<Entity.Key> keys) {
    final List<Entity.Key> keyList = new ArrayList<>(keys);
    final List<Entity> toSelect = new ArrayList<>(keys.size());
    stream().filter(entity -> keyList.contains(entity.getKey())).forEach(entity -> {
      toSelect.add(entity);
      keyList.remove(entity.getKey());
    });
    getSelectionModel().setSelectedItems(toSelect);
  }

  @Override
  public final Entity getEntityByKey(final Entity.Key primaryKey) {
    return getFilteredList().stream().filter(entity -> entity.getKey().equals(primaryKey)).findFirst().orElse(null);
  }

  @Override
  public final Iterator<Entity> getSelectedEntitiesIterator() {
    return getSelectionModel().getSelectedItems().iterator();
  }

  @Override
  public final int indexOf(final Entity.Key primaryKey) {
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
  public final void setColumns(final String... propertyIds) {
    final List<String> propertyIdList = asList(propertyIds);
    new ArrayList<>(columns).forEach(column -> {
      if (!propertyIdList.contains(((PropertyTableColumn) column).getProperty().getPropertyId())) {
        columns.remove(column);
      }
    });
    columns.sort((col1, col2) -> {
      final Integer first = propertyIdList.indexOf(((PropertyTableColumn) col1).getProperty().getPropertyId());
      final Integer second = propertyIdList.indexOf(((PropertyTableColumn) col2).getProperty().getPropertyId());

      return first.compareTo(second);
    });
  }

  @Override
  public final String getTableDataAsDelimitedString(final char delimiter) {
    final List<String> header = new ArrayList<>();
    final List<Property> properties = new ArrayList<>();
    columns.forEach(entityTableColumn -> {
      final Property property = ((PropertyTableColumn) entityTableColumn).getProperty();
      properties.add(property);
      header.add(property.getCaption());
    });

    return Text.getDelimitedString(header, Entities.getStringValueList(properties,
            getSelectionModel().isSelectionEmpty() ? getVisibleItems() : getSelectionModel().getSelectedItems()),
            String.valueOf(delimiter));
  }

  /**
   * Queries for the data used to populate this EntityTableModel when it is refreshed,
   * using the order by clause returned by {@link #getOrderBy()}
   * @return entities selected from the database according the the query condition.
   * @see #getQueryConditionRequiredState()
   * @see EntityTableConditionModel#getCondition()
   */
  @Override
  protected List<Entity> performQuery() {
    if (!conditionModel.isEnabled() && queryConditionRequiredState.get()) {
      return emptyList();
    }

    try {
      return getConnectionProvider().getConnection().select(selectCondition(
              getEntityId(), conditionModel.getCondition()).setFetchCount(fetchCount).setOrderBy(getOrderBy()));
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
    return getEntityDefinition().getOrderBy();
  }

  /**
   * Override to handle the edit model being set.
   * @param editModel the edit model that was just set, never null
   * @see #setEditModel(FXEntityEditModel)
   */
  protected void onSetEditModel(final FXEntityEditModel editModel) {/*Provided for subclasses*/}

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

  private void onDelete(final List<Entity> deletedEntities) {
    if (removeEntitiesOnDelete) {
      removeAll(deletedEntities);
    }
  }

  /**
   * Replace the entities identified by the Entity.Key map keys with their respective value
   * @param entityMap the entities to replace mapped to the corresponding primary key found in this table model
   */
  private void replaceEntitiesByKey(final Map<Entity.Key, Entity> entityMap) {
    final List<Integer> selected = getSelectionModel().getSelectedIndexes();
    replaceAll(entity -> {
      final Entity toReplaceWith = entityMap.get(entity.getKey());
      return toReplaceWith == null ? entity : toReplaceWith;
    });
    getSelectionModel().setSelectedIndexes(selected);
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
    for (final PropertyTableColumn column : initialColumns) {
      final Property property = column.getProperty();
      if (columns.contains(column)) {
        try {
          final org.json.JSONObject columnPreferences = preferences.getJSONObject(property.getPropertyId());
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
    for (final PropertyTableColumn column : initialColumns) {
      final Property property = column.getProperty();
      final org.json.JSONObject columnObject = new org.json.JSONObject();
      final boolean visible = columns.contains(column);
      columnObject.put(PREFERENCES_COLUMN_WIDTH, column.getWidth());
      columnObject.put(PREFERENCES_COLUMN_VISIBLE, visible);
      columnObject.put(PREFERENCES_COLUMN_INDEX, visible ? columns.indexOf(column) : -1);
      columnPreferencesRoot.put(property.getPropertyId(), columnObject);
    }

    return columnPreferencesRoot;
  }

  private void bindEditModelEvents() {
    getEditModel().addAfterInsertListener(this::onInsert);
    getEditModel().addAfterUpdateListener(this::onUpdate);
    getEditModel().addAfterDeleteListener(this::onDelete);
    getEditModel().addAfterRefreshListener(this::refresh);
    getEditModel().addEntitySetListener(entity -> {
      if (entity == null && selectionModelHasBeenSet() && !getSelectionModel().isSelectionEmpty()) {
        getSelectionModel().clearSelection();
      }
    });
  }

  private void bindEvents() {
    addRefreshListener(conditionModel::rememberCondition);
  }

  /**
   * A {@link TableColumn} based on a {@link Property} instance
   */
  public static class PropertyTableColumn extends TableColumn<Entity, Object> {

    private final Property property;

    protected PropertyTableColumn(final Property property) {
      super(property.getCaption());
      this.property = property;
    }

    /**
     * @return the underlying property
     */
    public final Property getProperty() {
      return property;
    }

    @Override
    public final String toString() {
      return property.getPropertyId();
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
        final org.json.JSONObject columnOnePreferences = preferences.getJSONObject(((PropertyTableColumn) col1).getProperty().getPropertyId());
        final org.json.JSONObject columnTwoPreferences = preferences.getJSONObject(((PropertyTableColumn) col2).getProperty().getPropertyId());
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
