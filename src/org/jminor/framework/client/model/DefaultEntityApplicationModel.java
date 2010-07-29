/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
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

  private final Event evtCascadeRefreshChanged = new Event();

  private final EntityDbProvider dbProvider;
  private final List<EntityModel> mainApplicationModels = new ArrayList<EntityModel>();

  public DefaultEntityApplicationModel(final EntityDbProvider dbProvider) {
    this.dbProvider = dbProvider;
    loadDomainModel();
  }

  public final void logout() {
    dbProvider.setUser(null);
    clear();
    handleLogout();
  }

  public final void login(final User user) {
    dbProvider.setUser(user);
    for (final EntityModel mainApplicationModel : mainApplicationModels) {
      mainApplicationModel.refresh();
    }
    handleLogin();
  }

  public final User getUser() {
    try {
      return dbProvider.getEntityDb().getUser();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public final EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  public final void addMainApplicationModels(final EntityModel... mainApplicationModels) {
    Util.rejectNullValue(mainApplicationModels, "mainApplicationModels");
    for (final EntityModel model : mainApplicationModels) {
      addMainApplicationModel(model);
    }
  }

  public final EntityModel addMainApplicationModel(final EntityModel detailModel) {
    this.mainApplicationModels.add(detailModel);

    return detailModel;
  }

  public final List<? extends EntityModel> getMainApplicationModels() {
    return Collections.unmodifiableList(mainApplicationModels);
  }

  public final boolean isCascadeRefresh() {
    return !mainApplicationModels.isEmpty() && mainApplicationModels.iterator().next().isCascadeRefresh();
  }

  public final void setCascadeRefresh(final boolean value) {
    if (!mainApplicationModels.isEmpty() && isCascadeRefresh() != value) {
      for (final EntityModel mainApplicationModel : mainApplicationModels) {
        mainApplicationModel.setCascadeRefresh(value);
      }

      evtCascadeRefreshChanged.fire();
    }
  }

  public final void refresh() {
    final boolean cascade = isCascadeRefresh();
    try {
      setCascadeRefresh(true);
      for (final EntityModel mainApplicationModel : mainApplicationModels) {
        mainApplicationModel.refresh();
      }
    }
    finally {
      setCascadeRefresh(cascade);
    }
  }

  public final void clear() {
    for (final EntityModel mainApplicationModel : mainApplicationModels) {
      mainApplicationModel.clear();
    }
  }

  public final EntityModel getMainApplicationModel(final Class<? extends EntityModel> modelClass) {
    for (final EntityModel model : mainApplicationModels) {
      if (model.getClass().equals(modelClass)) {
        return model;
      }
    }

    throw new RuntimeException("Detail model of class: " + modelClass + " not found");
  }

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
    throw new RuntimeException("No detail model for type " + entityID + " found in model: " + this);
  }

  public final EventObserver cascadeRefreshObserver() {
    return evtCascadeRefreshChanged;
  }

  /**
   * This method should load the domain model, for example by instantiating the domain model
   * class or simply loading it by name
   */
  protected abstract void loadDomainModel();

  protected void handleLogout() {}

  protected void handleLogin() {}

  protected EntityModel createEntityModel(final String entityID) {
    return new DefaultEntityModel(entityID, dbProvider);
  }
}
