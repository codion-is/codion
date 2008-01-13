/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.User;
import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.UserException;
import org.jminor.framework.db.EntityDbProviderFactory;
import org.jminor.framework.db.IEntityDbProvider;

import javax.swing.tree.DefaultTreeModel;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public abstract class EntityApplicationModel {

  public final Event evtSelectionFiltersDetailChanged = new Event("EntityApplicationModel.evtSelectionFiltersDetailChanged");
  public final Event evtCascadeRefreshChanged = new Event("EntityApplicationModel.evtCascadeRefreshChanged");

  public final State stSelectionEmpty = new State("EntityApplicationModel.stSelectionEmpty", true);

  private final IEntityDbProvider dbProvider;
  private final HashMap<String, EntityModel> mainApplicationModels;
  private final DefaultTreeModel applicationTreeModel;

  private static EntityApplicationModel entityApplicationModel;

  public EntityApplicationModel(final User user, final String appID) throws UserException {
    this(EntityDbProviderFactory.createEntityDbProvider(user, createClientKey(appID, user)));
  }

  public EntityApplicationModel(final IEntityDbProvider dbProvider) throws UserException {
    this.dbProvider = dbProvider;
    try {
      loadDbModel();
      mainApplicationModels = initMainApplicationModels();

      applicationTreeModel = FrameworkModelUtil.createApplicationTree(mainApplicationModels.values());
      bindEvents();
      entityApplicationModel = this;
    }
    catch (UserException ue) {
      throw ue;
    }
    catch (Exception ue) {
      throw new UserException(ue);
    }
  }

  public static EntityApplicationModel getMainEntityApplicationModel() {
    return entityApplicationModel;
  }

  /**
   * @return Value for property 'user'.
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
   * @return Value for property 'applicationTreeModel'.
   */
  public DefaultTreeModel getApplicationTreeModel() {
    return applicationTreeModel;
  }

  /**
   * @return Value for property 'dbConnectionProvider'.
   */
  public IEntityDbProvider getDbConnectionProvider() {
    return dbProvider;
  }

  public EntityModel getEntityModel(final String modelKey) {
    return mainApplicationModels.get(modelKey);
  }

  /**
   * @return Value for property 'mainApplicationModels'.
   */
  public HashMap<String, EntityModel> getMainApplicationModels() {
    return mainApplicationModels;
  }

  /**
   * @return Value for property 'cascadeRefresh'.
   */
  public boolean isCascadeRefresh() {
    final Collection<EntityModel> models = mainApplicationModels.values();

    return models.size() > 0 && models.iterator().next().getCascadeRefresh();
  }

  /**
   * fires: evtCascadeRefreshChanged
   * @param value the new value
   */
  public void setCascadeRefresh(final boolean value) {
    final Collection<EntityModel> models = mainApplicationModels.values();
    if (models.size() > 0 && isCascadeRefresh() != value) {
      for (final EntityModel mainApplicationModel : models)
        mainApplicationModel.setCascadeRefresh(value);

      evtCascadeRefreshChanged.fire();
    }
  }

  /**
   * @return Value for property 'selectionFiltersDetail'.
   */
  public boolean isSelectionFiltersDetail() {
    final Collection<EntityModel> models = mainApplicationModels.values();

    return models.size() > 0 && models.iterator().next().getSelectionFiltersDetail();
  }

  /**
   * fires: evtSelectionFiltersDetailChanged
   * @param value the new value
   */
  public void setSelectionFiltersDetail(boolean value) {
    final Collection<EntityModel> models = mainApplicationModels.values();
    if (models.size() > 0 && isSelectionFiltersDetail() != value) {
      for (final EntityModel mainApplicationModel : models)
        mainApplicationModel.setSelectionFiltersDetail(value);

      evtSelectionFiltersDetailChanged.fire();
    }
  }

  public void forceRefreshAll() throws UserException {
    final boolean cascade = isCascadeRefresh();
    try {
      setCascadeRefresh(true);
      refreshAll(true);
    }
    finally {
      setCascadeRefresh(cascade);
    }
  }

  public void refreshAll(final boolean forceRefresh) throws UserException {
    for (final EntityModel mainApplicationModel : mainApplicationModels.values()) {
      if (forceRefresh)
        mainApplicationModel.forceRefresh();
      else
        mainApplicationModel.refresh();
    }
  }

  protected abstract void loadDbModel();

  /**
   * @return Value for property 'rootEntityModelClasses'.
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  protected abstract List<Class<? extends EntityModel>> getRootEntityModelClasses()throws UserException;

  protected HashMap<String, EntityModel> initMainApplicationModels() throws UserException {
    return initMainApplicationModels(getRootEntityModelClasses());
  }

  protected HashMap<String, EntityModel> initMainApplicationModels(final List<Class<? extends EntityModel>> mainModelClasses) throws UserException {
    try {
      final HashMap<String, EntityModel> ret = new HashMap<String, EntityModel>(mainModelClasses.size());
      for (Class<? extends EntityModel> mainModelClass : mainModelClasses)
        ret.put(mainModelClass.getName(),
                mainModelClass.getConstructor(IEntityDbProvider.class).newInstance(getDbConnectionProvider()));

      return ret;
    }
    catch (InvocationTargetException e) {
      if (e.getTargetException() instanceof UserException)
        throw (UserException) e.getTargetException();

      throw new UserException(e.getTargetException());
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  /**
   * @return Value for property 'applicationModel'.
   */
  public static EntityApplicationModel getApplicationModel() {
    return entityApplicationModel;
  }

  protected void bindEvents() {}

  public static String createClientKey(final String reference, final User user) {
    return (reference != null ? (reference + " - ") : "")
            + "[" + user.getUsername() + "] " + Long.toHexString(System.currentTimeMillis());
  }
}
