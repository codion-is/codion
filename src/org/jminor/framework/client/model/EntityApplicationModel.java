/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Refreshable;
import org.jminor.common.model.User;
import org.jminor.framework.db.provider.EntityDbProvider;

import java.util.List;

/**
 * A central application model class.
 */
public interface EntityApplicationModel extends Refreshable {

  /**
   * Log out from this application model
   */
  void logout();

  /**
   * Logs in the given user
   * @param user the user to login
   */
  void login(final User user);

  /**
   * @return the current user
   */
  User getUser();

  /**
   * @return the EntityDbProvider instance being used by this EntityApplicationModel
   */
  EntityDbProvider getDbProvider();

  /**
   * Adds the given detail models to this model.
   * @param mainApplicationModels the detail models to add
   */
  void addMainApplicationModels(final EntityModel... mainApplicationModels);

  /**
   * Adds the given detail model to this model
   * @param detailModel the detail model
   * @return the detail model just added
   */
  EntityModel addMainApplicationModel(final EntityModel detailModel);

  /**
   * @return an unmodifiable List containing the main application models
   */
  List<? extends EntityModel> getMainApplicationModels();

  /**
   * @param modelClass the model class
   * @return the EntityModel of the given type
   */
  EntityModel getMainApplicationModel(final Class<? extends EntityModel> modelClass);

  /**
   * @param entityID the entity ID
   * @return the EntityModel based on the given entityID
   */
  EntityModel getMainApplicationModel(final String entityID);
}
