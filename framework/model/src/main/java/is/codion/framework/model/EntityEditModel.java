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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Specifies a class for editing {@link Entity} instances.
 */
public interface EntityEditModel {

	/**
	 * Specifies whether writable foreign key values should persist when the model is cleared or set to null<br>
	 * Value type: Boolean<br>
	 * Default value: true
	 */
	PropertyValue<Boolean> PERSIST_FOREIGN_KEYS = Configuration.booleanValue("is.codion.framework.model.EntityEditModel.persistForeignKeys", true);

	/**
	 * Indicates whether the application should ask for confirmation when exiting if some data is unsaved<br>
	 * and whether it should warn when unsaved data is about to be lost, i.e. due to selection changes.
	 * Value type: Boolean<br>
	 * Default value: false
	 */
	PropertyValue<Boolean> WARN_ABOUT_UNSAVED_DATA = Configuration.booleanValue("is.codion.framework.model.EntityEditModel.warnAboutUnsavedData", false);

	/**
	 * Specifies whether edit models post their insert, update and delete events to {@link EntityEditEvents}<br>
	 * Value type: Boolean<br>
	 * Default value: true
	 */
	PropertyValue<Boolean> EDIT_EVENTS = Configuration.booleanValue("is.codion.framework.model.EntityEditModel.editEvents", true);

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
	 * Populates this edit model with default values for all attributes.
	 * @see #defaultValue(Attribute)
	 * @see AttributeDefinition#defaultValue()
	 */
	void defaults();

	/**
	 * Copies the values from the given {@link Entity} into the underlying
	 * {@link Entity} being edited by this edit model. If {@code entity} is null
	 * the effect is the same as calling {@link #defaults()}.
	 * @param entity the entity
	 * @see #defaults()
	 */
	void set(Entity entity);

	/**
	 * Refreshes the active Entity from the database, discarding all changes.
	 * If the active Entity is new then calling this method has no effect.
	 */
	void refreshEntity();

	/**
	 * @return an immutable version of the {@link Entity} instance being edited
	 * @see Entity#immutable()
	 */
	Entity entity();

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
	 * @param attribute the attribute
	 * @return true if this value is allowed to be null in the underlying entity
	 */
	boolean nullable(Attribute<?> attribute);

	/**
	 * Sets the given value in the underlying Entity
	 * @param attribute the attribute to associate the given value with
	 * @param value the value to associate with the given attribute
	 * @param <T> the value type
	 * @return the previous value, if any
	 */
	<T> T put(Attribute<T> attribute, T value);

	/**
	 * Removes the given value from the underlying Entity
	 * @param attribute the attribute
	 * @param <T> the value type
	 * @return the value, if any
	 */
	<T> T remove(Attribute<T> attribute);

	/**
	 * Returns the value associated with the given attribute
	 * @param attribute the attribute
	 * @param <T> the value type
	 * @return the value associated with the given attribute
	 */
	<T> T get(Attribute<T> attribute);

	/**
	 * Returns the value associated with the given attribute
	 * @param attribute the attribute
	 * @param <T> the value type
	 * @return the value associated with the given attribute, an empty Optional in case it is null
	 */
	<T> Optional<T> optional(Attribute<T> attribute);

	/**
	 * Returns the value associated with the given foreign key.
	 * @param foreignKey the foreign key
	 * @return the foreign key value
	 */
	Entity referencedEntity(ForeignKey foreignKey);

	/**
	 * Returns a Value based on {@code attribute} in this edit model, note that
	 * subsequent calls for the same attribute return the same value instance.
	 * @param attribute the attribute
	 * @param <T> the value type
	 * @return a Value based on the given edit model value
	 */
	<T> Value<T> value(Attribute<T> attribute);

	/**
	 * @return the underlying domain entities
	 */
	Entities entities();

	/**
	 * @return the definition of the underlying entity
	 */
	EntityDefinition entityDefinition();

	/**
	 * @return a state controlling whether this edit model triggers a warning before overwriting unsaved data
	 * @see #WARN_ABOUT_UNSAVED_DATA
	 */
	State overwriteWarning();

	/**
	 * Making this edit model read-only prevents any changes from being
	 * persisted to the database, trying to insert, update or delete will
	 * cause an exception being thrown, it does not prevent editing.
	 * Use {@link #insertEnabled()}, {@link #updateEnabled()} and {@link #deleteEnabled()}
	 * to configure the enabled state of those specific actions.
	 * @return the State controlling whether this model is read only
	 */
	State readOnly();

