/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.db.EntityConnectionProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * A central application model class.
 */
public abstract class DefaultEntityApplicationModel<Model extends DefaultEntityModel> implements EntityApplicationModel<Model> {

  private final EntityConnectionProvider connectionProvider;
  private final List<Model> entityModels = new ArrayList<>();

  /**
   * Instantiates a new DefaultEntityApplicationModel
   * @param connectionProvider the EntityConnectionProvider instance
   * @throws IllegalArgumentException in case connectionProvider is null
   */
  public DefaultEntityApplicationModel(final EntityConnectionProvider connectionProvider) {
    Util.rejectNullValue(connectionProvider, "connectionProvider");
    this.connectionProvider = connectionProvider;
    try {
      loadDomainModel();
    }
    catch (final ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void login(final User user) {
    Util.rejectNullValue(user, "user");
    connectionProvider.setUser(user);
    for (final Model entityModel : entityModels) {
      entityModel.refresh();
    }
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
  public final void addEntityModels(final Model... entityModels) {
    Util.rejectNullValue(entityModels, "entityModels");
    for (final Model entityModel : entityModels) {
      addEntityModel(entityModel);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Model addEntityModel(final Model detailModel) {
    this.entityModels.add(detailModel);

    return detailModel;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsEntityModel(final Class<? extends Model> modelClass) {
    for (final Model entityModel : entityModels) {
      if (entityModel.getClass().equals(modelClass)) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsEntityModel(final String entityID) {
    for (final Model entityModel : entityModels) {
      if (entityModel.getEntityID().equals(entityID)) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsEntityModel(final Model entityModel) {
    return entityModels.contains(entityModel);
  }

  /** {@inheritDoc} */
  @Override
  public final List<? extends Model> getEntityModels() {
    return Collections.unmodifiableList(entityModels);
  }

  /** {@inheritDoc} */
  @Override
  public final void refresh() {
    for (final Model entityModel : entityModels) {
      entityModel.refresh();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    for (final Model entityModel : entityModels) {
      entityModel.clear();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Model getEntityModel(final Class<? extends Model> modelClass) {
    for (final Model model : entityModels) {
      if (model.getClass().equals(modelClass)) {
        return model;
      }
    }

    throw new IllegalArgumentException("EntityModel of type: " + modelClass + " not found");
  }

  /** {@inheritDoc} */
  @Override
  public final Model getEntityModel(final String entityID) {
    for (final Model entityModel : entityModels) {
      if (entityModel.getEntityID().equals(entityID)) {
        return entityModel;
      }
    }

    throw new IllegalArgumentException("EntityModel for type " + entityID + " not  found in model: " + this);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsUnsavedData() {
    return containsUnsavedData(entityModels);
  }

  /**
   * This method should load the domain model, for example by instantiating the domain model
   * class or simply loading it by name
   * @throws ClassNotFoundException in case the domain model class is not found on the classpath
   */
  protected abstract void loadDomainModel() throws ClassNotFoundException;

  /**
   * Called after a logout has been performed.
   * Override to add a logout handler.
   */
  protected void handleLogout() {}

  /**
   * Called after a login has been performed
   * Override to add a login handler.
   */
  protected void handleLogin() {}

  private static boolean containsUnsavedData(final Collection<? extends EntityModel> models) {
    for (final EntityModel model : models) {
      final EntityEditModel editModel = model.getEditModel();
      if (editModel.containsUnsavedData()) {
        return true;
      }
      else if (containsUnsavedData(model.getDetailModels())) {
        return true;
      }
    }

    return false;
  }
}
