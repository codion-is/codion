/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.provider.EntityDbProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A central application model class.
 */
public abstract class DefaultEntityApplicationModel implements EntityApplicationModel {

  private final EntityDbProvider dbProvider;
  private final List<EntityModel> mainApplicationModels = new ArrayList<EntityModel>();

  /**
   * Instantiates a new DefaultEntityApplicationModel
   * @param dbProvider the EntityDbProvider instance
   */
  public DefaultEntityApplicationModel(final EntityDbProvider dbProvider) {
    this.dbProvider = dbProvider;
    loadDomainModel();
  }

  /** {@inheritDoc} */
  public final void logout() {
    dbProvider.setUser(null);
    clear();
    handleLogout();
  }

  /** {@inheritDoc} */
  public final void login(final User user) {
    dbProvider.setUser(user);
    for (final EntityModel mainApplicationModel : mainApplicationModels) {
      mainApplicationModel.refresh();
    }
    handleLogin();
  }

  /** {@inheritDoc} */
  public final User getUser() {
    return dbProvider.getEntityDb().getUser();
  }

  /** {@inheritDoc} */
  public final EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  /** {@inheritDoc} */
  public final void addMainApplicationModels(final EntityModel... mainApplicationModels) {
    Util.rejectNullValue(mainApplicationModels, "mainApplicationModels");
    for (final EntityModel model : mainApplicationModels) {
      addMainApplicationModel(model);
    }
  }

  /** {@inheritDoc} */
  public final EntityModel addMainApplicationModel(final EntityModel detailModel) {
    this.mainApplicationModels.add(detailModel);

    return detailModel;
  }

  /** {@inheritDoc} */
  public final List<? extends EntityModel> getMainApplicationModels() {
    return Collections.unmodifiableList(mainApplicationModels);
  }

  /** {@inheritDoc} */
  public final void refresh() {
    for (final EntityModel mainApplicationModel : mainApplicationModels) {
      mainApplicationModel.refresh();
    }
  }

  /** {@inheritDoc} */
  public final void clear() {
    for (final EntityModel mainApplicationModel : mainApplicationModels) {
      mainApplicationModel.clear();
    }
  }

  /** {@inheritDoc} */
  public final EntityModel getMainApplicationModel(final Class<? extends EntityModel> modelClass) {
    for (final EntityModel model : mainApplicationModels) {
      if (model.getClass().equals(modelClass)) {
        return model;
      }
    }

    throw new IllegalArgumentException("Detail model of class: " + modelClass + " not found");
  }

  /** {@inheritDoc} */
  public final EntityModel getMainApplicationModel(final String entityID) {
    for (final EntityModel detailModel : mainApplicationModels) {
      if (detailModel.getEntityID().equals(entityID)) {
        return detailModel;
      }
    }

    if (Configuration.getBooleanValue(Configuration.AUTO_CREATE_ENTITY_MODELS)) {
      try {
        final EntityModel detailModel = new DefaultEntityModel(entityID, dbProvider);
        addMainApplicationModel(detailModel);
        return detailModel;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    throw new IllegalArgumentException("No detail model for type " + entityID + " found in model: " + this);
  }

  /**
   * This method should load the domain model, for example by instantiating the domain model
   * class or simply loading it by name
   */
  protected abstract void loadDomainModel();

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
}
