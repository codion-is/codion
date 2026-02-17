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
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.State;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.exception.EntityModifiedException;
import is.codion.framework.db.exception.UpdateEntityException;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.exception.ValidationException;

import java.util.Collection;
import java.util.Map;

import static is.codion.common.utilities.Configuration.booleanValue;

/**
 * Specifies a class for editing an {@link Entity} instance.
 * The underlying attribute values are available via {@link EntityEditor#value(Attribute)}.
 * @see #editor()
 */
public interface EntityEditModel {

	/**
	 * Specifies whether edit models publish their insert, update and delete events to {@link PersistenceEvents}
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see Settings#publishPersistenceEvents()
	 */
	PropertyValue<Boolean> PUBLISH_PERSISTENCE_EVENTS = booleanValue(EntityEditModel.class.getName() + ".publishPersistenceEvents", true);

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
	EntityEditor editor();

	/**
	 * @return the {@link EntityPersistence} used by this edit model
	 */
	EntityPersistence persistence();

	/**
	 * @return the {@link PersistTasks}
	 */
	PersistTasks tasks();

	/**
	 * @return the {@link PersistEvents}
	 */
	PersistEvents events();

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
	 * @throws ValidationException in case validation fails
	 * @throws IllegalStateException in case inserting is not enabled
	 * @see PersistEvents#beforeInsert()
	 * @see PersistEvents#afterInsert()
	 * @see Settings#insertEnabled()
	 * @see EntityEditor#validate(Entity)
	 */
	Entity insert();

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * Performs an insert on the given entities.
	 * @param entities the entities to insert
	 * @return a list containing the inserted entities
	 * @throws DatabaseException in case of a database exception
	 * @throws ValidationException in case validation fails
	 * @throws IllegalStateException in case inserting is not enabled
	 * @see PersistEvents#beforeInsert()
	 * @see PersistEvents#afterInsert()
	 * @see Settings#insertEnabled()
	 * @see EntityEditor#validate(Entity)
	 */
	Collection<Entity> insert(Collection<Entity> entities);

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * Performs an update on the active entity
	 * @return the updated entity
	 * @throws DatabaseException in case of a database exception
	 * @throws EntityModifiedException in case the entity has been modified since it was loaded
	 * @throws ValidationException in case validation fails
	 * @throws IllegalStateException in case updating is not enabled
	 * @throws UpdateEntityException in case the active entity is not modified
	 * @see PersistEvents#beforeUpdate()
	 * @see PersistEvents#afterUpdate()
	 * @see Settings#updateEnabled()
	 * @see EntityEditor#validate(Entity)
	 */
	Entity update();

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * Updates the given entities.
	 * @param entities the entities to update
	 * @return the updated entities
	 * @throws DatabaseException in case of a database exception
	 * @throws EntityModifiedException in case an entity has been modified since it was loaded
	 * @throws ValidationException in case validation fails
	 * @throws IllegalStateException in case updating is not enabled
	 * @throws UpdateEntityException in case any of the given entities are not modified
	 * @see PersistEvents#beforeUpdate()
	 * @see PersistEvents#afterUpdate()
	 * @see Settings#updateEnabled()
	 * @see EntityEditor#validate(Entity)
	 */
	Collection<Entity> update(Collection<Entity> entities);

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
	 * Provides {@link PersistTask}s
	 */
	interface PersistTasks {

		/**
		 * Creates a new {@link PersistTask} instance for inserting the active entity.
		 * @return a new {@link PersistTask} instance
		 * @throws IllegalStateException if inserting is not enabled
		 * @throws ValidationException in case validation fails
		 * @see EntityEditor#entity()
		 * @see Settings#insertEnabled()
		 */
		PersistTask insert();

		/**
		 * Creates a new {@link PersistTask} instance for inserting the given entity.
		 * @param entity the entity to insert
		 * @return a new {@link PersistTask} instance
		 * @throws IllegalStateException if inserting is not enabled
		 * @throws ValidationException in case validation fails
		 * @see Settings#insertEnabled()
		 */
		PersistTask insert(Entity entity);

		/**
		 * Creates a new {@link PersistTask} instance for inserting the given entities.
		 * @param entities the entities to insert
		 * @return a new {@link PersistTask} instance
		 * @throws IllegalStateException if inserting is not enabled
		 * @throws ValidationException in case validation fails
		 * @see Settings#insertEnabled()
		 */
		PersistTask insert(Collection<Entity> entities);

