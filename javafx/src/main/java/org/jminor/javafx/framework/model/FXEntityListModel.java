/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.TextUtil;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.PreferencesUtil;
import org.jminor.common.model.table.ColumnSummaryModel;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultEntityTableConditionModel;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.framework.model.EntityTableConditionModel;
import org.jminor.framework.model.EntityTableModel;

import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FXEntityListModel extends ObservableEntityList implements EntityTableModel<FXEntityEditModel> {

  private static final Logger LOG = LoggerFactory.getLogger(FXEntityListModel.class);

  private final EntityTableConditionModel conditionModel;

  private FXEntityEditModel editModel;
  private ObservableList<? extends TableColumn<Entity, ?>> columns;
  private List<PropertyTableColumn> initialColumns;

  private InsertAction insertAction = InsertAction.ADD_TOP;
  private boolean queryConditionRequired = false;
  private boolean queryConfigurationAllowed = true;
  private boolean batchUpdateAllowed = true;
  private boolean removeEntitiesOnDelete = true;
  private int fetchCount = -1;

  public FXEntityListModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this(entityID, connectionProvider, new DefaultEntityTableConditionModel(entityID, connectionProvider,
            null, new FXConditionModelProvider()));
  }

  public FXEntityListModel(final String entityID, final EntityConnectionProvider connectionProvider,
                           final EntityTableConditionModel conditionModel) {
    super(entityID, connectionProvider);
    if (!conditionModel.getEntityID().equals(entityID)) {
      throw new IllegalArgumentException("Entity ID mismatch, conditionModel: " + conditionModel.getEntityID()
              + ", tableModel: " + entityID);
    }
    if (Entities.getVisibleProperties(entityID).isEmpty()) {
      throw new IllegalStateException("No visible properties defined for entity: " + entityID);
    }
    this.conditionModel = conditionModel;
    bindEvents();
  }

  /** {@inheritDoc} */
  @Override
  public final void setEditModel(final FXEntityEditModel editModel) {
    Objects.requireNonNull(editModel, "editModel");
    if (this.editModel != null) {
      throw new IllegalStateException("Edit model has already been set");
    }
    if (!editModel.getEntityID().equals(getEntityID())) {
      throw new IllegalArgumentException("Entity ID mismatch, editModel: " + editModel.getEntityID() + ", tableModel: " + getEntityID());
    }
    this.editModel = editModel;
    bindEditModelEvents();
  }

  /** {@inheritDoc} */
  @Override
  public final FXEntityEditModel getEditModel() {
    if (editModel == null) {
      throw new IllegalStateException("No edit model has been set for list: " + this);
    }
    return editModel;
  }

  public final void setColumns(final ObservableList<? extends TableColumn<Entity, ?>> columns) {
    if (this.columns != null) {
      throw new IllegalStateException("Columns have already been set");
    }
    this.columns = columns;
    this.initialColumns = new ArrayList<>((Collection<PropertyTableColumn>) columns);
    applyPreferences();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableConditionModel getConditionModel() {
    return conditionModel;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isQueryConditionRequired() {
    return queryConditionRequired;
  }

  /** {@inheritDoc} */
  @Override
  public final FXEntityListModel setQueryConditionRequired(final boolean value) {
    this.queryConditionRequired = value;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final void setForeignKeyConditionValues(final Property.ForeignKeyProperty foreignKeyProperty, final Collection<Entity> entities) {
    if (conditionModel.setConditionValues(foreignKeyProperty.getPropertyID(), entities)) {
      refresh();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void deleteSelected() throws DatabaseException {
    getEditModel().delete(getSelectionModel().getSelectedItems());
  }

  /** {@inheritDoc} */
  @Override
  public final int getRowCount() {
    return size();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean hasEditModel() {
    return editModel != null;
  }

  /** {@inheritDoc} */
  @Override
  public final void replaceForeignKeyValues(final String foreignKeyEntityID, final Collection<Entity> foreignKeyValues) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(getEntityID(), foreignKeyEntityID);
    for (final Entity entity : getAllItems()) {
      for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
        for (final Entity foreignKeyValue : foreignKeyValues) {
          final Entity currentForeignKeyValue = entity.getForeignKey(foreignKeyProperty.getPropertyID());
          if (Objects.equals(currentForeignKeyValue, foreignKeyValue)) {
            currentForeignKeyValue.setAs(foreignKeyValue);
          }
        }
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void addEntities(final List<Entity> entities, final boolean atTop) {
    if (atTop) {
      addAll(0, entities);
    }
    else {
      addAll(entities);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void replaceEntities(final Collection<Entity> entities) {
    replaceEntitiesByKey(EntityUtil.mapToKey(entities));
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isQueryConfigurationAllowed() {
    return queryConfigurationAllowed;
  }

  /** {@inheritDoc} */
  @Override
  public final FXEntityListModel setQueryConfigurationAllowed(final boolean queryConfigurationAllowed) {
    this.queryConfigurationAllowed = queryConfigurationAllowed;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isDeleteAllowed() {
    return editModel != null && editModel.isDeleteAllowed();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isReadOnly() {
    return editModel == null || editModel.isReadOnly();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isUpdateAllowed() {
    return editModel != null && editModel.isUpdateAllowed();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isBatchUpdateAllowed() {
    return batchUpdateAllowed;
  }

  /** {@inheritDoc} */
  @Override
  public final FXEntityListModel setBatchUpdateAllowed(final boolean batchUpdateAllowed) {
    this.batchUpdateAllowed = batchUpdateAllowed;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnSummaryModel getColumnSummaryModel(final String propertyID) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public final Color getPropertyBackgroundColor(final int row, final Property property) {
    return (Color) get(row).getBackgroundColor(property);
  }

  /** {@inheritDoc} */
  @Override
  public final int getPropertyColumnIndex(final String propertyID) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public final String getStatusMessage() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public final int getFetchCount() {
    return fetchCount;
  }

  /** {@inheritDoc} */
  @Override
  public final FXEntityListModel setFetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final void update(final List<Entity> entities) throws ValidationException, DatabaseException {
    Objects.requireNonNull(entities);
    if (!isUpdateAllowed()) {
      throw new IllegalStateException("Updating is not allowed via this table model");
    }
    if (entities.size() > 1 && !batchUpdateAllowed) {
      throw new IllegalStateException("Batch update of entities is not allowed!");
    }
    editModel.update(entities);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isRemoveEntitiesOnDelete() {
    return removeEntitiesOnDelete;
  }

  /** {@inheritDoc} */
  @Override
  public final FXEntityListModel setRemoveEntitiesOnDelete(final boolean removeEntitiesOnDelete) {
    this.removeEntitiesOnDelete = removeEntitiesOnDelete;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final InsertAction getInsertAction() {
    return insertAction;
  }

  /** {@inheritDoc} */
  @Override
  public final FXEntityListModel setInsertAction(final InsertAction insertAction) {
    Objects.requireNonNull(insertAction);
    this.insertAction = insertAction;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<Entity> getEntitiesByKey(final Collection<Entity.Key> keys) {
    final List<Entity> entities = new ArrayList<>();
    getAllItems().forEach(entity -> keys.forEach(key -> {
      if (entity.getKey().equals(key)) {
        entities.add(entity);
      }
    }));

    return entities;
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public final Collection<Entity> getEntitiesByPropertyValue(final Map<String, Object> values) {
    final List<Entity> entities = new ArrayList<>();
    for (final Entity entity : getAllItems()) {
      boolean equal = true;
      for (final Map.Entry<String, Object> entries : values.entrySet()) {
        final String propertyID = entries.getKey();
        if (!entity.get(propertyID).equals(entries.getValue())) {
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

  /** {@inheritDoc} */
  @Override
  public final Entity getEntityByKey(final Entity.Key primaryKey) {
    for (final Entity entity : getFilteredList()) {
      if (entity.getKey().equals(primaryKey)) {
        return entity;
      }
    }

    return null;

  }

  /** {@inheritDoc} */
  @Override
  public final Iterator<Entity> getSelectedEntitiesIterator() {
    return getSelectionModel().getSelectedItems().iterator();
  }

  /** {@inheritDoc} */
  @Override
  public final int indexOf(final Entity.Key primaryKey) {
    return indexOf(getEntityByKey(primaryKey));
  }

  /** {@inheritDoc} */
  @Override
  public final void savePreferences() {
    if (Configuration.getBooleanValue(Configuration.USE_CLIENT_PREFERENCES)) {
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
  public final void setColumns(final String... propertyIDs) {
    final List<String> propertyIDList = Arrays.asList(propertyIDs);
    new ArrayList<>(columns).forEach(column -> {
      if (!propertyIDList.contains(((PropertyTableColumn) column).getProperty().getPropertyID())) {
        columns.remove(column);
      }
    });
    columns.sort((col1, col2) -> {
      final Integer first = propertyIDList.indexOf(((PropertyTableColumn) col1).getProperty().getPropertyID());
      final Integer second = propertyIDList.indexOf(((PropertyTableColumn) col2).getProperty().getPropertyID());

      return first.compareTo(second);
    });
  }

  /** {@inheritDoc} */
  @Override
  public final String getTableDataAsDelimitedString(final char delimiter) {
    final List<String> headerValues = new ArrayList<>();
    final List<Property> properties = new ArrayList<>();
    columns.forEach(entityTableColumn -> {
      final Property property = ((PropertyTableColumn) entityTableColumn).getProperty();
      properties.add(property);
      headerValues.add(property.getCaption());
    });

    final String[][] header = {headerValues.toArray(new String[headerValues.size()])};

    return TextUtil.getDelimitedString(header, EntityUtil.getStringValueArray(properties,
            getSelectionModel().isSelectionEmpty() ? getVisibleItems() : getSelectionModel().getSelectedItems()),
            String.valueOf(delimiter));
  }

  /** {@inheritDoc} */
  @Override
  protected List<Entity> queryContents() {
    final Condition<Property.ColumnProperty> condition = conditionModel.getTableCondition();
    if (condition == null && queryConditionRequired) {
      return new ArrayList<>();
    }

    try {
      return getConnectionProvider().getConnection().selectMany(EntityConditions.selectCondition(getEntityID(),
              condition, getOrderByClause(), fetchCount));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * The order by clause to use when selecting the data for this model,
   * by default the order by clause defined for the underlying entity
   * @return the order by clause
   * @see Entities#getOrderByClause(String)
   */
  protected String getOrderByClause() {
    return Entities.getOrderByClause(getEntityID());
  }

  /**
   * Returns the key used to identify user preferences for this table model, that is column positions, widths and such.
   * The default implementation is:
   * <pre>
   * {@code
   * return getClass().getSimpleName() + "-" + getEntityID();
   * }
   * </pre>
   * Override in case this key is not unique.
   * @return the key used to identify user preferences for this table model
   */
  protected String getUserPreferencesKey() {
    return getClass().getSimpleName() + "-" + getEntityID();
  }

  /** {@inheritDoc} */
  @Override
  protected void bindSelectionModelEvents() {
    super.bindSelectionModelEvents();
    getSelectionModel().addSelectedIndexListener(index -> {
      if (editModel != null) {
        editModel.setEntity(getSelectionModel().getSelectedItem());
      }
    });
  }

  private void handleInsert(final EntityEditModel.InsertEvent insertEvent) {
    getSelectionModel().clearSelection();
    if (!insertAction.equals(InsertAction.DO_NOTHING)) {
      addEntities(insertEvent.getInsertedEntities(), insertAction.equals(InsertAction.ADD_TOP));
    }
  }

  private void handleUpdate(final EntityEditModel.UpdateEvent updateEvent) {
    replaceEntitiesByKey(new HashMap<>(updateEvent.getUpdatedEntities()));
  }

  private void handleDelete(final EntityEditModel.DeleteEvent deleteEvent) {
    if (removeEntitiesOnDelete) {
      removeAll(deleteEvent.getDeletedEntities());
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
    if (Configuration.getBooleanValue(Configuration.USE_CLIENT_PREFERENCES)) {
      final String preferencesString = PreferencesUtil.getUserPreference(getUserPreferencesKey(), "");
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
          final org.json.JSONObject columnPreferences = preferences.getJSONObject(property.getPropertyID());
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
      columnPreferencesRoot.put(property.getPropertyID(), columnObject);
    }

    return columnPreferencesRoot;
  }

  private void bindEditModelEvents() {
    getEditModel().addAfterInsertListener(this::handleInsert);
    getEditModel().addAfterUpdateListener(this::handleUpdate);
    getEditModel().addAfterDeleteListener(this::handleDelete);
    getEditModel().addAfterRefreshListener(this::refresh);
    getEditModel().addEntitySetListener(entity -> {
      if (entity == null && !getSelectionModel().isSelectionEmpty()) {
        getSelectionModel().clearSelection();
      }
    });
  }

  private void bindEvents() {
    addRefreshListener(conditionModel::rememberCurrentConditionState);
  }

  public static class PropertyTableColumn extends TableColumn<Entity, Object> {

    private final Property property;

    protected PropertyTableColumn(final Property property) {
      super(property.getCaption());
      this.property = property;
    }

    public final Property getProperty() {
      return property;
    }

    @Override
    public final String toString() {
      return property.getPropertyID();
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
        final org.json.JSONObject columnOnePreferences = preferences.getJSONObject(((PropertyTableColumn) col1).getProperty().getPropertyID());
        final org.json.JSONObject columnTwoPreferences = preferences.getJSONObject(((PropertyTableColumn) col2).getProperty().getPropertyID());
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
