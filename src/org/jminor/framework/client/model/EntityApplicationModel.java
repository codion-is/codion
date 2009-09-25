/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.User;
import org.jminor.common.model.Event;
import org.jminor.common.model.UserException;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.db.provider.EntityDbProviderFactory;

import java.util.List;

public abstract class EntityApplicationModel {

  public final Event evtSelectionFiltersDetailChanged = new Event();
  public final Event evtCascadeRefreshChanged = new Event();

  private final EntityDbProvider dbProvider;
  private final List<? extends EntityModel> mainApplicationModels;

  public EntityApplicationModel(final User user, final String appID) throws UserException {
    this(EntityDbProviderFactory.createEntityDbProvider(user, createClientKey(appID, user)));
  }

  public EntityApplicationModel(final EntityDbProvider dbProvider) throws UserException {
    loadDomainModel();
    this.dbProvider = dbProvider;
    this.mainApplicationModels = initializeMainApplicationModels(dbProvider);
    bindEvents();
  }

  /**
   * @return the current user
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  public User getUser() throws UserException {
    try {
      return dbProvider.getEntityDb().getUser();
    }
    catch (Exception e) {
      throw new UserException(e);
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
    return mainApplicationModels;
  }

  /**
   * @return true if cascade refresh is active
   */
  public boolean isCascadeRefresh() {
    return mainApplicationModels.size() > 0 && mainApplicationModels.iterator().next().getCascadeRefresh();
  }

  /**
   * fires: evtCascadeRefreshChanged
   * @param value the new value
   */
  public void setCascadeRefresh(final boolean value) {
    if (mainApplicationModels.size() > 0 && isCascadeRefresh() != value) {
      for (final EntityModel mainApplicationModel : mainApplicationModels)
        mainApplicationModel.setCascadeRefresh(value);

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
      for (final EntityModel mainApplicationModel : mainApplicationModels)
        mainApplicationModel.setSelectionFiltersDetail(value);

      evtSelectionFiltersDetailChanged.fire();
    }
  }

  /**
   * Refreshes the whole application tree
   * @throws UserException in case of an exception
   */
  public void refreshAll() throws UserException {
    final boolean cascade = isCascadeRefresh();
    try {
      setCascadeRefresh(true);
      for (final EntityModel mainApplicationModel : mainApplicationModels)
        mainApplicationModel.refresh();
    }
    finally {
      setCascadeRefresh(cascade);
    }
  }

  public static String createClientKey(final String reference, final User user) {
    return (reference != null ? (reference + " - ") : "")
            + "[" + user.getUsername() + "] " + Long.toHexString(System.currentTimeMillis());
  }

  public EntityModel getMainApplicationModel(final Class modelClass) {
    for (final EntityModel model : mainApplicationModels)
      if (model.getClass().equals(modelClass))
        return model;

    return null;
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
   * @throws UserException in case of an exception
   */
  protected abstract List<? extends EntityModel> initializeMainApplicationModels(final EntityDbProvider dbProvider) throws UserException;

  protected void bindEvents() {}
}
