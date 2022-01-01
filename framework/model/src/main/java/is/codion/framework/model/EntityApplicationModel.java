/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.event.EventDataListener;
import is.codion.common.state.StateObserver;
import is.codion.common.user.User;
import is.codion.common.value.PropertyValue;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityType;

import java.util.List;

/**
 * A central application model class.
 * @param <M> the type of {@link EntityModel} this application model is based on
 * @param <E> the type of {@link EntityEditModel} used by this {@link EntityModel}
 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
 */
public interface EntityApplicationModel<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> {

  /**
   * Specifies a string to prepend to the username field in the login dialog<br>
   * Value type: String<br>
   * Default value: [empty string]
   */
  PropertyValue<String> USERNAME_PREFIX = Configuration.stringValue("codion.client.usernamePrefix", "");

  /**
   * Specifies whether user authentication is required<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> AUTHENTICATION_REQUIRED = Configuration.booleanValue("codion.client.authenticationRequired", true);

  /**
   * Specifies whether the client saves the last successful login username,<br>
   * which is then displayed as the default username the next time the application is started<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> SAVE_DEFAULT_USERNAME = Configuration.booleanValue("codion.client.saveDefaultUsername", true);

  /**
   * Specifies whether a periodic (30 sec) validity check of the underlying connection should be scheduled.
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> SCHEDULE_CONNECTION_VALIDATION = Configuration.booleanValue("codion.client.scheduleConnectionValidation", true);

  /**
   * Closes the underlying database connection.
   * @see #addLogoutListener(EventDataListener)
   */
  void logout();

  /**
   * Logs in the given user
   * @param user the user to login
   * @throws NullPointerException in case user is null
   * @see #addLoginListener(EventDataListener)
   */
  void login(User user);

  /**
   * @return the current user
   */
  User getUser();

  /**
   * @return the EntityConnectionProvider instance being used by this EntityApplicationModel
   */
  EntityConnectionProvider getConnectionProvider();

  /**
   * Returns a StateObserver which is active while the underlying application connection provider is connected.
   * Note that this state is updated every 30 seconds if {@link #SCHEDULE_CONNECTION_VALIDATION} is true
   * @return a StateObserver indicating the validity of the underlying connection provider
   * @see #SCHEDULE_CONNECTION_VALIDATION
   */
  StateObserver getConnectionValidObserver();

  /**
   * @return the underlying domain entities
   */
  Entities getEntities();

  /**
   * Adds the given entity models to this model.
   * @param entityModels the entity models to add
   */
  void addEntityModels(M... entityModels);

  /**
   * Adds the given entity model to this model
   * @param entityModel the detail model
   * @return the EntityModel model just added
   */
  M addEntityModel(M entityModel);

  /**
   * @param modelClass the application model class
   * @return true if this model contains a EntityModel instance of the given class
   */
  boolean containsEntityModel(Class<? extends M> modelClass);

  /**
   * @param entityType the entityType
   * @return true if this model contains a EntityModel for the given entityType
   */
  boolean containsEntityModel(EntityType entityType);

  /**
   * @param entityModel the entity model
   * @return true if this model contains the given EntityModel
   */
  boolean containsEntityModel(M entityModel);

  /**
   * @return true if this model warns about unsaved data
   * @see EntityEditModel#WARN_ABOUT_UNSAVED_DATA
   */
  boolean isWarnAboutUnsavedData();

  /**
   * @param warnAboutUnsavedData if true then this model warns about unsaved data
   * @see EntityEditModel#WARN_ABOUT_UNSAVED_DATA
   */
  void setWarnAboutUnsavedData(boolean warnAboutUnsavedData);

  /**
   * @return true if any edit model associated with this application model contains
   * modified and unsaved data, that is, existing entities that have been modified but not saved
   */
  boolean containsUnsavedData();

  /**
   * Saves user preferences relating to this application model, remember to call super.savePreferences() when overriding
   */
  void savePreferences();

  /**
   * @return an unmodifiable List containing the EntityModel instances contained
   * in this EntityApplicationModel
   */
  List<M> getEntityModels();

  /**
   * @param modelClass the model class
   * @return the EntityModel of the given type
   */
  M getEntityModel(Class<? extends M> modelClass);

  /**
   * @param entityType the entityType
   * @return the EntityModel based on the given entityType
   */
  M getEntityModel(EntityType entityType);

  /**
   * Refreshes all data models contained in this application model
   */
  void refresh();

  /**
   * Clears all data from models contained in this application model
   */
  void clear();

  /**
   * @param listener a listener notified each time a login is performed
   */
  void addLoginListener(EventDataListener<User> listener);

  /**
   * @param listener the listener to remove
   */
  void removeLoginListener(EventDataListener<User> listener);

  /**
   * @param listener a listener notified each time a logout is performed
   */
  void addLogoutListener(EventDataListener<User> listener);

  /**
   * @param listener the listener to remove
   */
  void removeLogoutListener(EventDataListener<User> listener);
}
