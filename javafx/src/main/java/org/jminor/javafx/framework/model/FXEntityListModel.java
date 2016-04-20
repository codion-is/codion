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
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultEntityTableCriteriaModel;
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

public class FXEntityListModel implements org.jminor.framework.model.EntityTableModel, ObservableList<Entity> {

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

  /**
   * If true then querying should be disabled if no criteria is specified
   */
  private boolean queryCriteriaRequired = false;

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

  public SortedList<Entity> getSortedList() {
    return sortedList;
  }

  public FilteredList<Entity> getFilteredList() {
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

  public void addRefreshListener(final EventListener listener) {
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

  @Override
  public void addSelectionChangedListener(final EventListener listener) {
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

  @Override
  public final Entity get(final int index) {
    return list.get(index);
  }

  public final void deleteSelected() throws DatabaseException {
    getEditModel().delete(getSelectionModel().getSelectedItems());
  }

  @Override
  public final int size() {
    return list.size();
  }

  @Override
  public final void addListener(final ListChangeListener<? super Entity> listener) {
    list.addListener(listener);
  }

  @Override
  public final void removeListener(final ListChangeListener<? super Entity> listener) {
    list.removeListener(listener);
  }

  @Override
  public final boolean addAll(final Entity... elements) {
    return list.addAll(elements);
  }

  @Override
  public final boolean setAll(final Entity... elements) {
    return list.setAll(elements);
  }

  @Override
  public final boolean setAll(final Collection<? extends Entity> col) {
    return list.setAll(col);
  }

  @Override
  public final boolean removeAll(final Entity... elements) {
    return list.removeAll();
  }

  @Override
  public final boolean retainAll(final Entity... elements) {
    return list.retainAll(elements);
  }

  @Override
  public final void remove(final int from, final int to) {
    list.remove(from, to);
  }

  @Override
  public final boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public final boolean contains(final Object o) {
    return list.contains(o);
  }

  @Override
  public final Iterator<Entity> iterator() {
    return list.iterator();
  }

  @Override
  public final Object[] toArray() {
    return list.toArray();
  }

  @Override
  public final <T> T[] toArray(final T[] a) {
    return list.toArray(a);
  }

  @Override
  public final boolean add(final Entity entity) {
    return list.add(entity);
  }

  @Override
  public final boolean remove(final Object o) {
    return list.remove(o);
  }

  @Override
  public final boolean containsAll(final Collection<?> c) {
    return list.containsAll(c);
  }

  @Override
  public final boolean addAll(final Collection<? extends Entity> c) {
    return list.addAll(c);
  }

  @Override
  public final boolean addAll(final int index, final Collection<? extends Entity> c) {
    return list.addAll(index, c);
  }

  @Override
  public final boolean removeAll(final Collection<?> c) {
    return list.removeAll(c);
  }

  @Override
  public final boolean retainAll(final Collection<?> c) {
    return list.retainAll(c);
  }

  @Override
  public final void clear() {
    list.clear();
  }

  @Override
  public final Entity set(final int index, final Entity element) {
    return list.set(index, element);
  }

  @Override
  public final void add(final int index, final Entity element) {
    list.add(index, element);
  }

  @Override
  public final Entity remove(final int index) {
    return list.remove(index);
  }

  @Override
  public final int indexOf(final Object o) {
    return list.indexOf(o);
  }

  @Override
  public final int lastIndexOf(final Object o) {
    return list.lastIndexOf(o);
  }

  @Override
  public final ListIterator<Entity> listIterator() {
    return list.listIterator();
  }

  @Override
  public final ListIterator<Entity> listIterator(final int index) {
    return list.listIterator(index);
  }

  @Override
  public final List<Entity> subList(final int fromIndex, final int toIndex) {
    return list.subList(fromIndex, toIndex);
  }

  @Override
  public final void addListener(final InvalidationListener listener) {
    list.addListener(listener);
  }

  @Override
  public final void removeListener(final InvalidationListener listener) {
    list.removeListener(listener);
  }

  @Override
  public List<Entity> getAllItems() {
    return this;
  }

  @Override
  public int getRowCount() {
    return size();
  }

  @Override
  public boolean hasEditModel() {
    return editModel != null;
  }

  @Override
  public void replaceForeignKeyValues(final String foreignKeyEntityID, final Collection<Entity> foreignKeyValues) {

  }

  @Override
  public void addEntities(final List<Entity> entities, final boolean atTop) {

  }

  @Override
  public void replaceEntities(final Collection<Entity> entities) {

  }

  @Override
  public boolean isQueryConfigurationAllowed() {
    return false;
  }

  @Override
  public EntityTableModel setQueryConfigurationAllowed(final boolean value) {
    return null;
  }

  @Override
  public boolean isDeleteAllowed() {
    return false;
  }

  @Override
  public boolean isReadOnly() {
    return false;
  }

  @Override
  public boolean isUpdateAllowed() {
    return false;
  }

  @Override
  public boolean isBatchUpdateAllowed() {
    return false;
  }

  @Override
  public EntityTableModel setBatchUpdateAllowed(final boolean batchUpdateAllowed) {
    return null;
  }

  @Override
  public ColumnSummaryModel getColumnSummaryModel(final String propertyID) {
    return null;
  }

  @Override
  public Color getPropertyBackgroundColor(final int row, final Property columnProperty) {
    return null;
  }

  @Override
  public int getPropertyColumnIndex(final String propertyID) {
    return 0;
  }

  @Override
  public String getStatusMessage() {
    return null;
  }

  @Override
  public int getFetchCount() {
    return 0;
  }

  @Override
  public EntityTableModel setFetchCount(final int fetchCount) {
    return null;
  }

  @Override
  public void update(final List<Entity> entities) throws ValidationException, DatabaseException {

  }

  @Override
  public boolean isRemoveEntitiesOnDelete() {
    return false;
  }

  @Override
  public EntityTableModel setRemoveEntitiesOnDelete(final boolean value) {
    return null;
  }

  @Override
  public InsertAction getInsertAction() {
    return null;
  }

  @Override
  public EntityTableModel setInsertAction(final InsertAction insertAction) {
    return null;
  }

  @Override
  public Collection<Entity> getEntitiesByKey(final Collection<Entity.Key> keys) {
    return null;
  }

  @Override
  public void setSelectedByKey(final Collection<Entity.Key> keys) {

  }

  @Override
  public Collection<Entity> getEntitiesByPropertyValue(final Map<String, Object> values) {
    return null;
  }

  @Override
  public Iterator<Entity> getSelectedEntitiesIterator() {
    return null;
  }

  @Override
  public Entity getEntityByKey(final Entity.Key primaryKey) {
    return null;
  }

  @Override
  public int indexOf(final Entity.Key primaryKey) {
    return 0;
  }

  @Override
  public void savePreferences() {

  }

  @Override
  public void setColumns(final String... propertyIDs) {

  }

  @Override
  public String getTableDataAsDelimitedString(final char delimiter) {
    return null;
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
              Entities.getOrderByClause(entityID)));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private void bindEditModelEvents() {
    getEditModel().addAfterInsertListener(insertEvent -> addAll(0, insertEvent.getInsertedEntities()));
    getEditModel().addAfterUpdateListener(updateEvent -> replaceEntitiesByKey(new HashMap<>(updateEvent.getUpdatedEntities())));
    getEditModel().addAfterDeleteListener(deleteEvent -> removeAll(deleteEvent.getDeletedEntities()));
    getEditModel().addAfterRefreshListener(this::refresh);
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
