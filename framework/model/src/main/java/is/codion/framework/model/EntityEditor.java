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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.ObservableValueSet;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.ValueSet;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;
import is.codion.framework.domain.entity.exception.AttributeValidationException;
import is.codion.framework.domain.entity.exception.EntityValidationException;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.utilities.Configuration.booleanValue;
import static java.util.Objects.requireNonNull;

/**
 * Provides edit access to an underlying entity.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 * @param <R> the {@link EntityEditor} type
 */
public interface EntityEditor<M extends EntityModel<M, E, T, R>, E extends EntityEditModel<M, E, T, R>,
				T extends EntityTableModel<M, E, T, R>, R extends EntityEditor<M, E, T, R>> {

	/**
	 * Specifies whether editors publish their insert, update and delete events to {@link PersistenceEvents}
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see EntityEditor#publishPersistenceEvents()
	 */
	PropertyValue<Boolean> PUBLISH_PERSISTENCE_EVENTS = booleanValue(EntityEditor.class.getName() + ".publishPersistenceEvents", true);

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
	 * @return the underlying domain entities
	 */
	Entities entities();

	/**
	 * @return the entity definition
	 */
	EntityDefinition entityDefinition();

	/**
	 * @return the connection provider
	 */
	EntityConnectionProvider connectionProvider();

	/**
	 * @return the {@link PersistEvents}
	 */
	PersistEvents events();

	/**
	 * @return a state controlling whether this edit model publishes insert, update and delete events
	 * on the {@link PersistenceEvents} event bus.
	 * @see #PUBLISH_PERSISTENCE_EVENTS
	 */
	State publishPersistenceEvents();

	/**
	 * @return the {@link EditorEntity} instance
	 */
	EditorEntity entity();

	/**
	 * Clears all values from the underlying entity, disregarding the {@link EditorValue#persist()} directive.
	 */
	void clear();

	/**
	 * Populates this editor with default values for all non-persistent attributes.
	 * <p>Notifies that the entity is about to change via {@link EditorEntity#changing()}
	 * @see EditorValue#defaultValue()
	 * @see EditorValue#persist()
	 * @see ValueAttributeDefinition#defaultValue()
	 * @see EditorEntity#changing()
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
	 * @return an {@link ObservableState} indicating the modified state of this editor entity
	 * @see Modified#additional()
	 * @see Exists
	 */
	Modified modified();

	/**
	 * Returns an observer notified each time a value is changed, either via its associated {@link EditorValue}
	 * instance or when the entity is set via {@link #clear()}, {@link EditorEntity#set(Entity)} or {@link #defaults()}.
	 * @return an observer notified each time a value is changed
	 */
	Observer<Attribute<?>> valueChanged();

	/**
	 * @return an {@link ObservableState} indicating whether the value of the entity primary key is present
	 */
	ObservableState primaryKeyPresent();

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
	 * @throws EntityValidationException in case the entity is invalid
	 */
	void validate() throws EntityValidationException;

	/**
	 * Validates the value associated with the given attribute, using the underlying validator.
	 * @param attribute the attribute the value is associated with
	 * @throws AttributeValidationException if the given value is not valid for the given attribute
	 */
	void validate(Attribute<?> attribute) throws AttributeValidationException;

	/**
	 * Validates the given entities, using the underlying validator.
	 * For entities of a type other than this edit model is based on,
	 * their respective validators are used.
	 * @param entities the entities to validate
	 * @throws EntityValidationException on finding the first invalid entity
	 * @see EntityDefinition#validator()
	 */
	void validate(Collection<Entity> entities) throws EntityValidationException;

	/**
	 * Validates the given entity, using the underlying validator.
	 * For entities of a type other than this edit model is based on,
	 * their respective validators are used.
	 * @param entity the entity to validate
	 * @throws EntityValidationException in case the entity is invalid
	 * @throws NullPointerException in case the entity is null
	 */
	void validate(Entity entity) throws EntityValidationException;

	/**
	 * Returns the {@link EditorValue} instance representing {@code attribute} in this {@link EntityEditor}.
	 * @param attribute the attribute
	 * @param <T> the value type
	 * @return the {@link EditorValue} representing the given attribute
	 */
	<T> EditorValue<T> value(Attribute<T> attribute);

	/**
	 * @return the {@link SearchModels} instance
	 */
	SearchModels searchModels();

	/**
	 * @return the {@link EditorPersistence} used by this editor
	 */
	EditorPersistence persistence();

	/**
	 * @param connection the connection to use when persisting
	 * @return the {@link PersistTasks} instance
	 */
	PersistTasks tasks(EntityConnection connection);

	/**
	 * Represents a task for persisting entities, inserting, updating or deleting, split up for use with a background thread.
	 * {@snippet :
	 *   PersistTask<Entity> insert = editor.insert().build();
	 *
	 *   PersistTask.Task<Entity> task = insert.prepare();
	 *
	 *   // Can safely be called in a background thread
	 *   PersistTask.Result<Entity> result = task.perform();
	 *
	 *   Entity insertedEntity = result.handle();
	 *}
	 * {@link Task#perform()} may be called on a background thread while {@link PersistTask#prepare()}
	 * and {@link Result#handle()} must be called on the UI thread.
	 * @param <T> the result type
	 */
	interface PersistTask<T> {

		/**
		 * Notifies listeners that an operation is about to be performed.
		 * Must be called on the UI thread if this model has a panel based on it.
		 * @return the task
		 */
		Task<T> prepare();

		/**
		 * The task performing the operation
		 * @param <T> the result type
		 */
		interface Task<T> {

			/**
			 * May be called in a background thread.
			 * @return the insert result
			 */
			Result<T> perform();
		}

		/**
		 * The task result
		 * @param <T> the result type
		 */
		interface Result<T> {

			/**
			 * Notifies listeners that the task has been performed.
			 * Must be called on the UI thread if this model has a panel based on it.
			 * @return the entities involved
			 */
			T handle();
		}
	}

	/**
	 * Manages the {@link EntityPersistence} used by this editor
	 */
	interface EditorPersistence {

		/**
		 * @return the {@link EntityPersistence} used by this editor
		 */
		EntityPersistence get();

		/**
		 * @param persistence the {@link EntityPersistence} to use, default is set if null
		 * @throws IllegalStateException in case the current instance is not replaceable
		 * @see EntityPersistence#replaceable()
		 */
		void set(EntityPersistence persistence);
	}

	/**
	 * Provides builders for async persist tasks.
	 */
	interface PersistTasks {

		/**
		 * @return an async task for inserting the active entity
		 * @throws EntityValidationException in case of validation failure
		 */
		PersistTask<Entity> insert() throws EntityValidationException;

		/**
		 * @param entity the entity
		 * @return an async task for inserting the given entity
		 * @throws EntityValidationException in case of validation failure
		 */
		PersistTask<Entity> insert(Entity entity) throws EntityValidationException;

		/**
		 * @param entities the entities
		 * @return an async task for inserting the given entities
		 * @throws EntityValidationException in case of validation failure
		 */
		PersistTask<Collection<Entity>> insert(Collection<Entity> entities) throws EntityValidationException;

		/**
		 * @return an async task for updating the active entity
		 * @throws EntityValidationException in case of validation failure
		 */
		PersistTask<Entity> update() throws EntityValidationException;

		/**
		 * @param entity the entity
		 * @return an async task for updating the given entity
		 * @throws IllegalStateException in case the entity is not modified
		 * @throws EntityValidationException in case of validation failure
		 */
		PersistTask<Entity> update(Entity entity) throws EntityValidationException;

		/**
		 * @param entities the entities
		 * @return an async task for updating the given entities
		 * @throws IllegalStateException in case any entity is not modified
		 * @throws EntityValidationException in case of validation failure
		 */
		PersistTask<Collection<Entity>> update(Collection<Entity> entities) throws EntityValidationException;

		/**
		 * @return an async task for deleting the active entity
		 */
		PersistTask<Entity> delete();

		/**
		 * @param entity the entity
		 * @return an async task for deleting the given entity
		 */
		PersistTask<Entity> delete(Entity entity);

		/**
		 * @param entities the entities
		 * @return an async task for deleting the given entities
		 */
		PersistTask<Collection<Entity>> delete(Collection<Entity> entities);
	}

	/**
	 * Provides persistence event observers for the editor
	 */
	interface PersistEvents {

		/**
		 * @return the {@link BeforePersist}
		 */
		BeforePersist before();

		/**
		 * @return the {@link AfterPersist}
		 */
		AfterPersist after();

		/**
		 * @return an observer notified each time one or more entities have been persisted, as in, inserted, updated or deleted via this model
		 */
		Observer<Collection<Entity>> persisted();

		/**
		 * Events triggered before entities are persisted.
		 */
		interface BeforePersist {

			/**
			 * @return an observer notified before insert is performed, after validation
			 */
			Observer<Collection<Entity>> insert();

			/**
			 * @return an observer notified before update is performed, after validation
			 */
			Observer<Collection<Entity>> update();

			/**
			 * @return an observer notified before delete is performed
			 */
			Observer<Collection<Entity>> delete();
		}

		/**
		 * Events triggered after entities are persisted.
		 */
		interface AfterPersist {

			/**
			 * @return an observer notified after insert is performed
			 */
			Observer<Collection<Entity>> insert();

			/**
			 * @return an observer notified after update is performed, with the updated entities, mapped to their state before the update
			 */
			Observer<Map<Entity, Entity>> update();

			/**
			 * @return an observer notified after delete is performed
			 */
			Observer<Collection<Entity>> delete();
		}
	}

	/**
	 * Provides access to the entity instance being edited.
	 */
	interface EditorEntity extends Observable<Entity> {

		/**
		 * @return an immutable copy of the entity being edited
		 */
		@Override
		Entity get();

		/**
		 * <p>Populates this editor entity with the values from the given entity or sets
		 * the default value for all attributes in case it is null.
		 * <p>Use {@link #clear()} in order to clear the editor of all values.
		 * <p>Notifies that the entity is about to change via {@link #changing()}
		 * @param entity the entity to set, if null, then defaults are set
		 * @throws IllegalArgumentException in case the entity is not of the correct type
		 * @see EditorValue#defaultValue()
		 * @see EditorValue#persist()
		 * @see ValueAttributeDefinition#defaultValue()
		 * @see #changing()
		 */
		void set(@Nullable Entity entity);

		/**
		 * <p>Throwing a {@link is.codion.common.model.CancelException} from a listener will cancel the change.
		 * @return an observer notified each time the entity is about to be changed via {@link #set(Entity)} or {@link #defaults()}.
		 * @see #set(Entity)
		 * @see #defaults()
		 */
		Observer<Entity> changing();

		/**
		 * Replaces the entity without notifying that it is changing. A null argument sets default values.
		 * @see #changing()
		 */
		void replace(@Nullable Entity entity);

		/**
		 * Refreshes the active Entity from the database, discarding all changes.
		 * If the active Entity is new then calling this method has no effect.
		 * @see #exists()
		 */
		void refresh();
	}

	/**
	 * Manages the {@link EntitySearchModel}s used by a {@link EntityEditModel}
	 */
	interface SearchModels {

		/**
		 * <p>Returns the {@link EntitySearchModel} associated with the given foreign key.
		 * If no such search model exists, one is created by calling {@link #create(ForeignKey)}.
		 * <p>This method always returns the same {@link EntitySearchModel} instance, once one has been created.
		 * @param foreignKey the foreign key for which to retrieve the {@link EntitySearchModel}
		 * @return the {@link EntitySearchModel} associated with the given foreign key
		 */
		EntitySearchModel get(ForeignKey foreignKey);

		/**
		 * <p>Creates a {@link EntitySearchModel} for looking up entities of the type referenced by the given foreign key,
		 * using the search attributes defined for that entity type.
		 * @param foreignKey the foreign key for which to create a {@link EntitySearchModel}
		 * @return a new {@link EntitySearchModel} for looking up entities of the type referenced by the given foreign key attribute,
		 * @throws IllegalStateException in case no searchable attributes can be found for the entity type referenced by the given foreign key
		 */
		EntitySearchModel create(ForeignKey foreignKey);
	}

	/**
	 * Provides models for editor components requiring database access.
	 * @param <M> the {@link EntityModel} type
	 * @param <E> the {@link EntityEditModel} type
	 * @param <T> the {@link EntityTableModel} type
	 * @param <R> the {@link EntityEditor} type
	 */
	interface ComponentModels<M extends EntityModel<M, E, T, R>, E extends EntityEditModel<M, E, T, R>,
					T extends EntityTableModel<M, E, T, R>, R extends EntityEditor<M, E, T, R>> {

		/**
		 * <p>Creates a {@link EntitySearchModel} for looking up entities of the type referenced by the given foreign key,
		 * using the search attributes defined for that entity type.
		 * @param foreignKey the foreign key for which to create a {@link EntitySearchModel}
		 * @param connectionProvider the connection provider
		 * @return a new {@link EntitySearchModel} for looking up entities of the type referenced by the given foreign key attribute,
		 * @throws IllegalStateException in case no searchable attributes can be found for the entity type referenced by the given foreign key
		 * @see EntityDefinition.Columns#searchable()
		 */
		default EntitySearchModel searchModel(ForeignKey foreignKey, EntityConnectionProvider connectionProvider) {
			Collection<Column<String>> searchable = requireNonNull(connectionProvider).entities()
							.definition(requireNonNull(foreignKey).referencedType())
							.columns()
							.searchable();
			if (searchable.isEmpty()) {
				throw new IllegalStateException("No searchable columns defined for entity: " + foreignKey.referencedType());
			}

			return EntitySearchModel.builder()
							.entityType(foreignKey.referencedType())
							.connectionProvider(connectionProvider)
							.build();
		}
	}

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
	 * @see #attributes()
	 * @see #additional()
	 */
	interface Modified extends ObservableState {

		/**
		 * Indicates which editor attributes are modified.
		 * Note that only attributes of an existing entity are regarded as modified.
		 * @return an {@link ObservableValueSet} indicating the currently modified attributes
		 * @see #value(Attribute)
		 * @see #exists()
		 */
		ObservableValueSet<Attribute<?>> attributes();

		/**
		 * Controls the 'additional' modified states for this {@link Modified} instance,
		 * which are combined with the entity modified state using OR.
		 * @return the {@link ValueSet} controlling the additional modified states
		 */
		ValueSet<ObservableState> additional();
	}

	/**
	 * Provides edit access to an {@link Attribute} value in the underlying entity being edited.
	 * @param <T> the attribute value type
	 */
	interface EditorValue<T> extends Value<T> {

		/**
		 * @return the attribute
		 */
		Attribute<T> attribute();

		/**
		 * @return the original value, or the current one if not modified
		 * @see #modified()
		 */
		@Nullable T original();

		/**
		 * Reverts to the original value if modified
		 */
		void revert();

		/**
		 * Updates the valid state of this value according to the underlying validator
		 * @see #valid()
		 */
		void validate();

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
		 * <p>Returns an {@link ObservableState} instance indicating whether the value of the given attribute has been modified,
		 * that is, if the current value differs from its default value.
		 * <p>Note that unlike {@link EditorEntity#modified()} this state does not depend on whether the underlying entity exists.
		 * @return an {@link ObservableState} indicating the modified state of the value of the given attribute
		 * @see EntityEditor#modified()
		 */
		ObservableState modified();

		/**
		 * <p>Returns an observer notified each time this value is modified via {@link EditorValue#set(Object)}.
		 * <p>This event is NOT triggered when the value changes due to the entity being set
		 * via {@link EditorEntity#set(Entity)}, {@link EntityEditor#defaults()} or {@link EntityEditor#clear()}.
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

		/**
		 * <p>Sets a {@link Propagator} for this attribute, which propagates derived values
		 * each time this attribute is edited via {@link #set(Object)} or set in an entity via {@link #set(Entity, Object)}.
		 * <p>When this attribute is edited via the editor, the {@link Propagator} sets associated values via
		 * their respective {@link EditorValue} instances, triggering the usual edit events.
		 * When this attribute is set in an entity via {@link #set(Entity, Object)}, the associated values
		 * are set directly on the entity via {@link Entity#set(Attribute, Object)}.
		 * {@snippet :
		 * // Populate billing address fields when customer changes
		 * editor().value(Invoice.CUSTOMER_FK).propagate((customer, setter) -> {
		 *     setter.set(Invoice.BILLINGADDRESS, customer == null ? null : customer.get(Customer.ADDRESS));
		 *     setter.set(Invoice.BILLINGCITY, customer == null ? null : customer.get(Customer.CITY));
		 * });
		 *
		 * // Decompose a date into constituent parts
		 * editor().value(Sample.SAMPLE_DATE).propagate((date, setter) -> {
		 *     setter.set(Sample.DAY, date == null ? null : date.getDayOfMonth());
		 *     setter.set(Sample.MONTH, date == null ? null : date.getMonthValue());
		 *     setter.set(Sample.YEAR, date == null ? null : date.getYear());
		 * });
		 *}
		 * @param propagator the propagator, null to remove
		 * @see Propagator
		 */
		void propagate(@Nullable Propagator<T> propagator);

		/**
		 * <p>Sets the given value for the underlying attribute in the given entity,
		 * applying the associated {@link Propagator}, if one is specified.
		 * <p>This method is used by components providing edit functionality outside the editor,
		 * such as editable table cells or multi-item editing dialogs, to ensure that
		 * derived values are updated along with the primary attribute value.
		 * <p>Note that {@code value} may be null.
		 * @param entity the entity
		 * @param value the value to set
		 * @see #propagate(Propagator)
		 */
		void set(Entity entity, @Nullable T value);

		/**
		 * <p>Propagates derived attribute values when a source attribute value changes.
		 * <p>A {@link Propagator} provides a unified mechanism for updating associated attribute values,
		 * replacing the need to separately handle value propagation in the editor
		 * (via {@link EditorValue#edited()} listeners) and in entities (via overriding).
		 * <p>The {@link Setter} provided to {@link #propagate(Object, Setter)} routes to the
		 * appropriate target depending on context:
		 * <ul>
		 * <li>When editing via the editor ({@link EditorValue#set(Object)}),
		 *     values are set via the respective {@link EditorValue} instances, triggering edit events.
		 * <li>When setting values in an entity ({@link EditorValue#set(Entity, Object)}),
		 *     values are set directly on the entity via {@link Entity#set(Attribute, Object)}.
		 * </ul>
		 * @param <T> the source attribute value type
		 * @see EditorValue#propagate(Propagator)
		 */
		interface Propagator<T> {

			/**
			 * Propagates derived values based on the given source attribute value.
			 * @param value the new source attribute value, may be null
			 * @param setter the setter to use when setting derived attribute values
			 */
			void propagate(@Nullable T value, Setter setter);

			/**
			 * Sets attribute values, routing to the appropriate target depending on context.
			 */
			interface Setter {

				/**
				 * Sets the value of the given attribute.
				 * @param attribute the attribute
				 * @param value the value
				 * @param <T> the value type
				 */
				<T> void set(Attribute<T> attribute, @Nullable T value);
			}
		}
	}
}
