/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.User;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.db.provider.EntityDbProviderFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A central application model class.
 */
public abstract class EntityApplicationModel implements Refreshable {

  private final Event evtSelectionFiltersDetailChanged = new Event();
  private final Event evtCascadeRefreshChanged = new Event();

  private final EntityDbProvider dbProvider;
  private final List<? extends EntityModel> mainApplicationModels;

  public EntityApplicationModel(final User user, final String applicationID) {
    loadDomainModel();
    this.dbProvider = initializeDbProvider(user, applicationID);
    this.mainApplicationModels = initializeMainApplicationModels(dbProvider);
    bindEvents();
  }

  public EntityApplicationModel(final EntityDbProvider dbProvider) {
    loadDomainModel();
    this.dbProvider = dbProvider;
    this.mainApplicationModels = initializeMainApplicationModels(dbProvider);
    bindEvents();
  }

  public void logout() {
    dbProvider.setUser(null);
    clear();
  }

  public void login(final User user) {
    dbProvider.setUser(user);
    for (final EntityModel mainApplicationModel : mainApplicationModels) {
      mainApplicationModel.refresh();
    }
  }

  /**
   * @return the current user
   */
  public User getUser() {
    try {
      return dbProvider.getEntityDb().getUser();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the EntityDbProvider instance being used by this EntityApplicationModel
   */
  public EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  /**
   * @return a List containing the main application models
   */
  public List<? extends EntityModel> getMainApplicationModels() {
    return new ArrayList<EntityModel>(mainApplicationModels);
  }

  /**
   * @return true if cascade refresh is active
   */
  public boolean isCascadeRefresh() {
    return mainApplicationModels.size() > 0 && mainApplicationModels.iterator().next().isCascadeRefresh();
  }

  /**
   * fires: evtCascadeRefreshChanged
   * @param value the new value
   */
  public void setCascadeRefresh(final boolean value) {
    if (mainApplicationModels.size() > 0 && isCascadeRefresh() != value) {
      for (final EntityModel mainApplicationModel : mainApplicationModels) {
        mainApplicationModel.setCascadeRefresh(value);
      }

      evtCascadeRefreshChanged.fire();
    }
  }

  /**
   * @return true if selection filters detail is active
   */
  public boolean isSelectionFiltersDetail() {
    return mainApplicationModels.size() > 0 && mainApplicationModels.iterator().next().isSelectionFiltersDetail();
  }

  /**
   * fires: evtSelectionFiltersDetailChanged
   * @param value the new value
   */
  public void setSelectionFiltersDetail(boolean value) {
    if (mainApplicationModels.size() > 0 && isSelectionFiltersDetail() != value) {
      for (final EntityModel mainApplicationModel : mainApplicationModels) {
        mainApplicationModel.setSelectionFiltersDetail(value);
      }

      evtSelectionFiltersDetailChanged.fire();
    }
  }

  /**
   * Refreshes the whole application tree
   */
  public void refresh() {
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

  public void clear() {
    for (final EntityModel mainApplicationModel : mainApplicationModels) {
      mainApplicationModel.clear();
    }
  }

  public EntityModel getMainApplicationModel(final Class modelClass) {
    for (final EntityModel model : mainApplicationModels) {
      if (model.getClass().equals(modelClass)) {
        return model;
      }
    }

    throw new RuntimeException("Detail model of class: " + modelClass + " not found");
  }

  public Event eventCascadeRefreshChanged() {
    return evtCascadeRefreshChanged;
  }

  public Event eventSelectionFiltersDetailChanged() {
    return evtSelectionFiltersDetailChanged;
  }

  /**
   * This method should load the domain model, for example by instantiating the domain model
   * class or simply loading it by name
   */
  protected abstract void loadDomainModel();

  /**
   * Returns the main EntityModel instances
   * @param dbProvider the EntityDbProvider instance
   * @return a list containing the main application models
   */
  protected abstract List<? extends EntityModel> initializeMainApplicationModels(final EntityDbProvider dbProvider);

  protected EntityDbProvider initializeDbProvider(final User user, final String applicationID) {
    return EntityDbProviderFactory.createEntityDbProvider(user, applicationID);
  }

  protected void bindEvents() {}

  protected EntityModel createEntityModel(final String entityID) {
    return new EntityModel(entityID, getDbProvider());
  }
}
