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

import java.util.Collection;
import java.util.Optional;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.Configuration.stringValue;

/**
 * A central application model class.
 * @param <M> the type of {@link EntityModel} this application model is based on
 * @param <E> the type of {@link EntityEditModel} used by this {@link EntityModel}
 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
 */
public interface EntityApplicationModel<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> {

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
	 * Specifies the user for logging into the application on the form {@code user:password}.
	 * If one is specified no login dialog is presented.
	 * <p>
	 * Initialized with the value of the CODION_CLIENT_USER environment variable.
	 * <p>
	 * <strong>Warning:</strong> System properties are visible in process listings.
	 * Use only for development/testing. In production, use secure credential management.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: System.getenv("CODION_CLIENT_USER")
	 * </ul>
	 * @see User#parse(String)
	 */
	PropertyValue<String> USER = stringValue("codion.client.user", System.getenv("CODION_CLIENT_USER"));

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
	EntityModels<M, E, T> entityModels();

	/**
	 * Refreshes all data models contained in this application model
	 */
	void refresh();

	/**
	 * Manages the {@link EntityModel}s for a {@link EntityApplicationModel}
	 * @param <M> the type of {@link EntityModel} this application model is based on
	 * @param <E> the type of {@link EntityEditModel} used by this {@link EntityModel}
	 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
	 */
	interface EntityModels<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> {

		/**
		 * @param modelClass the application model class
		 * @return true if this model contains a EntityModel instance of the given class
		 */
		boolean contains(Class<? extends M> modelClass);

		/**
		 * @param entityType the entityType
		 * @return true if this model contains a EntityModel for the given entityType
		 */
		boolean contains(EntityType entityType);

		/**
		 * @param entityModel the entity model
		 * @return true if this model contains the given EntityModel
		 */
		boolean contains(M entityModel);

		/**
		 * @return an unmodifiable Collection containing the EntityModel instances contained
		 * in this EntityApplicationModel
		 */
		Collection<M> get();

		/**
		 * @param <C> the model type
		 * @param modelClass the model class
		 * @return the EntityModel of the given type
		 */
		<C extends M> C get(Class<C> modelClass);

		/**
		 * @param entityType the entityType
		 * @return the EntityModel based on the given entityType
		 */
		M get(EntityType entityType);
	}
}
