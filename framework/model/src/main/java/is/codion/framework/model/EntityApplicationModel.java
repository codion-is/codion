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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.property.PropertyValue;
import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityType;

import java.util.List;
import java.util.Optional;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.Configuration.stringValue;

/**
 * A central application model class.
 * @param <E> the type of {@link EntityEditModel}
 * @param <T> the type of {@link EntityTableModel}
 */
public interface EntityApplicationModel<E extends EntityEditModel, T extends EntityTableModel<E>> {

	/**
	 * Specifies a string to prepend to the username field in the login dialog
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: [empty string]
	 * </ul>
	 */
	PropertyValue<String> USERNAME_PREFIX = stringValue("codion.client.usernamePrefix", "");

	/**
	 * Specifies whether user authentication is required
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> AUTHENTICATION_REQUIRED = booleanValue("codion.client.authenticationRequired", true);

	/**
	 * Specifies whether the client saves the last successful login username,
	 * which is then displayed as the default username the next time the application is started
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> SAVE_DEFAULT_USERNAME = booleanValue("codion.client.saveDefaultUsername", true);

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
	 * @return the {@link EntityModels}
	 */
	EntityModels<E, T> entityModels();

	/**
	 * Refreshes all data models contained in this application model
	 */
	void refresh();

	/**
	 * Manages the {@link EntityModel}s for a {@link EntityApplicationModel}
	 * @param <E> the type of {@link EntityEditModel}
	 * @param <T> the type of {@link EntityTableModel}
	 */
	interface EntityModels<E extends EntityEditModel, T extends EntityTableModel<E>> {

		/**
		 * Adds the given entity models to this model.
		 * @param entityModels the entity models to add
		 * @throws IllegalArgumentException in case any of the models has already been added
		 */
		void add(EntityModel<E, T>... entityModels);

		/**
		 * Adds the given entity model to this model
		 * @param entityModel the detail model
		 * @throws IllegalArgumentException in case the model has already been added
		 */
		void add(EntityModel<E, T> entityModel);

		/**
		 * @param modelClass the application model class
		 * @return true if this model contains a EntityModel instance of the given class
		 */
		boolean contains(Class<? extends EntityModel<E, T>> modelClass);

		/**
		 * @param entityType the entityType
		 * @return true if this model contains a EntityModel for the given entityType
		 */
		boolean contains(EntityType entityType);

		/**
		 * @param entityModel the entity model
		 * @return true if this model contains the given EntityModel
		 */
		boolean contains(EntityModel<E, T> entityModel);

		/**
		 * @return an unmodifiable List containing the EntityModel instances contained
		 * in this EntityApplicationModel
		 */
		List<EntityModel<E, T>> get();

		/**
		 * @param <C> the model type
		 * @param modelClass the model class
		 * @return the EntityModel of the given type
		 */
		<C extends EntityModel<E, T>> C get(Class<C> modelClass);

		/**
		 * @param <C> the model type
		 * @param entityType the entityType
		 * @return the EntityModel based on the given entityType
		 */
		<C extends EntityModel<E, T>> C get(EntityType entityType);
	}
}