		/**
		 * Creates a new {@link PersistTask} instance for updating the active entity.
		 * @return a new {@link PersistTask} instance
		 * @throws IllegalStateException in case the active entity is unmodified or if updating is not enabled
		 * @throws ValidationException in case validation fails
		 * @see EntityEditor#entity()
		 * @see Settings#updateEnabled()
		 */
		PersistTask update();

		/**
		 * Creates a new {@link PersistTask} instance for updating the given entity.
		 * @param entity the entity to update
		 * @return a new {@link PersistTask} instance
		 * @throws IllegalStateException in case the given entity is unmodified or if updating is not enabled
		 * @throws ValidationException in case validation fails
		 * @see Settings#updateEnabled()
		 */
		PersistTask update(Entity entity);

		/**
		 * Creates a new {@link PersistTask} instance for updating the given entities.
		 * @param entities the entities to update
		 * @return a new {@link PersistTask} instance
		 * @throws IllegalStateException in case any of the given entities are unmodified or if updating is not enabled
		 * @throws ValidationException in case validation fails
		 * @see Settings#updateEnabled()
		 */
		PersistTask update(Collection<Entity> entities);

		/**
		 * Creates a new {@link PersistTask} instance for deleting the active entity.
		 * @return a new {@link PersistTask} instance
		 * @throws IllegalStateException if deleting is not enabled
		 * @see EntityEditor#entity()
		 * @see Settings#deleteEnabled()
		 */
		PersistTask delete();

		/**
		 * Creates a new {@link PersistTask} instance for deleting the given entity.
		 * @param entity the entity to delete
		 * @return a new {@link PersistTask} instance
		 * @throws IllegalStateException if deleting is not enabled
		 * @see Settings#deleteEnabled()
		 */
		PersistTask delete(Entity entity);

		/**
		 * Creates a new {@link PersistTask} instance for deleting the given entities.
		 * @param entities the entities to delete
		 * @return a new {@link PersistTask} instance
		 * @throws IllegalStateException if deleting is not enabled
		 * @see Settings#deleteEnabled()
		 */
		PersistTask delete(Collection<Entity> entities);
	}

	/**
	 * Provides event observers for the edit model.
	 */
	interface PersistEvents {

		/**
		 * @return an observer notified before insert is performed, after validation
		 */
		Observer<Collection<Entity>> beforeInsert();

		/**
		 * @return an observer notified after insert is performed
		 */
		Observer<Collection<Entity>> afterInsert();

		/**
		 * @return an observer notified before update is performed, after validation
		 */
		Observer<Collection<Entity>> beforeUpdate();

		/**
		 * @return an observer notified after update is performed, with the updated entities, mapped to their state before the update
		 */
		Observer<Map<Entity, Entity>> afterUpdate();

		/**
		 * @return an observer notified before delete is performed
		 */
		Observer<Collection<Entity>> beforeDelete();

		/**
		 * @return an observer notified after delete is performed
		 */
		Observer<Collection<Entity>> afterDelete();

		/**
		 * @return an observer notified each time one or more entities have been persisted, as in, inserted, updated or deleted via this model
		 */
		Observer<Collection<Entity>> persisted();
	}

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

		/**
		 * @return a state controlling whether this edit model publishes insert, update and delete events
		 * on the {@link PersistenceEvents} event bus.
		 * @see #PUBLISH_PERSISTENCE_EVENTS
		 */
		State publishPersistenceEvents();
	}

	/**
	 * Represents a task for persisting entities, inserting, updating or deleting, split up for use with a background thread.
	 * {@snippet :
	 *   PersistTask insert = editModel.createInsert();
	 *
	 *   PersistTask.Task task = insert.prepare();
	 *
	 *   // Can safely be called in a background thread
	 *   PersistTask.Result result = task.perform();
	 *
	 *   Collection<Entity> insertedEntities = result.handle();
	 *}
	 * {@link Task#perform()} may be called on a background thread while {@link PersistTask#prepare()}
	 * and {@link Result#handle()} must be called on the UI thread.
	 */
	interface PersistTask {

		/**
		 * Notifies listeners that an operation is about to be performed.
		 * Must be called on the UI thread if this model has a panel based on it.
		 * @return the task
		 */
		Task prepare();

		/**
		 * The task performing the operation
		 */
		interface Task {

			/**
			 * May be called in a background thread.
			 * @return the insert result
			 */
			Result perform();
		}

		/**
		 * The task result
		 */
		interface Result {

			/**
			 * Notifies listeners that the task has been performed.
			 * Must be called on the UI thread if this model has a panel based on it.
			 * @return the entities involved
			 */
			Collection<Entity> handle();
		}
	}
}
