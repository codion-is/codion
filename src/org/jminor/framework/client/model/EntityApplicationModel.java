/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.User;
import org.jminor.common.model.Event;
import org.jminor.common.model.UserException;
import org.jminor.framework.db.EntityDbProviderFactory;
import org.jminor.framework.db.IEntityDbProvider;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public abstract class EntityApplicationModel {

  public final Event evtSelectionFiltersDetailChanged = new Event();
  public final Event evtCascadeRefreshChanged = new Event();

  private final IEntityDbProvider dbProvider;
  private final List<EntityModel> mainApplicationModels;

  public EntityApplicationModel(final User user, final String appID) throws UserException {
    this(EntityDbProviderFactory.createEntityDbProvider(user, createClientKey(appID, user)));
  }

  public EntityApplicationModel(final IEntityDbProvider dbProvider) throws UserException {
    loadDomainModel();
    this.dbProvider = dbProvider;
    this.mainApplicationModels = initializeMainApplicationModels();
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
   * @return the IEntityDbProvider instance being used by this EntityApplicationModel
   */
  public IEntityDbProvider getDbConnectionProvider() {
    return dbProvider;
  }

  /**
   * @return a List containing the main application models
   */
  public List<EntityModel> getMainApplicationModels() {
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
    return mainApplicationModels.size() > 0 && mainApplicationModels.iterator().next().getSelectionFiltersDetail();
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

  public EntityModel getMainApplicationModel(final Class<? extends EntityModel> modelClass) {
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
   * @return a List containing the classes of the main EntityModels
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  protected abstract List<Class<? extends EntityModel>> getMainEntityModelClasses() throws UserException;

  protected List<EntityModel> initializeMainApplicationModels() throws UserException {
    try {
      final List<Class<? extends EntityModel>> mainModelClasses = getMainEntityModelClasses();
      final List<EntityModel> ret = new ArrayList<EntityModel>(mainModelClasses.size());
      for (final Class<? extends EntityModel> mainModelClass : mainModelClasses)
        ret.add(instantiateMainApplicationModel(mainModelClass));

      return ret;
    }
    catch (InvocationTargetException e) {
      if (e.getTargetException() instanceof UserException)
        throw (UserException) e.getTargetException();

      throw new UserException(e.getTargetException());
    }
    catch (NoSuchMethodException e) {
      throw new UserException(e);
    }
    catch (IllegalAccessException e) {
      throw new UserException(e);
    }
    catch (InstantiationException e) {
      throw new UserException(e);
    }
  }

  protected EntityModel instantiateMainApplicationModel(Class<? extends EntityModel> mainModelClass)
          throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    return mainModelClass.getConstructor(IEntityDbProvider.class).newInstance(getDbConnectionProvider());
  }

  protected void bindEvents() {}
}
