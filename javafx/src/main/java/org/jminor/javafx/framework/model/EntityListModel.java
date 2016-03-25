/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.db.criteria.CriteriaUtil;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableSelectionModel;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

public class EntityListModel implements ObservableList<Entity> {

  private final String entityID;
  private final EntityConnectionProvider connectionProvider;
  private final ObservableList<Entity> list = FXCollections.observableArrayList();
  private final List<Criteria<Property.ColumnProperty>> propertyCriteria = new ArrayList<>();

  private final Event<TableSelectionModel<Entity>> selectionModelSetEvent = Events.event();
  private final State selectionEmptyState = States.state(true);
  private final State singleSelectionState = States.state();
  private final State multipleSelectionState = States.state();

  private TableSelectionModel<Entity> selectionModel;

  private EntityEditModel editModel;

  private EntitySelectCriteria selectCriteria;

  public EntityListModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this.entityID = entityID;
    this.connectionProvider = connectionProvider;
  }

  public EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  public final void setEditModel(final EntityEditModel editModel) {
    if (this.editModel != null) {
      throw new IllegalStateException("Edit model has already been set");
    }
    Objects.requireNonNull(editModel);
    this.editModel = editModel;
    bindEditModelEvents();
  }

  public final void setSelectionModel(final TableSelectionModel<Entity> selectionModel) {
    if (this.selectionModel != null) {
      throw new IllegalStateException("Selection model has already been set");
    }
    this.selectionModel = selectionModel;
    this.selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
    this.selectionEmptyState.setActive(this.selectionModel.isEmpty());
    this.singleSelectionState.setActive(this.selectionModel.getSelectedIndices().size() == 1);
    this.multipleSelectionState.setActive(!selectionEmptyState.isActive() && !singleSelectionState.isActive());
    selectionModelSetEvent.fire(selectionModel);
    bindSelectionModelEvents();
  }

  public TableSelectionModel<Entity> getSelectionModel() {
    if (selectionModel == null) {
      throw new IllegalStateException("Selection model has not been set");
    }
    return selectionModel;
  }

  public void addSelectionModelSetListener(final EventInfoListener<TableSelectionModel<Entity>> listener) {
    selectionModelSetEvent.addInfoListener(listener);
  }

  public final EntityEditModel getEditModel() {
    if (editModel == null) {
      throw new IllegalStateException("No edit model has been set for list: " + this);
    }
    return editModel;
  }

  public final void refresh() {
    try {
      setAll(connectionProvider.getConnection().selectMany(getSelectCriteria()));
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  public StateObserver getSelectionEmptyObserver() {
    if (selectionModel == null) {
      throw new IllegalStateException("No selection model has been set");
    }
    return selectionEmptyState.getObserver();
  }

  public StateObserver getSingleSelectionObserver() {
    if (selectionModel == null) {
      throw new IllegalStateException("No selection model has been set");
    }
    return singleSelectionState.getObserver();
  }

  public StateObserver getMultipleSelectionObserver() {
    if (selectionModel == null) {
      throw new IllegalStateException("No selection model has been set");
    }
    return multipleSelectionState.getObserver();
  }

  public void filterBy(final Property.ForeignKeyProperty foreignKeyProperty, final List<Entity> entities) throws DatabaseException {
    Objects.requireNonNull(foreignKeyProperty);
    Objects.requireNonNull(entities);
    propertyCriteria.clear();
    if (!entities.isEmpty()) {
      propertyCriteria.add(EntityCriteriaUtil.foreignKeyCriteria(foreignKeyProperty, SearchType.LIKE, entities));
    }
    refresh();
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

  protected EntitySelectCriteria getSelectCriteria() {
    if (propertyCriteria.isEmpty()) {
      return EntityCriteriaUtil.selectCriteria(entityID).setOrderByClause(Entities.getOrderByClause(entityID));
    }

    final CriteriaSet<Property.ColumnProperty> criteriaSet = CriteriaUtil.criteriaSet(
            Conjunction.AND, propertyCriteria);

    return EntityCriteriaUtil.selectCriteria(entityID, criteriaSet).setOrderByClause(Entities.getOrderByClause(entityID));
  }

  private void bindEditModelEvents() {
    editModel.addInsertListener(insertEvent -> addAll(insertEvent.getInsertedEntities()));
    editModel.addUpdateListener(updateEvent -> replaceEntitiesByKey(new HashMap<>(updateEvent.getUpdatedEntities())));
    editModel.addDeleteListener(deleteEvent -> removeAll(deleteEvent.getDeletedEntities()));
    editModel.addRefreshListener(this::refresh);
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
    selectionModel.getSelectedItems().addListener((ListChangeListener<Entity>) change -> {
      selectionEmptyState.setActive(selectionModel.isEmpty());
      singleSelectionState.setActive(this.selectionModel.getSelectedIndices().size() == 1);
      multipleSelectionState.setActive(!selectionEmptyState.isActive() && !singleSelectionState.isActive());
      final List<Entity> selected = selectionModel.getSelectedItems();
      if (selected.isEmpty()) {
        editModel.setEntity(null);
      }
      else {
        editModel.setEntity(selected.get(0));
      }
    });
  }
}
