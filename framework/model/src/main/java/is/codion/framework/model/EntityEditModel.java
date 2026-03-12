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

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.reactive.state.State;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.exception.EntityModifiedException;
import is.codion.framework.db.exception.UpdateEntityException;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.framework.model.EntityEditor.PersistEvents;
import is.codion.framework.model.EntityEditor.PersistTasks;

import java.util.Collection;

/**
 * Specifies a class for editing an {@link Entity} instance.
 * The underlying attribute values are available via {@link EntityEditor#value(Attribute)}.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 * @param <R> the {@link EntityEditor} type
 * @see #editor()
 */
public interface EntityEditModel<M extends EntityModel<M, E, T, R>, E extends EntityEditModel<M, E, T, R>,
				T extends EntityTableModel<M, E, T, R>, R extends EntityEditor<M, E, T, R>> {

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
	 * Note that the task builder methods throw an {@link IllegalStateException} in case the requested action is disabled.
	 * @return the {@link PersistTasks}
	 * @see Settings#insertEnabled()
	 * @see Settings#updateEnabled()
	 * @see Settings#deleteEnabled()
	 */
	PersistTasks tasks();

	/**
	 * @return the underlying domain entities
	 */
	Entities entities();

	/**
	 * @return the definition of the underlying entity
	 */
	EntityDefinition entityDefinition();

	/**
	 * @return the edit model settings
	 */
	Settings settings();

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * Performs an insert on the active entity, sets the primary key values of the active entity
	 * according to the primary key of the inserted entity
	 * @return the inserted entity
	 * @throws DatabaseException in case of a database exception
	 * @throws EntityValidationException in case validation fails
	 * @throws IllegalStateException in case inserting is not enabled
	 * @see PersistEvents#beforeInsert()
	 * @see PersistEvents#afterInsert()
	 * @see Settings#insertEnabled()
	 * @see EntityEditor#validate(Entity)
	 */
	Entity insert() throws EntityValidationException;

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * Performs an insert on the given entities.
	 * @param entities the entities to insert
	 * @return a list containing the inserted entities
	 * @throws DatabaseException in case of a database exception
	 * @throws EntityValidationException in case validation fails
	 * @throws IllegalStateException in case inserting is not enabled
	 * @see PersistEvents#beforeInsert()
	 * @see PersistEvents#afterInsert()
	 * @see Settings#insertEnabled()
	 * @see EntityEditor#validate(Entity)
	 */
	Collection<Entity> insert(Collection<Entity> entities) throws EntityValidationException;

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * Performs an update on the active entity
	 * @return the updated entity
	 * @throws DatabaseException in case of a database exception
	 * @throws EntityModifiedException in case the entity has been modified since it was loaded
	 * @throws EntityValidationException in case validation fails
	 * @throws IllegalStateException in case updating is not enabled
	 * @throws UpdateEntityException in case the active entity is not modified
	 * @see PersistEvents#beforeUpdate()
	 * @see PersistEvents#afterUpdate()
	 * @see Settings#updateEnabled()
	 * @see EntityEditor#validate(Entity)
	 */
	Entity update() throws EntityValidationException;

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * Updates the given entities.
	 * @param entities the entities to update
	 * @return the updated entities
	 * @throws DatabaseException in case of a database exception
	 * @throws EntityModifiedException in case an entity has been modified since it was loaded
	 * @throws EntityValidationException in case validation fails
	 * @throws IllegalStateException in case updating is not enabled
	 * @throws UpdateEntityException in case any of the given entities are not modified
	 * @see PersistEvents#beforeUpdate()
	 * @see PersistEvents#afterUpdate()
	 * @see Settings#updateEnabled()
	 * @see EntityEditor#validate(Entity)
	 */
	Collection<Entity> update(Collection<Entity> entities) throws EntityValidationException;

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * @return the deleted entity
	 * @throws DatabaseException in case of a database exception
	 * @throws IllegalStateException in case deleting is not enabled
	 * @see PersistEvents#beforeDelete()
	 * @see PersistEvents#afterDelete()
	 * @see Settings#deleteEnabled()
	 */
	Entity delete();

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * @param entities the entities to delete
	 * @return the deleted entities
	 * @throws DatabaseException in case of a database exception
	 * @throws IllegalStateException in case deleting is not enabled
	 * @see PersistEvents#beforeDelete()
	 * @see PersistEvents#afterDelete()
	 * @see Settings#deleteEnabled()
	 */
	Collection<Entity> delete(Collection<Entity> entities);

	/**
	 * The edit model settings.
	 */
	interface Settings {

		/**
		 * Making this edit model read-only prevents any changes from being
		 * persisted to the database, trying to insert, update or delete will
		 * cause an exception being thrown, it does not prevent editing.
		 * Use {@link #insertEnabled()}, {@link #updateEnabled()} and {@link #deleteEnabled()}
		 * to configure the enabled state of those specific actions.
		 * @return the {@link State} controlling whether this model is read only
		 */
		State readOnly();

		/**
		 * Disabling insert causes an exception being thrown when inserting.
		 * @return the {@link State} controlling whether inserting is enabled via this edit model
		 */
		State insertEnabled();

		/**
		 * Disabling update causes an exception being thrown when updating.
		 * @return the {@link State} controlling whether updating is enabled via this edit model
		 */
		State updateEnabled();

		/**
		 * Disabling updating multiple entities causes an exception being thrown when
		 * trying to update multiple entities at a time.
		 * @return the {@link State} controlling whether updating multiple entities is enabled
		 */
		State updateMultipleEnabled();

		/**
		 * Disabling delete causes an exception being thrown when deleting.
		 * @return the {@link State} controlling whether deleting is enabled via this edit model
		 */
		State deleteEnabled();
	}
}
