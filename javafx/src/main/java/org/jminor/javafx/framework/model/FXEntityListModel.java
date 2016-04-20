/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.table.ColumnSummaryModel;
import org.jminor.common.model.table.SelectionModel;
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

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

public class FXEntityListModel implements EntityTableModel, ObservableList<Entity> {

  private final String entityID;
  private final EntityConnectionProvider connectionProvider;
  private final ObservableList<Entity> list = FXCollections.observableArrayList();
  private final SortedList<Entity> sortedList;
  private final FilteredList<Entity> filteredList;
  private final EntityTableCriteriaModel criteriaModel;

  private final Event refreshEvent = Events.event();
  private final Event selectionChangedEvent = Events.event();

  private FXEntityListSelectionModel selectionModel;

  private FXEntityEditModel editModel;

  private InsertAction insertAction = InsertAction.ADD_TOP;
  private boolean queryCriteriaRequired = false;
  private boolean queryConfigurationAllowed = true;
  private boolean batchUpdateAllowed = true;
  private boolean removeEntitiesOnDelete = true;
  private int fetchCount = -1;

  public FXEntityListModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this(entityID, connectionProvider, new DefaultEntityTableCriteriaModel(entityID, connectionProvider,
            null, new FXCriteriaModelProvider(true)));
  }

  public FXEntityListModel(final String entityID, final EntityConnectionProvider connectionProvider,
                           final EntityTableCriteriaModel criteriaModel) {
    this.entityID = entityID;
    this.connectionProvider = connectionProvider;
    this.criteriaModel = criteriaModel;
    this.filteredList = new FilteredList<>(list);
    this.sortedList = new SortedList<>(filteredList);
  }

  public EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  public final void setEditModel(final org.jminor.framework.model.EntityEditModel editModel) {
    if (this.editModel != null) {
      throw new IllegalStateException("Edit model has already been set");
    }
    this.editModel = (FXEntityEditModel) Objects.requireNonNull(editModel);
    bindEditModelEvents();
  }

  public final void setSelectionModel(final javafx.scene.control.SelectionModel<Entity> selectionModel) {
    if (this.selectionModel != null) {
      throw new IllegalStateException("Selection model has already been set");
    }
    this.selectionModel = new FXEntityListSelectionModel(Objects.requireNonNull(selectionModel));
    bindSelectionModelEvents();
  }

  public final SelectionModel<Entity> getSelectionModel() {
    if (selectionModel == null) {
      throw new IllegalStateException("Selection model has not been set");
    }
    return selectionModel;
  }

  public final javafx.scene.control.SelectionModel<Entity> getListSelectionModel() {
    return selectionModel.getSelectionModel();
  }

  public final SortedList<Entity> getSortedList() {
    return sortedList;
  }

  public final FilteredList<Entity> getFilteredList() {
    return filteredList;
  }

  public final FXEntityEditModel getEditModel() {
    if (editModel == null) {
      throw new IllegalStateException("No edit model has been set for list: " + this);
    }
    return editModel;
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

  public final void refresh() {
    final List<Entity> queryResult = performQuery(criteriaModel.getTableCriteria());
    setAll(queryResult);
    criteriaModel.rememberCurrentCriteriaState();
    refreshEvent.fire();
  }

  public final void addRefreshListener(final EventListener listener) {
    refreshEvent.addListener(listener);
  }

  public final StateObserver getSelectionEmptyObserver() {
    if (selectionModel == null) {
      throw new IllegalStateException("No selection model has been set");
    }
    return selectionModel.getSelectionEmptyObserver();
  }

  public final StateObserver getSingleSelectionObserver() {
    if (selectionModel == null) {
      throw new IllegalStateException("No selection model has been set");
    }
    return selectionModel.getSingleSelectionObserver();
  }

  public final StateObserver getMultipleSelectionObserver() {
    if (selectionModel == null) {
      throw new IllegalStateException("No selection model has been set");
    }
    return selectionModel.getMultipleSelectionObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final void addSelectionChangedListener(final EventListener listener) {
    selectionChangedEvent.addListener(listener);
  }

  public final void setForeignKeyCriteriaValues(final Property.ForeignKeyProperty foreignKeyProperty, final Collection<Entity> entities) {
    if (criteriaModel.setCriteriaValues(foreignKeyProperty.getPropertyID(), entities)) {
      refresh();
    }
  }

  public final Callback<TableColumn.CellDataFeatures<Entity, Object>, ObservableValue<Object>> getCellValueFactory(final Property property) {
    return row -> new ReadOnlyObjectWrapper<>(row.getValue().get(property.getPropertyID()));
  }

  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  @Override
  public final Entity get(final int index) {
    return list.get(index);
  }

  public final void deleteSelected() throws DatabaseException {
    getEditModel().delete(getSelectionModel().getSelectedItems());
  }

  /** {@inheritDoc} */
  @Override
  public final int size() {
    return list.size();
  }

  /** {@inheritDoc} */
  @Override
  public final void addListener(final ListChangeListener<? super Entity> listener) {
    list.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeListener(final ListChangeListener<? super Entity> listener) {
    list.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean addAll(final Entity... elements) {
    return list.addAll(elements);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean setAll(final Entity... elements) {
    return list.setAll(elements);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean setAll(final Collection<? extends Entity> col) {
    return list.setAll(col);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean removeAll(final Entity... elements) {
    return list.removeAll();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean retainAll(final Entity... elements) {
    return list.retainAll(elements);
  }

  /** {@inheritDoc} */
  @Override
  public final void remove(final int from, final int to) {
    list.remove(from, to);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isEmpty() {
    return list.isEmpty();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean contains(final Object o) {
    return list.contains(o);
  }

  /** {@inheritDoc} */
  @Override
  public final Iterator<Entity> iterator() {
    return list.iterator();
  }

  /** {@inheritDoc} */
  @Override
  public final Object[] toArray() {
    return list.toArray();
  }

  /** {@inheritDoc} */
  @Override
  public final <T> T[] toArray(final T[] a) {
    return list.toArray(a);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean add(final Entity entity) {
    return list.add(entity);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean remove(final Object o) {
    return list.remove(o);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsAll(final Collection<?> c) {
    return list.containsAll(c);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean addAll(final Collection<? extends Entity> c) {
    return list.addAll(c);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean addAll(final int index, final Collection<? extends Entity> c) {
    return list.addAll(index, c);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean removeAll(final Collection<?> c) {
    return list.removeAll(c);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean retainAll(final Collection<?> c) {
    return list.retainAll(c);
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    list.clear();
  }

  /** {@inheritDoc} */
  @Override
  public final Entity set(final int index, final Entity element) {
    return list.set(index, element);
  }

  /** {@inheritDoc} */
  @Override
  public final void add(final int index, final Entity element) {
    list.add(index, element);
  }

  /** {@inheritDoc} */
  @Override
  public final Entity remove(final int index) {
    return list.remove(index);
  }

  /** {@inheritDoc} */
  @Override
  public final int indexOf(final Object o) {
    return list.indexOf(o);
  }

  /** {@inheritDoc} */
  @Override
  public final int lastIndexOf(final Object o) {
    return list.lastIndexOf(o);
  }

  /** {@inheritDoc} */
  @Override
  public final ListIterator<Entity> listIterator() {
    return list.listIterator();
  }

  /** {@inheritDoc} */
  @Override
  public final ListIterator<Entity> listIterator(final int index) {
    return list.listIterator(index);
  }

  /** {@inheritDoc} */
  @Override
  public final List<Entity> subList(final int fromIndex, final int toIndex) {
    return list.subList(fromIndex, toIndex);
  }

  /** {@inheritDoc} */
  @Override
  public final void addListener(final InvalidationListener listener) {
    list.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeListener(final InvalidationListener listener) {
    list.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public List<Entity> getAllItems() {
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public int getRowCount() {
    return size();
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasEditModel() {
    return editModel != null;
  }

  /** {@inheritDoc} */
  @Override
  public void replaceForeignKeyValues(final String foreignKeyEntityID, final Collection<Entity> foreignKeyValues) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = Entities.getForeignKeyProperties(this.entityID, foreignKeyEntityID);
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
  public void addEntities(final List<Entity> entities, final boolean atTop) {
    if (atTop) {
      addAll(0, entities);
    }
    else {
      addAll(entities);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void replaceEntities(final Collection<Entity> entities) {
    replaceEntitiesByKey(EntityUtil.mapToKey(entities));
  }

  /** {@inheritDoc} */
  @Override
  public boolean isQueryConfigurationAllowed() {
    return queryConfigurationAllowed;
  }

  /** {@inheritDoc} */
  @Override
  public EntityTableModel setQueryConfigurationAllowed(final boolean queryConfigurationAllowed) {
    this.queryConfigurationAllowed = queryConfigurationAllowed;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isDeleteAllowed() {
    return editModel != null && editModel.isDeleteAllowed();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isReadOnly() {
    return editModel == null || editModel.isReadOnly();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isUpdateAllowed() {
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
  public ColumnSummaryModel getColumnSummaryModel(final String propertyID) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public Color getPropertyBackgroundColor(final int row, final Property property) {
    return (Color) get(row).getBackgroundColor(property);
  }

  /** {@inheritDoc} */
  @Override
  public int getPropertyColumnIndex(final String propertyID) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public String getStatusMessage() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public int getFetchCount() {
    return fetchCount;
  }

  /** {@inheritDoc} */
  @Override
  public EntityTableModel setFetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public void update(final List<Entity> entities) throws ValidationException, DatabaseException {
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
  public boolean isRemoveEntitiesOnDelete() {
    return removeEntitiesOnDelete;
  }

  /** {@inheritDoc} */
  @Override
  public EntityTableModel setRemoveEntitiesOnDelete(final boolean removeEntitiesOnDelete) {
    this.removeEntitiesOnDelete = removeEntitiesOnDelete;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public InsertAction getInsertAction() {
    return insertAction;
  }

  /** {@inheritDoc} */
  @Override
  public EntityTableModel setInsertAction(final InsertAction insertAction) {
    Objects.requireNonNull(insertAction);
    this.insertAction = insertAction;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Collection<Entity> getEntitiesByKey(final Collection<Entity.Key> keys) {
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public void setSelectedByKey(final Collection<Entity.Key> keys) {
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
  public Entity getEntityByKey(final Entity.Key primaryKey) {
    for (final Entity entity : filteredList) {
      if (entity.getKey().equals(primaryKey)) {
        return entity;
      }
    }

    return null;

  }

  /** {@inheritDoc} */
  @Override
  public Iterator<Entity> getSelectedEntitiesIterator() {
    return selectionModel.getSelectedItems().iterator();
  }

  /** {@inheritDoc} */
  @Override
  public int indexOf(final Entity.Key primaryKey) {
    return indexOf(getEntityByKey(primaryKey));
  }

  /** {@inheritDoc} */
  @Override
  public void savePreferences() {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public void setColumns(final String... propertyIDs) {
    throw new UnsupportedOperationException();
  }

  /** {@inheritDoc} */
  @Override
  public String getTableDataAsDelimitedString(final char delimiter) {
    throw new UnsupportedOperationException();
  }

  /**
   * Queries for the data used to populate this EntityTableModel when it is refreshed,
   * @param criteria a criteria
   * @return entities selected from the database according the the query criteria.
   * @see EntityTableCriteriaModel#getTableCriteria()
   */
  protected List<Entity> performQuery(final Criteria<Property.ColumnProperty> criteria) {
    if (criteria == null && queryCriteriaRequired) {
      return new ArrayList<>();
    }

    try {
      return connectionProvider.getConnection().selectMany(EntityCriteriaUtil.selectCriteria(entityID, criteria,
              Entities.getOrderByClause(entityID), fetchCount));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private void bindEditModelEvents() {
    getEditModel().addAfterInsertListener(this::handleInsert);
    getEditModel().addAfterUpdateListener(this::handleUpdate);
    getEditModel().addAfterDeleteListener(this::handleDelete);
    getEditModel().addAfterRefreshListener(this::refresh);
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
    list.replaceAll(entity -> {
      final Entity toReplaceWith = entityMap.get(entity.getKey());
      return toReplaceWith == null ? entity : toReplaceWith;
    });
  }

  private void bindSelectionModelEvents() {
    selectionModel.addSelectionChangedListener(selectionChangedEvent);
    selectionModel.addSelectedIndexListener(() -> {
      if (editModel != null) {
        editModel.setEntity(selectionModel.getSelectedItem());
      }
    });
  }
}