	/**
	 * Disabling insert causes an exception being thrown when inserting.
	 * @return the state controlling whether inserting is enabled via this edit model
	 */
	State insertEnabled();

	/**
	 * Disabling update causes an exception being thrown when updating.
	 * @return the state controlling whether updating is enabled via this edit model
	 */
	State updateEnabled();

	/**
	 * Disabling updating multiple entities causes an exception being thrown when
	 * trying to update multiple entities at a time.
	 * @return the state controlling whether updating multiple entities is enabled
	 */
	State updateMultipleEnabled();

	/**
	 * Disabling delete causes an exception being thrown when deleting.
	 * @return the state controlling whether deleting is enabled via this edit model
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
	 * Returns the {@link Value} instance controlling the default value supplier for the given attribute.
	 * Used when the underlying value is not persistent. Use {@link #defaults()} or {@link #set(Entity)}
	 * with a null parameter to populate the model with the default values.
	 * @param attribute the attribute
	 * @param <S> the value supplier type
	 * @param <T> the value type
	 * @return the {@link Value} instance controlling the default value supplier
	 * @see #persist(Attribute)
	 */
	<S extends Supplier<T>, T> Value<S> defaultValue(Attribute<T> attribute);

	/**
	 * @return a state controlling whether this edit model posts insert, update and delete events
	 * on the {@link EntityEditEvents} event bus.
	 * @see #EDIT_EVENTS
	 */
	State editEvents();

	/**
	 * Returns a State controlling whether the last used value for this attribute should persist when the model is cleared.
	 * @param attribute the attribute
	 * @return a State controlling whether the given attribute value should persist when the model is cleared
	 * @see EntityEditModel#PERSIST_FOREIGN_KEYS
	 */
	State persist(Attribute<?> attribute);

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
	Entity insert() throws DatabaseException, ValidationException;

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * Performs an insert on the given entities.
	 * @param entities the entities to insert
	 * @return a list containing the inserted entities
	 * @throws DatabaseException in case of a database exception
	 * @throws ValidationException in case validation fails
	 * @throws IllegalStateException in case inserting is not enabled
	 * @see #addBeforeInsertListener(Consumer)
	 * @see #addAfterInsertListener(Consumer)
	 * @see EntityValidator#validate(Entity)
	 */
	Collection<Entity> insert(Collection<Entity> entities) throws DatabaseException, ValidationException;

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
	Entity update() throws DatabaseException, ValidationException;

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * Updates the given entities.
	 * @param entities the entities to update
	 * @return the updated entities
	 * @throws DatabaseException in case of a database exception
	 * @throws is.codion.common.db.exception.RecordModifiedException in case an entity has been modified since it was loaded
	 * @throws ValidationException in case validation fails
	 * @throws IllegalStateException in case updating is not enabled
	 * @see #addBeforeUpdateListener(Consumer)
	 * @see #addAfterUpdateListener(Consumer)
	 * @see EntityValidator#validate(Entity)
	 */
	Collection<Entity> update(Collection<Entity> entities) throws DatabaseException, ValidationException;

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * @throws DatabaseException in case of a database exception
	 * @throws IllegalStateException in case deleting is not enabled
	 * @see #addBeforeDeleteListener(Consumer)
	 * @see #addAfterDeleteListener(Consumer)
	 */
	void delete() throws DatabaseException;

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * @param entities the entities to delete
	 * @throws DatabaseException in case of a database exception
	 * @throws IllegalStateException in case deleting is not enabled
	 * @see #addBeforeDeleteListener(Consumer)
	 * @see #addAfterDeleteListener(Consumer)
	 */
	void delete(Collection<Entity> entities) throws DatabaseException;

	/**
	 * Creates a new {@link Insert} instance for inserting the active entity.
	 * @return a new {@link Insert} instance
	 * @throws ValidationException in case validation fails
	 */
	Insert createInsert() throws ValidationException;

	/**
	 * Creates a new {@link Insert} instance for inserting the given entities.
	 * @param entities the entities to insert
	 * @return a new {@link Insert} instance
	 * @throws ValidationException in case validation fails
	 */
	Insert createInsert(Collection<Entity> entities) throws ValidationException;

	/**
	 * Creates a new {@link Update} instance for updating the active entity.
	 * @return a new {@link Update} instance
	 * @throws IllegalArgumentException in case the active entity is unmodified
	 * @throws ValidationException in case validation fails
	 */
	Update createUpdate() throws ValidationException;

