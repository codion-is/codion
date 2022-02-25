/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * A central application model class.
 * @param <M> the type of {@link DefaultEntityModel} this application model is based on
 * @param <E> the type of {@link DefaultEntityEditModel} used by this {@link EntityModel}
 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
 */
public class DefaultEntityApplicationModel<M extends DefaultEntityModel<M, E, T>,
        E extends DefaultEntityEditModel, T extends EntityTableModel<E>> implements EntityApplicationModel<M, E, T> {

  private static final int VALIDITY_CHECK_INTERVAL_SECONDS = 30;

  private final EntityConnectionProvider connectionProvider;
  private final State connectionValidState = State.state();
  private final Event<User> loginEvent = Event.event();
  private final Event<User> logoutEvent = Event.event();
  private final TaskScheduler validityCheckScheduler = TaskScheduler.builder(this::checkConnectionValidity)
          .interval(VALIDITY_CHECK_INTERVAL_SECONDS)
          .initialDelay(VALIDITY_CHECK_INTERVAL_SECONDS)
          .timeUnit(TimeUnit.SECONDS)
          .build();
  private final List<M> entityModels = new ArrayList<>();

  private boolean warnAboutUnsavedData = EntityEditModel.WARN_ABOUT_UNSAVED_DATA.get();

  /**
   * Instantiates a new DefaultEntityApplicationModel
   * @param connectionProvider the EntityConnectionProvider instance
   * @throws NullPointerException in case connectionProvider is null
   */
  public DefaultEntityApplicationModel(final EntityConnectionProvider connectionProvider) {
    requireNonNull(connectionProvider, "connectionProvider");
    this.connectionProvider = connectionProvider;
    if (SCHEDULE_CONNECTION_VALIDATION.get()) {
      validityCheckScheduler.start();
      connectionProvider.addOnConnectListener(connection -> {
        connectionValidState.set(true);
        validityCheckScheduler.start();
      });
    }
  }

  @Override
  public final void login(final User user) {
    requireNonNull(user, "user");
    connectionProvider.setUser(user);
    refresh();
    loginEvent.onEvent(user);
  }

  @Override
  public final void logout() {
    User user = connectionProvider.getUser();
    connectionProvider.setUser(null);
    clear();
    logoutEvent.onEvent(user);
  }

  @Override
  public final User getUser() {
    return connectionProvider.getConnection().getUser();
  }

  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  @Override
  public final StateObserver getConnectionValidObserver() {
    return connectionValidState.getObserver();
  }

  @Override
  public final Entities getEntities() {
    return connectionProvider.getEntities();
  }

  @Override
  @SafeVarargs
  public final void addEntityModels(final M... entityModels) {
    requireNonNull(entityModels, "entityModels");
    for (final M entityModel : entityModels) {
      addEntityModel(entityModel);
    }
  }

  @Override
  public final M addEntityModel(final M detailModel) {
    this.entityModels.add(detailModel);

    return detailModel;
  }

  @Override
  public final boolean containsEntityModel(final Class<? extends M> modelClass) {
    return entityModels.stream()
            .anyMatch(entityModel -> entityModel.getClass().equals(modelClass));
  }

  @Override
  public final boolean containsEntityModel(final EntityType entityType) {
    return entityModels.stream()
            .anyMatch(entityModel -> entityModel.getEntityType().equals(entityType));
  }

  @Override
  public final boolean containsEntityModel(final M entityModel) {
    return entityModels.contains(entityModel);
  }

  @Override
  public final List<M> getEntityModels() {
    return Collections.unmodifiableList(entityModels);
  }

  @Override
  public final void refresh() {
    for (final M entityModel : entityModels) {
      if (entityModel.containsTableModel()) {
        entityModel.getTableModel().refresh();
      }
    }
  }

  @Override
  public final void clear() {
    for (final M entityModel : entityModels) {
      entityModel.clear();
    }
  }

  @Override
  public final <T extends M> T getEntityModel(final Class<? extends M> modelClass) {
    for (final M model : entityModels) {
      if (model.getClass().equals(modelClass)) {
        return (T) model;
      }
    }

    throw new IllegalArgumentException("EntityModel of type: " + modelClass + " not found");
  }

  @Override
  public final M getEntityModel(final EntityType entityType) {
    for (final M entityModel : entityModels) {
      if (entityModel.getEntityType().equals(entityType)) {
        return entityModel;
      }
    }

    throw new IllegalArgumentException("EntityModel for type " + entityType + " not  found in model: " + this);
  }

  @Override
  public final boolean isWarnAboutUnsavedData() {
    return warnAboutUnsavedData;
  }

  @Override
  public final void setWarnAboutUnsavedData(final boolean warnAboutUnsavedData) {
    this.warnAboutUnsavedData = warnAboutUnsavedData;
  }

  @Override
  public final boolean containsUnsavedData() {
    return containsUnsavedData(entityModels);
  }

  @Override
  public void savePreferences() {
    getEntityModels().forEach(EntityModel::savePreferences);
  }

  @Override
  public final void addLoginListener(final EventDataListener<User> listener) {
    loginEvent.addDataListener(listener);
  }

  @Override
  public final void removeLoginListener(final EventDataListener<User> listener) {
    loginEvent.removeDataListener(listener);
  }

  @Override
  public final void addLogoutListener(final EventDataListener<User> listener) {
    logoutEvent.addDataListener(listener);
  }

  @Override
  public final void removeLogoutListener(final EventDataListener<User> listener) {
    logoutEvent.removeDataListener(listener);
  }

  private void checkConnectionValidity() {
    connectionValidState.set(connectionProvider.isConnectionValid());
    if (!connectionValidState.get()) {
      validityCheckScheduler.stop();
    }
  }

  private static boolean containsUnsavedData(final Collection<? extends EntityModel<?, ?, ?>> models) {
    for (final EntityModel<?, ?, ?> model : models) {
      EntityEditModel editModel = model.getEditModel();
      if (editModel.containsUnsavedData() || containsUnsavedData(model.getDetailModels())) {
        return true;
      }
    }

    return false;
  }
}
