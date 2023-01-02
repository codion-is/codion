/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

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
 * @param <E> the type of {@link AbstractEntityEditModel} used by this {@link EntityModel}
 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
 */
public class DefaultEntityApplicationModel<M extends DefaultEntityModel<M, E, T>,
        E extends AbstractEntityEditModel, T extends EntityTableModel<E>> implements EntityApplicationModel<M, E, T> {

  private static final int VALIDITY_CHECK_INTERVAL_SECONDS = 30;

  private final EntityConnectionProvider connectionProvider;
  private final State connectionValidState = State.state();
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
  public DefaultEntityApplicationModel(EntityConnectionProvider connectionProvider) {
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
  public final User user() {
    return connectionProvider.connection().user();
  }

  @Override
  public final EntityConnectionProvider connectionProvider() {
    return connectionProvider;
  }

  @Override
  public final StateObserver connectionValidObserver() {
    return connectionValidState.observer();
  }

  @Override
  public final Entities entities() {
    return connectionProvider.entities();
  }

  @Override
  @SafeVarargs
  public final void addEntityModels(M... entityModels) {
    requireNonNull(entityModels, "entityModels");
    for (M entityModel : entityModels) {
      addEntityModel(entityModel);
    }
  }

  @Override
  public final M addEntityModel(M detailModel) {
    this.entityModels.add(detailModel);

    return detailModel;
  }

  @Override
  public final boolean containsEntityModel(Class<? extends M> modelClass) {
    return entityModels.stream()
            .anyMatch(entityModel -> entityModel.getClass().equals(modelClass));
  }

  @Override
  public final boolean containsEntityModel(EntityType entityType) {
    return entityModels.stream()
            .anyMatch(entityModel -> entityModel.entityType().equals(entityType));
  }

  @Override
  public final boolean containsEntityModel(M entityModel) {
    return entityModels.contains(entityModel);
  }

  @Override
  public final List<M> entityModels() {
    return Collections.unmodifiableList(entityModels);
  }

  @Override
  public final void refresh() {
    for (M entityModel : entityModels) {
      if (entityModel.containsTableModel()) {
        entityModel.tableModel().refresh();
      }
    }
  }

  @Override
  public final <T extends M> T entityModel(Class<? extends M> modelClass) {
    for (M model : entityModels) {
      if (model.getClass().equals(modelClass)) {
        return (T) model;
      }
    }

    throw new IllegalArgumentException("EntityModel of type: " + modelClass + " not found");
  }

  @Override
  public final M entityModel(EntityType entityType) {
    for (M entityModel : entityModels) {
      if (entityModel.entityType().equals(entityType)) {
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
  public final void setWarnAboutUnsavedData(boolean warnAboutUnsavedData) {
    this.warnAboutUnsavedData = warnAboutUnsavedData;
  }

  @Override
  public final boolean containsUnsavedData() {
    return containsUnsavedData(entityModels);
  }

  @Override
  public void savePreferences() {
    entityModels().forEach(EntityModel::savePreferences);
  }

  private void checkConnectionValidity() {
    connectionValidState.set(connectionProvider.isConnectionValid());
    if (!connectionValidState.get()) {
      validityCheckScheduler.stop();
    }
  }

  private static boolean containsUnsavedData(Collection<? extends EntityModel<?, ?, ?>> models) {
    for (EntityModel<?, ?, ?> model : models) {
      EntityEditModel editModel = model.editModel();
      if (editModel.containsUnsavedData() || containsUnsavedData(model.detailModels())) {
        return true;
      }
    }

    return false;
  }
}