	/**
	 * Creates a new {@link Update} instance for updating the given entities.
	 * @param entities the entities to update
	 * @return a new {@link Update} instance
	 * @throws IllegalArgumentException in case any of the given entities are unmodified
	 * @throws ValidationException in case validation fails
	 */
	Update createUpdate(Collection<Entity> entities) throws ValidationException;

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
	void validate(Attribute<?> attribute) throws ValidationException;

	/**
	 * Validates the current state of the entity
	 * @throws ValidationException in case the entity is invalid
	 */
	void validate() throws ValidationException;

	/**
	 * Validates the given entities, using the underlying validator.
	 * For entities of a type other than this edit model is based on,
	 * their respective validators are used.
	 * @param entities the entities to validate
	 * @throws ValidationException on finding the first invalid entity
	 * @see EntityDefinition#validator()
	 */
	void validate(Collection<Entity> entities) throws ValidationException;

	/**
	 * Validates the given entity, using the underlying validator.
	 * For entities of a type other than this edit model is based on,
	 * their respective validators are used.
	 * @param entity the entity to validate
	 * @throws ValidationException in case the entity is invalid
	 * @throws NullPointerException in case the entity is null
	 */
	void validate(Entity entity) throws ValidationException;

	/**
	 * @return a {@link StateObserver} indicating the valid status of the underlying Entity.
	 * @see #validate(Attribute)
	 * @see EntityValidator#validate(Entity)
	 */
	StateObserver valid();

	/**
	 * @param attribute the attribute
	 * @return a {@link StateObserver} indicating the valid status of the given attribute.
	 */
	StateObserver valid(Attribute<?> attribute);

	/**
	 * Returns a {@link StateObserver} indicating when and if any values in the underlying Entity have been modified.
	 * @return a {@link StateObserver} indicating the modified state of this edit model
	 */
	StateObserver modified();

	/**
	 * Returns a {@link StateObserver} instance indicating whether the value of the given attribute has been modified.
	 * @param attribute the attribute
	 * @return a {@link StateObserver} indicating the modified state of the value of the given attribute
	 * @throws IllegalArgumentException in case attribute is not part of the underlying entity
	 * @see #modified()
	 */
	StateObserver modified(Attribute<?> attribute);

	/**
	 * @return a {@link StateObserver} indicating whether the active entity exists in the database
	 */
	StateObserver exists();

	/**
	 * @return a {@link StateObserver} indicating whether the primary key of the active entity is null
	 */
	StateObserver primaryKeyNull();

	/**
	 * Adds a listener notified each time the value associated with the given attribute is edited via
	 * {@link #put(Attribute, Object)} or {@link #remove(Attribute)}, note that this event is only fired
	 * if the value actually changes.
	 * @param attribute the attribute for which to monitor value edits
	 * @param listener a listener notified each time the value of the given attribute is edited via this model
	 * @param <T> the value type
	 */
	<T> void addEditListener(Attribute<T> attribute, Consumer<T> listener);

	/**
	 * Removes the given listener.
	 * @param attribute the attribute
	 * @param listener the listener to remove
	 * @param <T> the value type
	 */
	<T> void removeEditListener(Attribute<T> attribute, Consumer<T> listener);

	/**
	 * Adds a listener notified each time the value associated with the given attribute changes, either
	 * via editing or when the active entity is set.
	 * @param attribute the attribute for which to monitor value changes
	 * @param listener a listener notified each time the value of the {@code attribute} changes
	 * @param <T> the value type
	 * @see #set(Entity)
	 */
	<T> void addValueListener(Attribute<T> attribute, Consumer<T> listener);

	/**
	 * Removes the given listener.
	 * @param attribute the attribute for which to remove the listener
	 * @param listener the listener to remove
	 * @param <T> the value type
	 */
	<T> void removeValueListener(Attribute<T> attribute, Consumer<T> listener);

	/**
	 * @param listener a listener notified each time a value changes, providing the attribute
	 */
	void addValueChangeListener(Consumer<Attribute<?>> listener);

	/**
	 * @param listener the listener to remove
	 */
	void removeValueChangeListener(Consumer<Attribute<?>> listener);

	/**
	 * Notified each time the entity is set via {@link #set(Entity)} or {@link #defaults()}.
	 * @param listener a listener notified each time the entity is set, possibly to null
	 * @see #set(Entity)
	 * @see #defaults()
	 */
	void addEntityListener(Consumer<Entity> listener);

