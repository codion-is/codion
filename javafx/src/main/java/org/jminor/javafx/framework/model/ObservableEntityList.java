/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ObservableEntityList implements ObservableList<Entity> {

  private final EntityConnectionProvider connectionProvider;
  private final ObservableList<Entity> list = FXCollections.observableArrayList();
  private final String entityID;

  public ObservableEntityList(final String entityID, final EntityConnectionProvider connectionProvider) {
    this.entityID = entityID;
    this.connectionProvider = connectionProvider;
  }

  public final void refresh() throws DatabaseException {
    final List<Entity> entities = connectionProvider.getConnection().selectMany(getSelectCriteria());
    clear();
    addAll(entities);
  }

  public final Callback<TableColumn.CellDataFeatures<Entity, Object>, ObservableValue<Object>> getCellValueFactory(final Property property) {
    return new Callback<TableColumn.CellDataFeatures<Entity, Object>, ObservableValue<Object>>() {
      @Override
      public ObservableValue<Object> call(final TableColumn.CellDataFeatures<Entity, Object> row) {
        return new ReadOnlyObjectWrapper<Object>(row.getValue().getValue(property.getPropertyID()));
      }
    };
  }

  public final String getEntityID() {
    return entityID;
  }

  @Override
  public final Entity get(final int index) {
    return list.get(index);
  }

  protected EntitySelectCriteria getSelectCriteria() {
    return EntityCriteriaUtil.selectCriteria(entityID);
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
    return removeAll(c);
  }

  @Override
  public final boolean retainAll(final Collection<?> c) {
    return retainAll(c);
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
}
