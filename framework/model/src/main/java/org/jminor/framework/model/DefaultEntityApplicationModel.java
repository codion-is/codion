/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.States;
import org.jminor.common.TaskScheduler;
import org.jminor.common.User;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
    Objects.requireNonNull(connectionProvider, "connectionProvider");
    this.connectionProvider = connectionProvider;
    if (SCHEDULE_CONNECTION_VALIDATION.get()) {
      validityCheckScheduler.start();
      connectionProvider.addOnConnectListener(() -> {
        connectionValidState.setActive(true);
        validityCheckScheduler.start();
      });
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void login(final User user) {
    Objects.requireNonNull(user, "user");
    connectionProvider.setUser(user);
    refresh();
    handleLogin();
  }

  /** {@inheritDoc} */
  @Override
  public final void logout() {
    connectionProvider.setUser(null);
    clear();
    handleLogout();
  }

  /** {@inheritDoc} */
  @Override
  public final User getUser() {
    return connectionProvider.getConnection().getUser();
  }

  /** {@inheritDoc} */
  @Override
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public StateObserver getConnectionValidObserver() {
    return connectionValidState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public Entities getDomain() {
    return connectionProvider.getDomain();
  }

  /** {@inheritDoc} */
  @Override
  public final void addEntityModels(final M... entityModels) {
    Objects.requireNonNull(entityModels, "entityModels");
    for (final M entityModel : entityModels) {
      addEntityModel(entityModel);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final M addEntityModel(final M detailModel) {
    this.entityModels.add(detailModel);

    return detailModel;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsEntityModel(final Class<? extends M> modelClass) {
    return entityModels.stream().anyMatch(entityModel -> entityModel.getClass().equals(modelClass));
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsEntityModel(final String entityId) {
    return entityModels.stream().anyMatch(entityModel -> entityModel.getEntityId().equals(entityId));
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsEntityModel(final M entityModel) {
    return entityModels.contains(entityModel);
  }

  /** {@inheritDoc} */
  @Override
  public final List<M> getEntityModels() {
    return Collections.unmodifiableList(entityModels);
  }

  /** {@inheritDoc} */
  @Override
  public final void refresh() {
    for (final M entityModel : entityModels) {
      entityModel.refresh();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    for (final M entityModel : entityModels) {
      entityModel.clear();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final M getEntityModel(final Class<? extends M> modelClass) {
    for (final M model : entityModels) {
      if (model.getClass().equals(modelClass)) {
        return model;
      }
    }

    throw new IllegalArgumentException("EntityModel of type: " + modelClass + " not found");
  }

  /** {@inheritDoc} */
  @Override
  public final M getEntityModel(final String entityId) {
    for (final M entityModel : entityModels) {
      if (entityModel.getEntityId().equals(entityId)) {
        return entityModel;
      }
    }

    throw new IllegalArgumentException("EntityModel for type " + entityId + " not  found in model: " + this);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isWarnAboutUnsavedData() {
    return warnAboutUnsavedData;
  }

  /** {@inheritDoc} */
  @Override
  public final void setWarnAboutUnsavedData(final boolean warnAboutUnsavedData) {
    this.warnAboutUnsavedData = warnAboutUnsavedData;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsUnsavedData() {
    return containsUnsavedData(entityModels);
  }

  /** {@inheritDoc} */
  @Override
  public void savePreferences() {
    getEntityModels().forEach(EntityModel::savePreferences);
  }

  /**
   * Called after a logout has been performed.
   * Override to add a logout handler.
   */
  protected void handleLogout() {/*For subclasses*/}

  /**
   * Called after a login has been performed
   * Override to add a login handler.
   */
  protected void handleLogin() {/*For subclasses*/}

  private void checkConnectionValidity() {
    connectionValidState.setActive(connectionProvider.isConnectionValid());
    if (!connectionValidState.isActive()) {
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
