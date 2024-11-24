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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Configuration;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.observer.Mutable;
import is.codion.common.observer.Observer;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.exception.ValidationException;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Specifies a class for editing {@link Entity} instances.
 * The underlying attribute values are available via {@link #value(Attribute)}.
 */
public interface EntityEditModel {

	/**
	 * Specifies whether foreign key values should persist by default when defaults are set
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see EditableEntity#defaults()
	 */
	PropertyValue<Boolean> PERSIST_FOREIGN_KEYS = Configuration.booleanValue(EntityEditModel.class.getName() + ".persistForeignKeys", true);

	/**
	 * Specifies whether edit models post their insert, update and delete events to {@link EntityEditEvents}
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see #postEditEvents()
	 * @see EntityTableModel#HANDLE_EDIT_EVENTS
	 */
	PropertyValue<Boolean> POST_EDIT_EVENTS = Configuration.booleanValue(EntityEditModel.class.getName() + ".postEditEvents", true);

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
	 * Returns a {@link EditableEntity} wrapping the entity being edited. {@link EditableEntity#get()} returns
	 * an immutable copy of the {@link Entity} instance being edited, while {@link EditableEntity#set(Object)}
	 * copies the values from the given {@link Entity} into the underlying {@link Entity}.
	 * Note that value changes must go through the {@link EditableValue} accessible via {@link #value(Attribute)}.
	 * @return the {@link EditableEntity} wrapping the {@link Entity} instance being edited
	 * @see Entity#immutable()
	 */
	EditableEntity entity();

	/**
	 * Returns the {@link EditableValue} instance representing {@code attribute} in this edit model.
	 * @param attribute the attribute
	 * @param <T> the value type
	 * @return the {@link EditableValue} representing the given attribute
	 */
	<T> EditableValue<T> value(Attribute<T> attribute);

	/**
	 * @return the underlying domain entities
	 */
	Entities entities();

	/**
	 * @return the definition of the underlying entity
	 */
	EntityDefinition entityDefinition();

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
	 * Creates a {@link EntitySearchModel} for looking up entities referenced by the given foreign key,
	 * using the search attributes defined for that entity type.
	 * @param foreignKey the foreign key for which to create a {@link EntitySearchModel}
	 * @return a {@link EntitySearchModel} for looking up entities of the type referenced by the given foreign key attribute,
	 * @throws IllegalStateException in case no searchable attributes can be found for the entity type referenced by the given foreign key
	 */
	EntitySearchModel createForeignKeySearchModel(ForeignKey foreignKey);

	/**
	 * @param foreignKey the foreign key for which to retrieve the {@link EntitySearchModel}
	 * @return the {@link EntitySearchModel} associated with the {@code foreignKey}, if no search model
	 * has been initialized for the given foreign key, a new one is created, associated with the foreign key and returned.
	 */
	EntitySearchModel foreignKeySearchModel(ForeignKey foreignKey);

	/**
	 * @return a state controlling whether this edit model posts insert, update and delete events
	 * on the {@link EntityEditEvents} event bus.
	 * @see #POST_EDIT_EVENTS
	 */
	State postEditEvents();

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * Performs an insert on the active entity, sets the primary key values of the active entity
	 * according to the primary key of the inserted entity
	 * @return the inserted entity
	 * @throws DatabaseException in case of a database exception
	 * @throws ValidationException in case validation fails
	 * @throws IllegalStateException in case inserting is not enabled
	 * @see EntityValidator#validate(Entity)
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
	 * @see #beforeInsert()
	 * @see #afterInsert()
	 * @see EntityValidator#validate(Entity)
	 */
	Collection<Entity> insert(Collection<Entity> entities);

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * Performs an update on the active entity
	 * @return the updated entity
	 * @throws DatabaseException in case of a database exception
	 * @throws is.codion.common.db.exception.RecordModifiedException in case the entity has been modified since it was loaded
	 * @throws ValidationException in case validation fails
	 * @throws IllegalStateException in case updating is not enabled
	 * @throws is.codion.common.db.exception.UpdateException in case the active entity is not modified
	 * @see EntityValidator#validate(Entity)
	 */
	Entity update();

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * Updates the given entities.
	 * @param entities the entities to update
	 * @return the updated entities
	 * @throws DatabaseException in case of a database exception
	 * @throws is.codion.common.db.exception.RecordModifiedException in case an entity has been modified since it was loaded
	 * @throws ValidationException in case validation fails
	 * @throws IllegalStateException in case updating is not enabled
	 * @see #beforeUpdate()
	 * @see #afterUpdate()
	 * @see EntityValidator#validate(Entity)
	 */
	Collection<Entity> update(Collection<Entity> entities);

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * @return the deleted entity
	 * @throws DatabaseException in case of a database exception
	 * @throws IllegalStateException in case deleting is not enabled
	 * @see #beforeDelete()
	 * @see #afterDelete()
	 */
	Entity delete();

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * @param entities the entities to delete
	 * @return the deleted entities
	 * @throws DatabaseException in case of a database exception
	 * @throws IllegalStateException in case deleting is not enabled
	 * @see #beforeDelete()
	 * @see #afterDelete()
	 */
	Collection<Entity> delete(Collection<Entity> entities);

	/**
	 * Creates a new {@link Insert} instance for inserting the active entity.
	 * @return a new {@link Insert} instance
	 * @throws ValidationException in case validation fails
	 */
	Insert createInsert();

	/**
	 * Creates a new {@link Insert} instance for inserting the given entities.
	 * @param entities the entities to insert
	 * @return a new {@link Insert} instance
	 * @throws ValidationException in case validation fails
	 */
	Insert createInsert(Collection<Entity> entities);

	/**
	 * Creates a new {@link Update} instance for updating the active entity.
	 * @return a new {@link Update} instance
	 * @throws IllegalArgumentException in case the active entity is unmodified
	 * @throws ValidationException in case validation fails
	 */
	Update createUpdate();

	/**
	 * Creates a new {@link Update} instance for updating the given entities.
	 * @param entities the entities to update
	 * @return a new {@link Update} instance
	 * @throws IllegalArgumentException in case any of the given entities are unmodified
	 * @throws ValidationException in case validation fails
	 */
	Update createUpdate(Collection<Entity> entities);

	/**
	 * Creates a new {@link Delete} instance for deleting the active entity.
	 * @return a new {@link Delete} instance
	 */
	Delete createDelete();

	/**
	 * Creates a new {@link Delete} instance for deleting the given entities.
	 * @param entities the entities to delete
	 * @return a new {@link Delete} instance
	 */
	Delete createDelete(Collection<Entity> entities);

	/**
	 * Adds the given entities to all foreign key models based on that entity type
	 * @param foreignKey the foreign key
	 * @param entities the values
	 */
	void add(ForeignKey foreignKey, Collection<Entity> entities);

	/**
	 * Removes the given entities from all foreign key models based on that entity type and clears any foreign
	 * key values referencing them.
	 * @param foreignKey the foreign key
	 * @param entities the values
	 */
	void remove(ForeignKey foreignKey, Collection<Entity> entities);

	/**
	 * For every field referencing the given foreign key values, replaces that foreign key instance with
	 * the corresponding entity from {@code entities}, useful when attribute
	 * values have been changed in the referenced entity that must be reflected in the edit model.
	 * @param foreignKey the foreign key
	 * @param entities the foreign key entities
	 */
	void replace(ForeignKey foreignKey, Collection<Entity> entities);

	/**
	 * Validates the value associated with the given attribute, using the underlying validator.
	 * @param attribute the attribute the value is associated with
	 * @throws ValidationException if the given value is not valid for the given attribute
	 */
	void validate(Attribute<?> attribute);

	/**
	 * Validates the given entities, using the underlying validator.
	 * For entities of a type other than this edit model is based on,
	 * their respective validators are used.
	 * @param entities the entities to validate
	 * @throws ValidationException on finding the first invalid entity
	 * @see EntityDefinition#validator()
	 */
	void validate(Collection<Entity> entities);

	/**
	 * Validates the given entity, using the underlying validator.
	 * For entities of a type other than this edit model is based on,
	 * their respective validators are used.
	 * @param entity the entity to validate
	 * @throws ValidationException in case the entity is invalid
	 * @throws NullPointerException in case the entity is null
	 */
	void validate(Entity entity);

	/**
	 * @return an observer notified before an insert is performed
	 */
	Observer<Collection<Entity>> beforeInsert();

	/**
	 * @return an observer notified after an insert is performed
	 */
	Observer<Collection<Entity>> afterInsert();

	/**
	 * @return an observer notified before an update is performed
	 */
	Observer<Map<Entity.Key, Entity>> beforeUpdate();

	/**
	 * @return an observer notified after an update is performed
	 */
	Observer<Map<Entity.Key, Entity>> afterUpdate();

	/**
	 * @return an observer notified before a delete is performed
	 */
	Observer<Collection<Entity>> beforeDelete();

	/**
	 * @return an observer notified after a delete is performed
	 */
	Observer<Collection<Entity>> afterDelete();

	/**
	 * @return an observer notified each time one or more entities have been inserted, updated or deleted via this model
	 */
	Observer<?> afterInsertUpdateOrDelete();

	/**
	 * Provides access to the active entity being edited.
	 */
	interface EditableEntity extends Mutable<Entity> {

		/**
		 * Sets the given entity or defaults if null. Use {@link #clear()} in order to clear the entity of all values.
		 * @param entity the entity to set, if null, then defaults are set
		 */
		@Override
		void set(Entity entity);

		/**
		 * Clears all values from the underlying entity, disregarding the {@link EditableValue#persist()} directive.
		 */
		@Override
		void clear();

		/**
		 * Populates this edit model with default values for all attributes.
		 * @see EditableValue#defaultValue()
		 * @see EditableValue#persist()
		 * @see AttributeDefinition#defaultValue()
		 */
		void defaults();

		/**
		 * Refreshes the active Entity from the database, discarding all changes.
		 * If the active Entity is new then calling this method has no effect.
		 */
		void refresh();

		/**
		 * Reverts all attribute value changes.
		 */
		void revert();

		/**
		 * @return a {@link StateObserver} indicating whether the entity exists in the database
		 * @see Exists#predicate()
		 */
		Exists exists();

		/**
		 * Returns a {@link StateObserver} indicating whether any values have been modified.
		 * @return a {@link StateObserver} indicating the modified state of this entity
		 * @see Modified#predicate()
		 */
		Modified modified();

		/**
		 * @return a {@link StateObserver} indicating whether the entity has been edited, that is, exists and is modified
		 * @see #modified()
		 * @see #exists()
		 */
		StateObserver edited();

		/**
		 * @return an observer notified each time the entity is about to be changed
		 * via {@link EditableEntity#set(Object)} or {@link EditableEntity#defaults()}
		 * @see EditableEntity#set(Object)
		 * @see #defaults()
		 */
		Observer<Entity> changing();

		/**
		 * Returns an observer notified each time a value changes, either via its associated {@link EditableValue}
		 * instance or when the entity is set via {@link EditableEntity#set(Object)} or {@link EditableEntity#defaults()}.
		 * @return an observer notified each time a value changes
		 */
		Observer<Attribute<?>> valueChanged();

		/**
		 * @param attribute the attribute
		 * @return a {@link StateObserver} indicating whether the value of the given attribute is null
		 */
		StateObserver isNull(Attribute<?> attribute);

		/**
		 * @param attribute the attribute
		 * @return a {@link StateObserver} indicating whether the value of the given attribute is not null
		 */
		StateObserver isNotNull(Attribute<?> attribute);

		/**
		 * @return a {@link StateObserver} indicating whether the primary key of the entity is null
		 */
		StateObserver primaryKeyNull();

		/**
		 * @return a {@link StateObserver} indicating the valid status of the underlying Entity.
		 * @see #validate(Attribute)
		 * @see EntityValidator#validate(Entity)
		 */
		StateObserver valid();

		/**
		 * Controls the validator used by this edit model.
		 * @return the {@link Value} controlling the validator
		 * @see #validate(Entity)
		 */
		Value<EntityValidator> validator();

		/**
		 * Validates the current state of the entity
		 * @throws ValidationException in case the entity is invalid
		 */
		void validate();

		/**
		 * @param attribute the attribute
		 * @return true if this value is allowed to be null according to the validator
		 * @see #validator()
		 */
		boolean nullable(Attribute<?> attribute);

		/**
		 * Returns the {@link EditableValue} instance representing {@code attribute} in this {@link EditableEntity}.
		 * @param attribute the attribute
		 * @param <T> the value type
		 * @return the {@link EditableValue} representing the given attribute
		 */
		<T> EditableValue<T> value(Attribute<T> attribute);

		/**
		 * Indicates whether the active entity exists in the database.
		 * @see #predicate()
		 */
		interface Exists extends StateObserver {

			/**
			 * Controls the 'exists' predicate for this {@link Exists} instance, which is responsible for providing
			 * the exists state of the underlying entity.
			 * @return the {@link Value} controlling the predicate used to check if the entity exists
			 * @see EntityDefinition#exists()
			 * @see Entity#exists()
			 */
			Value<Predicate<Entity>> predicate();
		}

		/**
		 * Indicates whether the active entity is modified.
		 * @see #predicate()
		 */
		interface Modified extends StateObserver {

			/**
			 * Controls the 'modified' predicate for this {@link Modified} instance, which is responsible for providing
			 * the modified state of the underlying entity.
			 * @return the {@link Value} controlling the predicate used to check if the entity is modified
			 * @see Entity#modified()
			 */
			Value<Predicate<Entity>> predicate();

			/**
			 * Updates the modified state
			 */
			void update();
		}
	}

	/**
	 * Provides access the an {@link Attribute} value in the entity being edited.
	 * @param <T> the value type
	 */
	interface EditableValue<T> extends Value<T> {

		/**
		 * Reverts to the original value if modified
		 */
		void revert();

		/**
		 * Returns a {@link State} controlling whether the last used value for this attribute should persist when defaults are set.
		 * @return a {@link State} controlling whether the given attribute value should persist when defaults are set
		 * @see EditableEntity#defaults()
		 * @see EntityEditModel#PERSIST_FOREIGN_KEYS
		 */
		State persist();

		/**
		 * @return a {@link StateObserver} indicating the valid status of this attribute value.
		 */
		StateObserver valid();

		/**
		 * Returns a {@link StateObserver} instance indicating whether the value of the given attribute has been modified.
		 * @return a {@link StateObserver} indicating the modified state of the value of the given attribute
		 * @see EditableEntity#modified()
		 */
		StateObserver modified();

		/**
		 * Returns an observer notified each time this value is edited via {@link EditableValue#set(Object)}.
		 * <p>
		 * This event is not triggered when the value changes due to the entity being set
		 * via {@link EditableValue#set(Object)} or {@link EditableEntity#defaults()}.
		 * <p>
		 * Note that this event is only triggered if the value actually changes.
		 * @return an observer notified when the given attribute value is edited
		 */
		Observer<T> edited();

		/**
		 * Returns the {@link Value} instance controlling the default value supplier for the given attribute.
		 * Used when the underlying value is not persistent.
		 * Use {@link EditableEntity#defaults()} to populate the model with the default values.
		 * @return the {@link Value} instance controlling the default value supplier
		 * @see #persist()
		 */
		Value<Supplier<T>> defaultValue();
	}

	/**
	 * Represents a task for inserting entities, split up for use with a background thread.
	 * <pre>
	 * {@code
	 *   Insert insert = editModel.createInsert();
	 *
	 *   Insert.Task task = insert.prepare();
	 *
	 *   // Can safely be called in a background thread
	 *   Insert.Result result = task.perform();
	 *
	 *   Collection<Entity> insertedEntities = result.handle();
	 * }
	 * </pre>
	 * {@link Task#perform()} may be called on a background thread while {@link Insert#prepare()}
	 * and {@link Result#handle()} must be called on the UI thread.
	 */
	interface Insert {

		/**
		 * Notifies listeners that an insert is about to be performed.
		 * Must be called on the UI thread if this model has a panel based on it.
		 * @return the insert task
		 */
		Task prepare();

		/**
		 * The task performing the insert operation
		 */
		interface Task {

			/**
			 * May be called in a background thread.
			 * @return the insert result
			 */
			Result perform();
		}

		/**
		 * The insert task result
		 */
		interface Result {

			/**
			 * Notifies listeners that an insert has been performed.
			 * Must be called on the UI thread if this model has a panel based on it.
			 * @return the inserted entities
			 */
			Collection<Entity> handle();
		}
	}

	/**
	 * Represents a task for updating entities.
	 * <pre>
	 * {@code
	 *   Update update = editModel.createUpdate();
	 *
	 *   Update.Task task = update.prepare();
	 *
	 *   // Can safely be called in a background thread
	 *   Update.Result result = task.perform();
	 *
	 *   Collection<Entity> updatedEntities = result.handle();
	 * }
	 * </pre>
	 * {@link Task#perform()} may be called on a background thread while {@link Update#prepare()}
	 * and {@link Result#handle()} must be called on the UI thread.
	 */
	interface Update {

		/**
		 * Notifies listeners that an update is about to be performed.
		 * Must be called on the UI thread if this model has a panel based on it.
		 * @return the update task
		 */
		Task prepare();

		/**
		 * The task performing the update operation
		 */
		interface Task {

			/**
			 * May be called in a background thread.
			 * @return the update result
			 */
			Result perform();
		}

		/**
		 * The update task result
		 */
		interface Result {

			/**
			 * Notifies listeners that an update has been performed.
			 * Must be called on the UI thread if this model has a panel based on it.
			 * @return the updated entities
			 */
			Collection<Entity> handle();
		}
	}

	/**
	 * Represents a task for deleting entities.
	 * <pre>
	 * {@code
	 *   Delete delete = editModel.createDelete();
	 *
	 *   Delete.Task task = delete.prepare();
	 *
	 *   // Can safely be called in a background thread
	 *   Delete.Result result = task.perform();
	 *
	 *   Collection<Entity> deletedEntities = result.handle();
	 * }
	 * </pre>
	 * {@link Task#perform()} may be called on a background thread while {@link Delete#prepare()}
	 * and {@link Result#handle()} must be called on the UI thread.
	 */
	interface Delete {

		/**
		 * Notifies listeners that a delete is about to be performed.
		 * Must be called on the UI thread if this model has a panel based on it.
		 * @return the delete task
		 */
		Task prepare();

		/**
		 * The task performing the delete operation
		 */
		interface Task {

			/**
			 * May be called in a background thread.
			 * @return the delete result
			 */
			Result perform();
		}

		/**
		 * The delete task result
		 */
		interface Result {

			/**
			 * Notifies listeners that a delete has been performed.
			 * Must be called on the UI thread if this model has a panel based on it.
			 * @return the deleted entities
			 */
			Collection<Entity> handle();
		}
	}
}
