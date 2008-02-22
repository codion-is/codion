/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.User;
import org.jminor.common.model.Event;
import org.jminor.common.model.UserException;
import org.jminor.framework.db.EntityDbProviderFactory;
import org.jminor.framework.db.IEntityDbProvider;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class EntityApplicationModel {

  public final Event evtSelectionFiltersDetailChanged = new Event("EntityApplicationModel.evtSelectionFiltersDetailChanged");
  public final Event evtCascadeRefreshChanged = new Event("EntityApplicationModel.evtCascadeRefreshChanged");

  private final IEntityDbProvider dbProvider;
  private final List<EntityModel> mainApplicationModels;
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

      applicationTreeModel = createApplicationTree(mainApplicationModels);
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

  /**
   * @return Value for property 'mainApplicationModels'.
   */
  public List<EntityModel> getMainApplicationModels() {
    return mainApplicationModels;
  }

  /**
   * @return Value for property 'cascadeRefresh'.
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
   * @return Value for property 'selectionFiltersDetail'.
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
    for (final EntityModel mainApplicationModel : mainApplicationModels) {
      if (forceRefresh)
        mainApplicationModel.forceRefresh();
      else
        mainApplicationModel.refresh();
    }
  }

  /**
   * @return Value for property 'applicationModel'.
   */
  public static EntityApplicationModel getApplicationModel() {
    return entityApplicationModel;
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

  protected abstract void loadDbModel();

  /**
   * @return Value for property 'rootEntityModelClasses'.
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  protected abstract List<Class<? extends EntityModel>> getRootEntityModelClasses()throws UserException;

  protected List<EntityModel> initMainApplicationModels() throws UserException {
    return initMainApplicationModels(getRootEntityModelClasses());
  }

  protected List<EntityModel> initMainApplicationModels(final List<Class<? extends EntityModel>> mainModelClasses) throws UserException {
    try {
      final List<EntityModel> ret = new ArrayList<EntityModel>(mainModelClasses.size());
      for (Class<? extends EntityModel> mainModelClass : mainModelClasses)
        ret.add(mainModelClass.getConstructor(IEntityDbProvider.class).newInstance(getDbConnectionProvider()));

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

  protected void bindEvents() {}

  private static DefaultTreeModel createApplicationTree(final Collection<? extends EntityModel> entityModels) {
    final DefaultTreeModel applicationTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    addModelsToTree((DefaultMutableTreeNode) applicationTreeModel.getRoot(), entityModels);

    return applicationTreeModel;
  }

  private static void addModelsToTree(final DefaultMutableTreeNode root, final Collection<? extends EntityModel> models) {
    for (final EntityModel model : models) {
      final DefaultMutableTreeNode node = new DefaultMutableTreeNode(model);
      root.add(node);
      if (model.getDetailModels().size() > 0)
        addModelsToTree(node, model.getDetailModels());
    }
  }
}
