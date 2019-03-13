/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Configuration;
import org.jminor.common.StateObserver;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.model.Refreshable;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;

import java.util.List;

/**
 * A central application model class.
 * @param <M> the type of {@link EntityModel} used by this application model
 */
public interface EntityApplicationModel<M extends EntityModel> extends Refreshable {

  /**
   * Specifies a string to prepend to the username field in the login dialog<br>
   * Value type: String<br>
   * Default value: [empty string]
   */
  Value<String> USERNAME_PREFIX = Configuration.stringValue("jminor.client.usernamePrefix", "");

  /**
   * Specifies whether user authentication is required<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  Value<Boolean> AUTHENTICATION_REQUIRED = Configuration.booleanValue("jminor.client.authenticationRequired", true);

  /**
   * Specifies whether or not the client saves the last successful login username,<br>
   * which is then displayed as the default username the next time the application is started<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  Value<Boolean> SAVE_DEFAULT_USERNAME = Configuration.booleanValue("jminor.client.saveDefaultUsername", true);

  /**
   * The report path used for the default report generation,
   * either file or http based
   */
  Value<String> REPORT_PATH = Configuration.stringValue("jminor.report.path", null);

  /**
   * Specifies whether a periodic (30 sec) validity check of the underlying connection should be scheduled.
   * Value type: Boolean<br>
   * Default value: true
   */
  Value<Boolean> SCHEDULE_CONNECTION_VALIDATION = Configuration.booleanValue("jminor.client.scheduleConnectionValidation", true);

  /**
   * @return the value associated with {@link #REPORT_PATH}
   * @throws IllegalArgumentException in case it is not specified
   */
  static String getReportPath() {
    final String path = REPORT_PATH.get();
    if (Util.nullOrEmpty(path)) {
      throw new IllegalArgumentException(REPORT_PATH + " property is not specified");
    }

    return path;
  }

  /**
   * Log out from this application model
   */
  void logout();

  /**
   * Logs in the given user
   * @param user the user to login
   * @throws NullPointerException in case user is null
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
   * Returns a StateObserver which is active while the underlying application connection provider is connected.
   * Note that this state is updated every 30 seconds if {@link #SCHEDULE_CONNECTION_VALIDATION} is true
   * @return a StateObserver indicating the validity of the underlying connection provider
   * @see #SCHEDULE_CONNECTION_VALIDATION
   */
  StateObserver getConnectionValidObserver();

  /**
   * @return the underlying domain model Entities
   */
  Entities getDomain();

  /**
   * Adds the given entity models to this model.
   * @param entityModels the entity models to add
   */
  void addEntityModels(final M... entityModels);

  /**
   * Adds the given entity model to this model
   * @param entityModel the detail model
   * @return the EntityModel model just added
   */
  M addEntityModel(final M entityModel);

  /**
   * @param modelClass the application model class
   * @return true if this model contains a EntityModel instance of the given class
   */
  boolean containsEntityModel(final Class<? extends M> modelClass);

  /**
   * @param entityId the entity ID
   * @return true if this model contains a EntityModel for the given entity ID
   */
  boolean containsEntityModel(final String entityId);

  /**
   * @param entityModel the entity model
   * @return true if this model contains the given EntityModel
   */
  boolean containsEntityModel(final M entityModel);

  /**
   * @return true if this model warns about unsaved data
   * @see EntityEditModel#WARN_ABOUT_UNSAVED_DATA
   */
  boolean isWarnAboutUnsavedData();

  /**
   * @param warnAboutUnsavedData if true then this model warns about unsaved data
   * @see EntityEditModel#WARN_ABOUT_UNSAVED_DATA
   */
  void setWarnAboutUnsavedData(final boolean warnAboutUnsavedData);

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
  M getEntityModel(final Class<? extends M> modelClass);

  /**
   * @param entityId the entity ID
   * @return the EntityModel based on the given entityId
   */
  M getEntityModel(final String entityId);
}
