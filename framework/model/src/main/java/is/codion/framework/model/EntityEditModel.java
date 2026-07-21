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
 * Copyright (c) 2009 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;

import java.util.prefs.Preferences;

/**
 * Represents the context for editing an {@link Entity} instance.
 * The underlying attribute values are available via {@link EntityEditor#value(Attribute)}.
 * @param <R> the {@link EntityEditor} type
 * @see #editor()
 */
public interface EntityEditModel<R extends EntityEditor<R>> {

	/**
	 * @return the type of the entity this edit model is based on
	 */
	EntityType entityType();

	/**
	 * @return the connection provider used by this edit model
	 */
	EntityConnectionProvider connectionProvider();

	/**
	 * Do not cache or keep the connection returned by this method in a long living field,
	 * since it may become invalid and thereby unusable.
	 * @return the connection used by this edit model
	 */
	EntityConnection connection();

	/**
	 * Returns a {@link EntityEditor} wrapping the entity being edited. {@link EntityEditor.EditorEntity#get()} returns
	 * an immutable copy of the {@link Entity} instance being edited, while {@link EntityEditor.EditorEntity#set(Entity)}
	 * copies the values from the given {@link Entity} into the underlying {@link Entity}.
	 * Note that value changes must go through the {@link EntityEditor.EditorValue} accessible via {@link EntityEditor#value(Attribute)}.
	 * @return the {@link EntityEditor} wrapping the {@link Entity} instance being edited
	 * @see Entity#immutable()
	 */
	R editor();

	/**
	 * @return the underlying domain entities
	 */
	Entities entities();

	/**
	 * @return the definition of the underlying entity
	 */
	EntityDefinition entityDefinition();

	/**
	 * Stores any custom persistent state to the given preferences node. Does nothing by default,
	 * override to persist edit model state along with the rest of the model tree.
	 * @param preferences the preferences node to store to
	 * @see #restore(Preferences)
	 */
	default void store(Preferences preferences) {}

	/**
	 * Restores any custom persistent state from the given preferences node. Does nothing by default,
	 * override to restore edit model state along with the rest of the model tree.
	 * @param preferences the preferences node to restore from
	 * @see #store(Preferences)
	 */
	default void restore(Preferences preferences) {}
}
