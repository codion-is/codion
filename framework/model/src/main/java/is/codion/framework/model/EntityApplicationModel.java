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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.utilities.user.User;
import is.codion.common.utilities.version.Version;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityType;

import java.util.Collection;
import java.util.Optional;
import java.util.prefs.Preferences;

import static is.codion.common.utilities.Configuration.booleanValue;
import static is.codion.common.utilities.Configuration.stringValue;

/**
 * A central application model class.
 * @param <M> the type of {@link EntityModel} this application model is based on
 * @param <E> the type of {@link EntityEditModel} used by this {@link EntityModel}
 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
 */
public interface EntityApplicationModel<M extends EntityModel<M, E, T>, E extends EntityEditModel, T extends EntityTableModel<E>> {

	/**
	 * Specifies whether the client should apply and save user preferences
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> USER_PREFERENCES =
					booleanValue(EntityApplicationModel.class.getName() + ".userPreferences", true);

	/**
	 * Specifies whether the application should restore default preferences, that is, not load any saved user preferences.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	PropertyValue<Boolean> RESTORE_DEFAULT_PREFERENCES =
					booleanValue(EntityApplicationModel.class.getName() + ".restoreDefaultPreferences", false);

	/**
	 * Specifies the key to use when creating file based application preferences.
	 * Note that this string may only contain valid filename characters and symbols.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null
	 * </ul>
	 */
	PropertyValue<String> PREFERENCES_KEY = stringValue("codion.client.preferencesKey");

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
	 * @return the application preferences instance
	 * @see #PREFERENCES_KEY
	 */
	Preferences preferences();

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