	/**
	 * Removes the given listener.
	 * @param listener the listener to remove
	 */
	void removeEntityListener(Consumer<Entity> listener);

	/**
	 * @param listener a listener to be notified before an insert is performed
	 */
	void addBeforeInsertListener(Consumer<Collection<Entity>> listener);

	/**
	 * Removes the given listener.
	 * @param listener a listener to remove
	 */
	void removeBeforeInsertListener(Consumer<Collection<Entity>> listener);

	/**
	 * @param listener a listener to be notified each time insert has been performed
	 */
	void addAfterInsertListener(Consumer<Collection<Entity>> listener);

	/**
	 * Removes the given listener.
	 * @param listener a listener to remove
	 */
	void removeAfterInsertListener(Consumer<Collection<Entity>> listener);

	/**
	 * @param listener a listener to be notified before an update is performed
	 */
	void addBeforeUpdateListener(Consumer<Map<Entity.Key, Entity>> listener);

	/**
	 * Removes the given listener.
	 * @param listener a listener to remove
	 */
	void removeBeforeUpdateListener(Consumer<Map<Entity.Key, Entity>> listener);

	/**
	 * @param listener a listener to be notified each time an update has been performed,
	 * with the updated entities, mapped to their respective original primary keys, that is,
	 * the primary keys before the update was performed
	 */
	void addAfterUpdateListener(Consumer<Map<Entity.Key, Entity>> listener);

	/**
	 * Removes the given listener.
	 * @param listener a listener to remove
	 */
	void removeAfterUpdateListener(Consumer<Map<Entity.Key, Entity>> listener);

	/**
	 * @param listener a listener to be notified before a delete is performed
	 */
	void addBeforeDeleteListener(Consumer<Collection<Entity>> listener);

	/**
	 * Removes the given listener.
	 * @param listener a listener to remove
	 */
	void removeBeforeDeleteListener(Consumer<Collection<Entity>> listener);

	/**
	 * @param listener a listener to be notified each time delete has been performed
	 */
	void addAfterDeleteListener(Consumer<Collection<Entity>> listener);

	/**
	 * Removes the given listener.
	 * @param listener a listener to remove
	 */
	void removeAfterDeleteListener(Consumer<Collection<Entity>> listener);

	/**
	 * @param listener a listener notified each time one or more entities are updated, inserted or deleted via this model
	 */
	void addInsertUpdateOrDeleteListener(Runnable listener);

	/**
	 * Removes the given listener.
	 * @param listener a listener to remove
	 */
	void removeInsertUpdateOrDeleteListener(Runnable listener);

	/**
	 * @param listener a listener notified each time the active entity is about to be set
	 */
	void addConfirmOverwriteListener(Consumer<State> listener);

	/**
	 * Removes the given listener.
	 * @param listener a listener to remove
	 */
	void removeConfirmOverwriteListener(Consumer<State> listener);

	/**
	 * Represents a task for inserting entities, split up for use with a background thread.
	 * <pre>
	 *   Insert insert = editModel.createInsert();
	 *
	 *   Insert.Task task = insert.prepare();
	 *
	 *   // Can safely be called in a background thread
	 *   Insert.Result result = task.perform();
	 *
	 *   Collection&lt;Entity&gt; insertedEntities = result.handle();
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
			 * @throws DatabaseException in case of a database exception
			 */
			Result perform() throws DatabaseException;
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
	 *   Update update = editModel.createUpdate();
	 *
	 *   Update.Task task = update.prepare();
	 *
	 *   // Can safely be called in a background thread
	 *   Update.Result result = task.perform();
	 *
	 *   Collection&lt;Entity&gt; updatedEntities = result.handle();
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
			 * @throws DatabaseException in case of a database exception
			 */
			Result perform() throws DatabaseException;
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
	 *   Delete delete = editModel.createDelete();
	 *
	 *   Delete.Task task = delete.prepare();
	 *
	 *   // Can safely be called in a background thread
	 *   Delete.Result result = task.perform();
	 *
	 *   result.handle();
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
			 * @throws DatabaseException in case of a database exception
			 */
			Result perform() throws DatabaseException;
		}

		/**
		 * The delete task result
		 */
		interface Result {

			/**
			 * Notifies listeners that a delete has been performed.
			 * Must be called on the UI thread if this model has a panel based on it.
			 */
			void handle();
		}
	}
}
