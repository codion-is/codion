/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.TaskScheduler;
import org.jminor.common.state.State;
import org.jminor.common.state.StateObserver;
import org.jminor.common.state.States;
import org.jminor.common.user.User;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * A central application model class.
 * @param <M> the {@link DefaultEntityModel} type this application model is based on
 */
public class DefaultEntityApplicationModel<M extends DefaultEntityModel> implements EntityApplicationModel<M> {

  private static final int VALIDITY_CHECK_INTERVAL_SECONDS = 30;

  private final EntityConnectionProvider connectionProvider;
  private final State connectionValidState = States.state();
  private final TaskScheduler validityCheckScheduler = new TaskScheduler(this::checkConnectionValidity,
          VALIDITY_CHECK_INTERVAL_SECONDS, VALIDITY_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
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
      connectionProvider.addOnConnectListener(() -> {
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
    onLogin();
  }

  @Override
  public final void logout() {
    connectionProvider.setUser(null);
    clear();
    onLogout();
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
  public final Domain getDomain() {
    return connectionProvider.getDomain();
  }

  @Override
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
    return entityModels.stream().anyMatch(entityModel -> entityModel.getClass().equals(modelClass));
  }

  @Override
  public final boolean containsEntityModel(final String entityId) {
    return entityModels.stream().anyMatch(entityModel -> entityModel.getEntityId().equals(entityId));
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
      entityModel.refresh();
    }
  }

  @Override
  public final void clear() {
    for (final M entityModel : entityModels) {
      entityModel.clear();
    }
  }

  @Override
  public final M getEntityModel(final Class<? extends M> modelClass) {
    for (final M model : entityModels) {
      if (model.getClass().equals(modelClass)) {
        return model;
      }
    }

    throw new IllegalArgumentException("EntityModel of type: " + modelClass + " not found");
  }

  @Override
  public final M getEntityModel(final String entityId) {
    for (final M entityModel : entityModels) {
      if (entityModel.getEntityId().equals(entityId)) {
        return entityModel;
      }
    }

    throw new IllegalArgumentException("EntityModel for type " + entityId + " not  found in model: " + this);
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

  /**
   * Called after a logout has been performed.
   * Override to add a logout handler.
   */
  protected void onLogout() {/*For subclasses*/}

  /**
   * Called after a login has been performed
   * Override to add a login handler.
   */
  protected void onLogin() {/*For subclasses*/}

  private void checkConnectionValidity() {
    connectionValidState.set(connectionProvider.isConnectionValid());
    if (!connectionValidState.get()) {
      validityCheckScheduler.stop();
    }
  }

  private static boolean containsUnsavedData(final Collection<? extends EntityModel> models) {
    for (final EntityModel model : models) {
      final EntityEditModel editModel = model.getEditModel();
      if (editModel.containsUnsavedData() || containsUnsavedData(model.getDetailModels())) {
        return true;
      }
    }

    return false;
  }
}
