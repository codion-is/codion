/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.Refreshable;
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
public abstract class EntityApplicationModel implements Refreshable {

  private final Event evtSelectionFiltersDetailChanged = new Event();
  private final Event evtCascadeRefreshChanged = new Event();

  private final EntityDbProvider dbProvider;
  private final List<EntityModel> mainApplicationModels = new ArrayList<EntityModel>();

  public EntityApplicationModel(final EntityDbProvider dbProvider) {
    this.dbProvider = dbProvider;
    loadDomainModel();
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
   * Adds the given detail models to this model.
   * @param detailModels the detail models to add
   */
  public void addMainApplicationModels(final EntityModel... detailModels) {
    Util.rejectNullValue(detailModels);
    for (final EntityModel detailModel : detailModels) {
      addMainApplicationModel(detailModel);
    }
  }

  /**
   * Adds the given detail model to this model
   * @param detailModel the detail model
   * @return the detail model just added
   */
  public EntityModel addMainApplicationModel(final EntityModel detailModel) {
    this.mainApplicationModels.add(detailModel);

    return detailModel;
  }

  /**
   * @return an unmodifiable List containing the main application models
   */
  public List<? extends EntityModel> getMainApplicationModels() {
    return Collections.unmodifiableList(mainApplicationModels);
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

  public EntityModel getMainApplicationModel(final String entityID) {
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

  protected void bindEvents() {}

  protected EntityModel createEntityModel(final String entityID) {
    return new DefaultEntityModel(entityID, dbProvider);
  }
}
