/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.ColumnSummaryModel;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultEntityTableCriteriaModel;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.framework.model.EntityTableCriteriaModel;
import org.jminor.framework.model.EntityTableModel;

import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FXEntityListModel extends ObservableEntityList implements EntityTableModel {

  private final EntityTableCriteriaModel criteriaModel;

  private FXEntityEditModel editModel;
  private ObservableList<? extends TableColumn<Entity, ?>> columns;

  private InsertAction insertAction = InsertAction.ADD_TOP;
  private boolean queryCriteriaRequired = false;
  private boolean queryConfigurationAllowed = true;
  private boolean batchUpdateAllowed = true;
  private boolean removeEntitiesOnDelete = true;
  private int fetchCount = -1;

  public FXEntityListModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this(entityID, connectionProvider, new DefaultEntityTableCriteriaModel(entityID, connectionProvider,
            null, new FXCriteriaModelProvider()));
  }

  public FXEntityListModel(final String entityID, final EntityConnectionProvider connectionProvider,
                           final EntityTableCriteriaModel criteriaModel) {
    super(entityID, connectionProvider);
    if (!criteriaModel.getEntityID().equals(entityID)) {
      throw new IllegalArgumentException("Entity ID mismatch, criteriaModel: " + criteriaModel.getEntityID()
              + ", tableModel: " + entityID);
    }
    if (Entities.getVisibleProperties(entityID).isEmpty()) {
      throw new IllegalStateException("No visible properties defined for entity: " + entityID);
    }
    this.criteriaModel = criteriaModel;
    bindEvents();
  }

  public final void setEditModel(final EntityEditModel editModel) {
    Util.rejectNullValue(editModel, "editModel");
    if (this.editModel != null) {
      throw new IllegalStateException("Edit model has already been set");
    }
    if (!editModel.getEntityID().equals(getEntityID())) {
      throw new IllegalArgumentException("Entity ID mismatch, editModel: " + editModel.getEntityID() + ", tableModel: " + getEntityID());
    }
    this.editModel = (FXEntityEditModel) editModel;
    bindEditModelEvents();
  }

  public final FXEntityEditModel getEditModel() {
    if (editModel == null) {
      throw new IllegalStateException("No edit model has been set for list: " + this);
    }
    return editModel;
  }

  public final void setColumns(final ObservableList<? extends TableColumn<Entity, ?>> columns) {
    this.columns = columns;
  }

  public final EntityTableCriteriaModel getCriteriaModel() {
    return criteriaModel;
  }

  public final boolean isQueryCriteriaRequired() {
    return queryCriteriaRequired;
  }

  public final FXEntityListModel setQueryCriteriaRequired(final boolean value) {
    this.queryCriteriaRequired = value;
    return this;
  }

  public final void setForeignKeyCriteriaValues(final Property.ForeignKeyProperty foreignKeyProperty, final Collection<Entity> entities) {
    if (criteriaModel.setCriteriaValues(foreignKeyProperty.getPropertyID(), entities)) {
      refresh();
    }
  }

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
  public final EntityTableModel setQueryConfigurationAllowed(final boolean queryConfigurationAllowed) {
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
  public final EntityTableModel setBatchUpdateAllowed(final boolean batchUpdateAllowed) {
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
  public final EntityTableModel setFetchCount(final int fetchCount) {
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
  public final EntityTableModel setRemoveEntitiesOnDelete(final boolean removeEntitiesOnDelete) {
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
  public final EntityTableModel setInsertAction(final InsertAction insertAction) {
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
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public final void setColumns(final String... propertyIDs) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public final String getTableDataAsDelimitedString(final char delimiter) {
    final List<String> headerValues = new ArrayList<>();
    final List<Property> properties = new ArrayList<>();
    columns.forEach(entityTableColumn -> {
      final Property property = ((PropertyColumn) entityTableColumn).getProperty();
      properties.add(property);
      headerValues.add(property.getCaption());
    });

    final String[][] header = {headerValues.toArray(new String[headerValues.size()])};

    return Util.getDelimitedString(header, EntityUtil.getStringValueArray(properties,
            getSelectionModel().isSelectionEmpty() ? getVisibleItems() : getSelectionModel().getSelectedItems()),
            String.valueOf(delimiter));
  }

  protected List<Entity> queryContents() {
    final Criteria<Property.ColumnProperty> criteria = criteriaModel.getTableCriteria();
    if (criteria == null && queryCriteriaRequired) {
      return new ArrayList<>();
    }

    try {
      return getConnectionProvider().getConnection().selectMany(EntityCriteriaUtil.selectCriteria(getEntityID(), criteria,
              getOrderByClause(), fetchCount));
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

  protected void bindSelectionModelEvents() {
    super.bindSelectionModelEvents();
    getSelectionModel().addSelectedIndexListener(() -> {
      if (editModel != null) {
        editModel.setEntity(getSelectionModel().getSelectedItem());
      }
    });
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
    addRefreshListener(criteriaModel::rememberCurrentCriteriaState);
  }

  public static class PropertyColumn extends TableColumn<Entity, Object> {

    private final Property property;

    protected PropertyColumn(final Property property) {
      super(property.getCaption());
      this.property = property;
    }

    public Property getProperty() {
      return property;
    }
  }
}
