/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityType;

import java.util.List;
import java.util.Optional;

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
   * @return the current user
   */
  User user();

  /**
   * @return the EntityConnectionProvider instance being used by this EntityApplicationModel
   */
  EntityConnectionProvider connectionProvider();

  /**
   * Do not cache or keep the connection returned by this method in a long living field,
   * since it may become invalid and thereby unusable.
   * @return the connection used by this application model
   */
  EntityConnection connection();

  /**
   * @return the application version, an empty Optional in case no version information is available
   */
  Optional<Version> version();

  /**
   * @return the underlying domain entities
   */
  Entities entities();

  /**
   * Adds the given entity models to this model.
   * @param entityModels the entity models to add
   * @throws IllegalArgumentException in case any of the models has already been added
   */
  void addEntityModels(M... entityModels);

  /**
   * Adds the given entity model to this model
   * @param entityModel the detail model
   * @throws IllegalArgumentException in case the model has already been added
   */
  void addEntityModel(M entityModel);

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
   * @return the State controlling whether this model warns about unsaved data
   * @see EntityEditModel#WARN_ABOUT_UNSAVED_DATA
   */
  State warnAboutUnsavedData();

  /**
   * @return true if any edit model associated with this application model contains
   * modified and unsaved data, that is, existing entities that have been modified but not saved
   */
  boolean containsUnsavedData();

  /**
   * Saves any user preferences. Note that if {@link EntityModel#USE_CLIENT_PREFERENCES} is set to 'false',
   * calling this method has no effect. Remember to call super.savePreferences() when overriding.
   */
  void savePreferences();

  /**
   * @return an unmodifiable List containing the EntityModel instances contained
   * in this EntityApplicationModel
   */
  List<M> entityModels();

  /**
   * @param <C> the model type
   * @param modelClass the model class
   * @return the EntityModel of the given type
   */
  <C extends M> C entityModel(Class<C> modelClass);

  /**
   * @param <C> the model type
   * @param entityType the entityType
   * @return the EntityModel based on the given entityType
   */
  <C extends M> C entityModel(EntityType entityType);

  /**
   * Refreshes all data models contained in this application model
   */
  void refresh();
}
