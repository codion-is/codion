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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.Event;
import is.codion.common.observer.Observable;
import is.codion.common.observer.Observer;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
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
import is.codion.framework.model.AbstractEntityEditModel.DefaultEditEvents;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.Configuration.booleanValue;
import static java.util.Objects.requireNonNull;

/**
 * Specifies a class for editing {@link Entity} instances.
 * The underlying attribute values are available via {@link EntityEditor#value(Attribute)}.
 * @see #editor()
 */
public interface EntityEditModel {

	/**
	 * Specifies whether edit models post their insert, update and delete events to {@link EditEvents}
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see #editEvents()
	 */
	PropertyValue<Boolean> EDIT_EVENTS = booleanValue(EntityEditModel.class.getName() + ".editEvents", true);

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
	 * Returns a {@link EntityEditor} wrapping the entity being edited. {@link EntityEditor#get()} returns
	 * an immutable copy of the {@link Entity} instance being edited, while {@link EntityEditor#set(Entity)}
	 * copies the values from the given {@link Entity} into the underlying {@link Entity}.
	 * Note that value changes must go through the {@link EditorValue} accessible via {@link EntityEditor#value(Attribute)}.
	 * @return the {@link EntityEditor} wrapping the {@link Entity} instance being edited
	 * @see Entity#immutable()
	 */
	EntityEditor editor();

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
	 * <p>Creates a {@link EntitySearchModel} for looking up entities of the type referenced by the given foreign key,
	 * using the search attributes defined for that entity type.
	 * @param foreignKey the foreign key for which to create a {@link EntitySearchModel}
	 * @return a new {@link EntitySearchModel} for looking up entities of the type referenced by the given foreign key attribute,
	 * @throws IllegalStateException in case no searchable attributes can be found for the entity type referenced by the given foreign key
	 */
	EntitySearchModel createSearchModel(ForeignKey foreignKey);

	/**
	 * <p>Returns the {@link EntitySearchModel} associated with the given foreign key.
	 * If no such search model exists, one is created by calling {@link #createSearchModel(ForeignKey)}.
	 * <p>This method always returns the same {@link EntitySearchModel} instance, once one has been created.
	 * @param foreignKey the foreign key for which to retrieve the {@link EntitySearchModel}
	 * @return the {@link EntitySearchModel} associated with the given foreign key
	 */
	EntitySearchModel searchModel(ForeignKey foreignKey);

	/**
	 * @return a state controlling whether this edit model posts insert, update and delete events
	 * on the {@link EditEvents} event bus.
	 * @see #EDIT_EVENTS
	 */
	State editEvents();

	/**
	 * Refreshes the active Entity from the database, discarding all changes.
	 * If the active Entity is new then calling this method has no effect.
	 */
	void refresh();

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * Performs an insert on the active entity, sets the primary key values of the active entity
	 * according to the primary key of the inserted entity
	 * @return the inserted entity
	 * @throws DatabaseException in case of a database exception
	 * @throws ValidationException in case validation fails
	 * @throws IllegalStateException in case inserting is not enabled
	 * @see #beforeInsert()
	 * @see #afterInsert()
	 * @see #insertEnabled()
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
	 * @see #beforeInsert()
	 * @see #afterInsert()
	 * @see #insertEnabled()
	 * @see EntityEditor#validate(Entity)
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
	 * @see #beforeUpdate()
	 * @see #afterUpdate()
	 * @see #updateEnabled()
	 * @see EntityEditor#validate(Entity)
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
	 * @throws is.codion.common.db.exception.UpdateException in case any of the given entities are not modified
	 * @see #beforeUpdate()
	 * @see #afterUpdate()
	 * @see #updateEnabled()
	 * @see EntityEditor#validate(Entity)
	 */
	Collection<Entity> update(Collection<Entity> entities);

	/**
	 * Note: This method must be called on the UI thread in case a panel has been based on this model.
	 * @return the deleted entity
	 * @throws DatabaseException in case of a database exception
	 * @throws IllegalStateException in case deleting is not enabled
	 * @see #beforeDelete()
	 * @see #afterDelete()
	 * @see #deleteEnabled()
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
	 * @see #deleteEnabled()
	 */
	Collection<Entity> delete(Collection<Entity> entities);

	/**
	 * Creates a new {@link EditTask} instance for inserting the active entity.
	 * @return a new {@link EditTask} instance
	 * @throws IllegalStateException in inserting is not enabled
	 * @throws ValidationException in case validation fails
	 * @see #insertEnabled()
	 */
	EditTask createInsert();

	/**
	 * Creates a new {@link EditTask} instance for inserting the given entities.
	 * @param entities the entities to insert
	 * @return a new {@link EditTask} instance
	 * @throws IllegalStateException in inserting is not enabled
	 * @throws ValidationException in case validation fails
	 * @see #insertEnabled()
	 */
	EditTask createInsert(Collection<Entity> entities);

	/**
	 * Creates a new {@link EditTask} instance for updating the active entity.
	 * @return a new {@link EditTask} instance
	 * @throws IllegalStateException in case the active entity is unmodified or if updating is not enabled
	 * @throws ValidationException in case validation fails
	 * @see #updateEnabled()
	 */
	EditTask createUpdate();

	/**
	 * Creates a new {@link EditTask} instance for updating the given entities.
	 * @param entities the entities to update
	 * @return a new {@link EditTask} instance
	 * @throws IllegalStateException in case any of the given entities are unmodified or if updating is not enabled
	 * @throws ValidationException in case validation fails
	 * @see #updateEnabled()
	 */
	EditTask createUpdate(Collection<Entity> entities);

	/**
	 * Creates a new {@link EditTask} instance for deleting the active entity.
	 * @return a new {@link EditTask} instance
	 * @throws IllegalStateException in deleting is not enabled
	 * @see #deleteEnabled()
	 */
	EditTask createDelete();

	/**
	 * Creates a new {@link EditTask} instance for deleting the given entities.
	 * @param entities the entities to delete
	 * @return a new {@link EditTask} instance
	 * @throws IllegalStateException in deleting is not enabled
	 * @see #deleteEnabled()
	 */
	EditTask createDelete(Collection<Entity> entities);

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
	 * @return an observer notified each time one or more entities have been inserted, updated or deleted via this model
	 */
	Observer<Collection<Entity>> afterInsertUpdateOrDelete();

	/**
	 * <p>Applies the given value to the given entities. This method can be used by components
	 * providing edit functionality, such as editable tables, in order to apply the edited value,
	 * ensuring that any associated values are updated as well.
	 * <p>By default, this sets the given attribute value in the entities via {@link Entity#set(Attribute, Object)}.
	 * <p>Override to customize, f.ex. when associated values must be changed accordingly.
	 * {@snippet :
	 *  @Override
	 * 	public <T> void applyEdit(Collection<Entity> entities, Attribute<T> attribute, T value) {
	 * 	  super.applyEdit(entities, attribute, value);
	 * 	  if (attribute.equals(Invoice.CUSTOMER_FK)) {
	 * 	    Entity customer = (Entity) value;
	 * 	    // Set the billing address when the customer is changed
	 * 	    entities.forEach(entity ->
	 *            entity.set(Invoice.BILLINGADDRESS, customer.get(Customer.ADDRESS)));
	 * 	  }
	 * 	}
	 *}
	 * @param entities the entities to apply the value to
	 * @param attribute the attribute being edited
	 * @param value the value to apply
	 * @see Entity#set(Attribute, Object)
	 */
	<T> void applyEdit(Collection<Entity> entities, Attribute<T> attribute, @Nullable T value);

	/**
	 * @param entityType the entity type
	 * @return the central {@link EditEvents} instance for the given entity type
	 */
	static EditEvents events(EntityType entityType) {
		return AbstractEntityEditModel.EVENTS.computeIfAbsent(requireNonNull(entityType), k -> new DefaultEditEvents());
	}

	/**
	 * Provides edit access to the underlying entity.
	 */
	interface EntityEditor extends Observable<Entity> {

		/**
		 * Specifies whether foreign key values should persist by default when defaults are set
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 * @see EntityEditor#defaults()
		 */
		PropertyValue<Boolean> PERSIST_FOREIGN_KEYS = booleanValue(EntityEditor.class.getName() + ".persistForeignKeys", true);

		/**
		 * @return an immutable copy of the entity being edited
		 */
		@Override
		Entity get();

		/**
		 * <p>Populates this editor with the values from the given entity or sets the default value for all attributes in case it is null.
		 * <p>Use {@link #clear()} in order to clear the editor of all values.
		 * @param entity the entity to set, if null, then defaults are set
		 * @see EditorValue#defaultValue()
		 * @see EditorValue#persist()
		 * @see AttributeDefinition#defaultValue()
		 */
		void set(@Nullable Entity entity);

		/**
		 * Clears all values from the underlying entity, disregarding the {@link EditorValue#persist()} directive.
		 */
		void clear();

		/**
		 * Populates this edit model with default values for all non-persistent attributes.
		 * @see EditorValue#defaultValue()
		 * @see EditorValue#persist()
		 * @see AttributeDefinition#defaultValue()
		 */
		void defaults();

		/**
		 * Reverts all attribute value changes.
		 */
		void revert();

		/**
		 * @return an {@link ObservableState} indicating whether the entity exists in the database
		 * @see Exists#predicate()
		 */
		Exists exists();

		/**
		 * <p>Returns an {@link ObservableState} indicating whether any values have been modified.
		 * <p>Note that only existing entities are modified, new, or non-existing entities are never modified.
		 * @return an {@link ObservableState} indicating the modified state of this entity
		 * @see Modified#predicate()
		 * @see Exists
		 */
		Modified modified();

		/**
		 * @return an observer notified each time the entity is about to be changed
		 * via {@link #set(Entity)} or {@link #defaults()}
		 * @see #set(Entity)
		 * @see #defaults()
		 */
		Observer<Entity> changing();

		/**
		 * Returns an observer notified each time a value is changed, either via its associated {@link EditorValue}
		 * instance or when the entity is set via {@link #clear()}, {@link #set(Entity)} or {@link #defaults()}.
		 * @return an observer notified each time a value is changed
		 */
		Observer<Attribute<?>> valueChanged();

		/**
		 * @return an {@link ObservableState} indicating whether the primary key of the entity is null
		 */
		ObservableState primaryKeyNull();

		/**
		 * @return an {@link ObservableState} indicating the valid status of the underlying Entity.
		 * @see #validate(Attribute)
		 * @see EntityValidator#validate(Entity)
		 */
		ObservableState valid();

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
		 * @param attribute the attribute
		 * @return true if this value is allowed to be null according to the validator
		 * @see #validator()
		 */
		boolean nullable(Attribute<?> attribute);

		/**
		 * Returns the {@link EditorValue} instance representing {@code attribute} in this {@link EntityEditor}.
		 * @param attribute the attribute
		 * @param <T> the value type
		 * @return the {@link EditorValue} representing the given attribute
		 */
		<T> EditorValue<T> value(Attribute<T> attribute);

		/**
		 * Indicates whether the active entity exists in the database.
		 * @see #predicate()
		 */
		interface Exists extends ObservableState {

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
		 * Indicates whether the active entity is modified, that is, exists and has one or more modified attribute values.
		 * @see #predicate()
		 */
		interface Modified extends ObservableState {

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
	 * Provides edit access to an {@link Attribute} value in the underlying entity being edited.
	 * @param <T> the attribute value type
	 */
	interface EditorValue<T> extends Value<T> {

		/**
		 * Reverts to the original value if modified
		 */
		void revert();

		/**
		 * <p>Returns a {@link State} controlling whether the last used value for this attribute should persist when defaults are set.
		 * @return a {@link State} controlling whether the given attribute value should persist when defaults are set
		 * @see EntityEditor#defaults()
		 * @see EntityEditor#PERSIST_FOREIGN_KEYS
		 */
		State persist();

		/**
		 * @return an {@link ObservableState} indicating the valid status of this attribute value.
		 */
		ObservableState valid();

		/**
		 * @return an {@link ObservableState} indicating whether the value of this attribute is non-null
		 */
		ObservableState present();

		/**
		 * @return the validation message in case the value is invalid, otherwise the attribute description
		 */
		Observable<String> message();

		/**
		 * <p>Returns an {@link ObservableState} instance indicating whether the value of the given attribute has been modified.
		 * <p>Note that only attributes of existing entities are modified, attributes of new, or non-existing entities are never modified.
		 * @return an {@link ObservableState} indicating the modified state of the value of the given attribute
		 * @see EntityEditor#modified()
		 */
		ObservableState modified();

		/**
		 * <p>Returns an observer notified each time this value is modified via {@link EditorValue#set(Object)}.
		 * <p>This event is NOT triggered when the value changes due to the entity being set
		 * via {@link EntityEditor#set(Entity)}, {@link EntityEditor#defaults()} or {@link EntityEditor#clear()}.
		 * <p>Note that this event is only triggered if the value actually changes.
		 * @return an observer notified when the given attribute value is edited
		 */
		Observer<T> edited();

		/**
		 * <p>Returns the {@link Value} instance controlling the default value supplier for the given attribute.
		 * <p>Used when the underlying value is not persistent.
		 * <p>Use {@link EntityEditor#defaults()} to populate the editor with default values.
		 * @return the {@link Value} instance controlling the default value supplier
		 * @see #persist()
		 */
		Value<Supplier<T>> defaultValue();
	}

	/**
	 * Represents a task for inserting, updating or deleting entities, split up for use with a background thread.
	 * {@snippet :
	 *   EditTask insert = editModel.createInsert();
	 *
	 *   EditTask.Task task = insert.prepare();
	 *
	 *   // Can safely be called in a background thread
	 *   EditTask.Result result = task.perform();
	 *
	 *   Collection<Entity> insertedEntities = result.handle();
	 *}
	 * {@link Task#perform()} may be called on a background thread while {@link EditTask#prepare()}
	 * and {@link Result#handle()} must be called on the UI thread.
	 */
	interface EditTask {

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

	/**
	 * @see EntityEditModel#EDIT_EVENTS
	 * @see EntityEditModel#editEvents()
	 * @see #events(EntityType)
	 */
	interface EditEvents {

		/**
		 * Returns an insert {@link Event}, notified each time entities are inserted.
		 * @return the insert {@link Event}
		 */
		Event<Collection<Entity>> inserted();

		/**
		 * Returns an update {@link Event}, notified each time entities are updated.
		 * @return the update {@link Event}
		 */
		Event<Map<Entity, Entity>> updated();

		/**
		 * Returns delete {@link Event}, notified each time entities are deleted.
		 * @return the delete {@link Event}
		 */
		Event<Collection<Entity>> deleted();
	}
}
