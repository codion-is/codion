/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A central application model class.
 * @param <M> the type of {@link DefaultEntityModel} this application model is based on
 * @param <E> the type of {@link AbstractEntityEditModel} used by this {@link EntityModel}
 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
 */
public class DefaultEntityApplicationModel<M extends DefaultEntityModel<M, E, T>,
        E extends AbstractEntityEditModel, T extends EntityTableModel<E>> implements EntityApplicationModel<M, E, T> {

  private final EntityConnectionProvider connectionProvider;
  private final Version version;
  private final List<M> entityModels = new ArrayList<>();

  private boolean warnAboutUnsavedData = EntityEditModel.WARN_ABOUT_UNSAVED_DATA.get();

  /**
   * Instantiates a new DefaultEntityApplicationModel
   * @param connectionProvider the EntityConnectionProvider instance
   * @throws NullPointerException in case connectionProvider is null
   */
  public DefaultEntityApplicationModel(EntityConnectionProvider connectionProvider) {
    this(connectionProvider, null);
  }

  /**
   * Instantiates a new DefaultEntityApplicationModel
   * @param connectionProvider the EntityConnectionProvider instance
   * @param version the application version
   * @throws NullPointerException in case connectionProvider is null
   */
  public DefaultEntityApplicationModel(EntityConnectionProvider connectionProvider, Version version) {
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    this.version = version;
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
  public final Optional<Version> version() {
    return Optional.ofNullable(version);
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
  public final void addEntityModel(M entityModel) {
    if (this.entityModels.contains(requireNonNull(entityModel))) {
      throw new IllegalArgumentException("Entity model " + entityModel + " has already been added");
    }
    this.entityModels.add(entityModel);
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
  public final <C extends M> C entityModel(Class<C> modelClass) {
    for (M model : entityModels) {
      if (model.getClass().equals(modelClass)) {
        return (C) model;
      }
    }

    throw new IllegalArgumentException("EntityModel of type: " + modelClass + " not found");
  }

  @Override
  public final <C extends M> C entityModel(EntityType entityType) {
    for (M entityModel : entityModels) {
      if (entityModel.entityType().equals(entityType)) {
        return (C) entityModel;
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
