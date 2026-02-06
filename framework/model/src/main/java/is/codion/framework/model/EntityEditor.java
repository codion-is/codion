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
import is.codion.common.utilities.property.PropertyValue;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.utilities.Configuration.booleanValue;

/**
 * Provides edit access to an underlying entity.
 */
public interface EntityEditor extends Observable<Entity> {

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
	 * @return an immutable copy of the entity being edited
	 */
	@Override
	Entity get();

	/**
	 * <p>Populates this editor with the values from the given entity or sets the default value for all attributes in case it is null.
	 * <p>Use {@link #clear()} in order to clear the editor of all values.
	 * <p>Notifies that the entity is about to change via {@link #changing()}
	 * @param entity the entity to set, if null, then defaults are set
	 * @see EditorValue#defaultValue()
	 * @see EditorValue#persist()
	 * @see ValueAttributeDefinition#defaultValue()
	 * @see #changing()
	 */
	void set(@Nullable Entity entity);

	/**
	 * Clears all values from the underlying entity, disregarding the {@link EditorValue#persist()} directive.
	 */
	void clear();

	/**
	 * Replaces the entity without notifying that it is changing.
	 */
	void replace(Entity entity);

	/**
	 * Populates this edit model with default values for all non-persistent attributes.
	 * <p>Notifies that the entity is about to change via {@link #changing()}
	 * @see EditorValue#defaultValue()
	 * @see EditorValue#persist()
	 * @see ValueAttributeDefinition#defaultValue()
	 * @see #changing()
	 */
	void defaults();

	/**
	 * Reverts all attribute value changes.
	 */
	void revert();

	/**
	 * Refreshes the active Entity from the database, discarding all changes.
	 * If the active Entity is new then calling this method has no effect.
	 * @see #exists()
	 */
	void refresh();

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
	 * <p>Throwing a {@link is.codion.common.model.CancelException} from a listener will cancel the change.
	 * @return an observer notified each time the entity is about to be changed via {@link #set(Entity)} or {@link #defaults()}.
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
	 * @return the {@link SearchModels} instance
	 */
	SearchModels searchModels();

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
	 * Provides data models for editor components.
	 */
	interface EditorModels {

		/**
		 * <p>Creates a {@link EntitySearchModel} for looking up entities of the type referenced by the given foreign key,
		 * using the search attributes defined for that entity type.
		 * @param foreignKey the foreign key for which to create a {@link EntitySearchModel}
		 * @param editor the editor
		 * @return a new {@link EntitySearchModel} for looking up entities of the type referenced by the given foreign key attribute,
		 * @throws IllegalStateException in case no searchable attributes can be found for the entity type referenced by the given foreign key
		 */
		EntitySearchModel createSearchModel(ForeignKey foreignKey, EntityEditor editor);

		/**
		 * <p>Called when a {@link EntitySearchModel} is created in {@link SearchModels#get(ForeignKey)}.
		 * @param foreignKey the foreign key
		 * @param entitySearchModel the search model
		 * @param editor the editor
		 */
		default void configure(ForeignKey foreignKey, EntitySearchModel entitySearchModel, EntityEditor editor) {}
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
	 * @see #predicate()
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
}
