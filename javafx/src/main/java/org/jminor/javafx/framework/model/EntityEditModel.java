/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Value;
import org.jminor.common.model.valuemap.DefaultValueMapEditModel;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;

import javafx.collections.ObservableList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class EntityEditModel extends DefaultValueMapEditModel<String, Object> {

  private final EntityConnectionProvider connectionProvider;

  private final Event<List<Entity>> insertEvent = Events.event();
  private final Event<List<Entity>> updateEvent = Events.event();
  private final Event<List<Entity>> deleteEvent = Events.event();

  private final Event<Entity> entitySetEvent = Events.event();
  private final State entityNewState = States.state(true);

  public EntityEditModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this(entityID, connectionProvider, Entities.getValidator(entityID));
  }

  public EntityEditModel(final String entityID, final EntityConnectionProvider connectionProvider,
                         final Entity.Validator validator) {
    super(Entities.entity(entityID), validator);
    this.connectionProvider = connectionProvider;
    bindEvents();
  }

  public final String getEntityID() {
    return getEntity().getEntityID();
  }

  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  public final StateObserver getModifiedObserver() {
    return getEntity().getModifiedObserver();
  }

  public final StateObserver getEntityNewObserver() {
    return entityNewState.getObserver();
  }

  public final void setEntity(final Entity entity) {
    this.getEntity().setAs(entity == null ? getDefaultEntity() : entity);
    entitySetEvent.fire(entity);
  }

  public final Entity getEntityCopy(final boolean includeKeyValues) {
    final Entity copy = (Entity) getEntity().getCopy();
    if (!includeKeyValues) {
      copy.clearKeyValues();
    }

    return copy;
  }

  public final void clear() {
    setEntity(null);
  }

  public final List<Entity> insert() throws DatabaseException, ValidationException {
    final boolean includePrimaryKeyValues = !Entities.isPrimaryKeyAutoGenerated(getEntityID());
    final List<Entity> insertedEntities = connectionProvider.getConnection().selectMany(
            doInsert(Collections.singletonList(getEntityCopy(includePrimaryKeyValues))));
    if (insertedEntities.isEmpty()) {
      throw new RuntimeException("Insert did not return an entity, usually caused by a misconfigured key generator");
    }
    setEntity(insertedEntities.get(0));
    insertEvent.fire(insertedEntities);

    return insertedEntities;
  }

  public final List<Entity> insert(final List<Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    if (entities.isEmpty()) {
      return Collections.emptyList();
    }
    final List<Entity> insertedEntities = connectionProvider.getConnection().selectMany(doInsert(entities));
    insertEvent.fire(insertedEntities);

    return insertedEntities;
  }

  public final Entity update() throws DatabaseException {
    final List<Entity> updated = update(Collections.singletonList(getEntity()));
    setEntity(updated.get(0));

    return updated.get(0);
  }

  public final List<Entity> update(final List<Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    final List<Entity> modified = EntityUtil.getModifiedEntities(entities);
    if (!modified.isEmpty()) {
      final List<Entity> updated = doUpdate(modified);
      updateEvent.fire(updated);

      return updated;
    }

    return entities;
  }

  public final void delete() throws DatabaseException {
    delete(Collections.singletonList(getEntity()));
  }

  public final void delete(final List<Entity> entities) throws DatabaseException {
    Objects.requireNonNull(entities);
    if (!entities.isEmpty()) {
      doDelete(entities);
      deleteEvent.fire(entities);
    }
  }

  public ObservableList<Entity> createForeignKeyList(final String propertyID) {
    return new ObservableEntityList(Entities.getForeignKeyProperty(getEntity().getEntityID(),
            propertyID).getReferencedEntityID(), connectionProvider);
  }

  public final Value createValue(final String propertyID) {
    return new EntityValue<>(propertyID, this);
  }

  public boolean isEntityNew() {
    return EntityUtil.isEntityNew(getEntity());
  }

  public final void addEntitySetListener(final EventInfoListener<Entity> listener) {
    entitySetEvent.addInfoListener(listener);
  }

  public final void addInsertListener(final EventInfoListener<List<Entity>> listener) {
    insertEvent.addInfoListener(listener);
  }

  public final void addUpdateListener(final EventInfoListener<List<Entity>> listener) {
    updateEvent.addInfoListener(listener);
  }

  public final void addDeleteListener(final EventInfoListener<List<Entity>> listener) {
    deleteEvent.addInfoListener(listener);
  }

  protected Entity getDefaultEntity() {
    //todo
    final Entity entity = Entities.entity(getEntityID());
    entity.saveAll();

    return entity;
  }

  protected List<Entity.Key> doInsert(final List<Entity> entities) throws DatabaseException {
    return connectionProvider.getConnection().insert(entities);
  }

  protected List<Entity> doUpdate(final List<Entity> entities) throws DatabaseException {
    return connectionProvider.getConnection().update(entities);
  }

  protected void doDelete(final List<Entity> entities) throws DatabaseException {
    connectionProvider.getConnection().delete(EntityUtil.getKeys(entities));
  }

  private Entity getEntity() {
    return (Entity) getValueMap();
  }

  private void bindEvents() {
    getValueObserver().addInfoListener(valueChange -> entityNewState.setActive(isEntityNew()));
    entitySetEvent.addInfoListener(activeEntity -> entityNewState.setActive(isEntityNew()));
    entityNewState.addInfoListener(active -> System.out.println("Entity new: " + active));
    getValueObserver().addInfoListener(valueChange -> System.out.println(valueChange + " | modified: " + getEntity().isModified()));
  }

  private static final class EntityValue<V> implements Value<V> {

    private final String propertyID;
    private final EntityEditModel editModel;
    private final Event<V> valueChangeEvent = Events.event();

    private EntityValue(final String propertyID, final EntityEditModel editModel) {
      this.propertyID = propertyID;
      this.editModel = editModel;
      this.editModel.addValueListener(propertyID, valueChange -> valueChangeEvent.fire((V) valueChange.getNewValue()));
    }

    @Override
    public void set(final V value) {
      editModel.setValue(propertyID, value);
    }

    @Override
    public V get() {
      return (V) editModel.getValue(propertyID);
    }

    @Override
    public EventObserver<V> getObserver() {
      return valueChangeEvent.getObserver();
    }
  }
}
