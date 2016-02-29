/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.model.Refreshable;
import org.jminor.common.model.User;
import org.jminor.framework.db.EntityConnectionProvider;

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
   * @throws IllegalArgumentException in case user is null
   */
  void login(final User user);

  /**
   * @return the current user
   */
  User getUser();

  /**
   * @return the EntityConnectionProvider instance being used by this EntityApplicationModel
   */
  EntityConnectionProvider getConnectionProvider();

  /**
   * Adds the given entity models to this model.
   * @param entityModels the entity models to add
   */
  void addEntityModels(final EntityModel... entityModels);

  /**
   * Adds the given entity model to this model
   * @param entityModel the detail model
   * @return the EntityModel model just added
   */
  EntityModel addEntityModel(final EntityModel entityModel);

  /**
   * @param modelClass the application model class
   * @return true if this model contains a EntityModel instance of the given class
   */
  boolean containsEntityModel(final Class<? extends EntityModel> modelClass);

  /**
   * @param entityID the entity ID
   * @return true if this model contains a EntityModel for the given entity ID
   */
  boolean containsEntityModel(final String entityID);

  /**
   * @param entityModel the entity model
   * @return true if this model contains the given EntityModel
   */
  boolean containsEntityModel(final EntityModel entityModel);

  /**
   * @return true if any edit model associated with this application model contains
   * modified and unsaved data, that is, existing entities that have been modified but not saved
   */
  boolean containsUnsavedData();

  /**
   * @return an unmodifiable List containing the EntityModel instances contained
   * in this EntityApplicationModel
   */
  List<? extends EntityModel> getEntityModels();

  /**
   * @param modelClass the model class
   * @return the EntityModel of the given type
   */
  EntityModel getEntityModel(final Class<? extends EntityModel> modelClass);

  /**
   * @param entityID the entity ID
   * @return the EntityModel based on the given entityID
   */
  EntityModel getEntityModel(final String entityID);
}
