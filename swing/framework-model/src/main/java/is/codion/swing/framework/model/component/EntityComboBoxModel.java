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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.component;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.model.EntityEditEvents;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A ComboBoxModel based on an Entity, showing by default all the entities in the underlying table.
 * @see #entityComboBoxModel(EntityType, EntityConnectionProvider)
 */
public interface EntityComboBoxModel extends FilterComboBoxModel<Entity> {

	/**
	 * Specifies whether entity combo box models handle entity edit events, by replacing updated entities and removing deleted ones
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see #handleEditEvents()
	 * @see is.codion.framework.model.EntityEditModel#POST_EDIT_EVENTS
	 */
	PropertyValue<Boolean> HANDLE_EDIT_EVENTS =
					Configuration.booleanValue(EntityComboBoxModel.class.getName() + ".handleEditEvents", true);

	/**
	 * @return the connection provider used by this combo box model
	 */
	EntityConnectionProvider connectionProvider();

	/**
	 * @return the type of the entity this combo box model is based on
	 */
	EntityType entityType();

	/**
	 * Enables the null item and sets the null item caption.
	 * @param nullCaption the null item caption
	 * @throws NullPointerException in case {@code nullCaption} is null
	 * @see ComboBoxItems#nullItem()
	 */
	void setNullCaption(String nullCaption);

	/**
	 * Controls the attributes to include when selecting the entities to populate this model with.
	 * Note that the primary key attribute values are always included.
	 * An empty Collection indicates that all attributes should be selected.
	 * @return the {@link ValueSet} controlling the attributes to select, an empty {@link ValueSet} indicating all available attributes
	 */
	ValueSet<Attribute<?>> attributes();

	/**
	 * @return the {@link State} controlling whether this combo box model should handle entity edit events, by adding inserted items,
	 * updating any updated items and removing deleted ones
	 * @see EntityEditEvents
	 */
	State handleEditEvents();

	/**
	 * @param primaryKey the primary key of the entity to fetch from this model
	 * @return the entity with the given key if found in the model, an empty Optional otherwise
	 */
	Optional<Entity> find(Entity.Key primaryKey);

	/**
	 * Selects the entity with the given primary key, whether filtered or visible.
	 * If the entity is not available in the model this method returns silently without changing the selection.
	 * @param primaryKey the primary key of the entity to select
	 */
	void select(Entity.Key primaryKey);

	/**
	 * Controls the condition supplier to use when querying data, set to null to fetch all underlying entities.
	 * @return a value controlling the condition supplier
	 */
	Value<Supplier<Condition>> condition();

	/**
	 * Controls the order by to use when selecting entities for this model.
	 * Note that in order for this to have an effect, you must disable sorting
	 * by setting the sort comparator to null via {@link VisibleItems#comparator()}
	 * @return the {@link Value} controlling the {@link OrderBy}
	 * @see VisibleItems#comparator()
	 */
	Value<OrderBy> orderBy();

	/**
	 * @return the foreign key filter
	 */
	ForeignKeyFilter foreignKeyFilter();

	/**
	 * @param foreignKey the foreign key
	 * @return a new {@link ForeignKeyComboBoxModelFactory}
	 */
	ForeignKeyComboBoxModelFactory foreignKeyComboBoxModel(ForeignKey foreignKey);

	/**
	 * @param foreignKey the foreign key
	 * @return a new {@link ForeignKeyComboBoxModelLinker}
	 */
	ForeignKeyComboBoxModelLinker foreignKeyComboBoxModelLinker(ForeignKey foreignKey);

	/**
	 * Provides a combo box for filtering this combo box instance, either by filter predicate or query condition.
	 */
	interface ForeignKeyComboBoxModelFactory {

		/**
		 * Returns a combo box model for selecting a foreign key value for filtering this model.
		 * @return a combo box model for selecting a filtering value for this combo box model
		 * @see #foreignKeyComboBoxModelLinker(ForeignKey)
		 */
		EntityComboBoxModel filter();

		/**
		 * Returns a combo box model for selecting a foreign key value for using as a query condition in this model.
		 * Note that each time the selection changes in the resulting model this model is refreshed.
		 * @return a combo box model for selecting a condition query value for this combo box model
		 * @see #foreignKeyComboBoxModelLinker(ForeignKey)
		 */
		EntityComboBoxModel condition();
	}

	/**
	 * Links a given combo box model representing master entities to this combo box model
	 * so that selection in the master model filters this model, either filter predicate or query condition
	 */
	interface ForeignKeyComboBoxModelLinker {

		/**
		 * Links the given foreign key combo box model via filter predicate
		 * @param foreignKeyModel the combo box model to link
		 */
		void filter(EntityComboBoxModel foreignKeyModel);

		/**
		 * Links the given foreign key combo box model via query condition
		 * @param foreignKeyModel the combo box model to link
		 */
		void condition(EntityComboBoxModel foreignKeyModel);
	}

	/**
	 * Creates a {@link Value} linked to the selected entity via the value of the given attribute.
	 * @param <T> the attribute type
	 * @param attribute the attribute
	 * @return a {@link Value} for selecting items by attribute value
	 */
	<T> Value<T> createSelectorValue(Attribute<T> attribute);

	/**
	 * @param entityType the type of the entity this combo box model should represent
	 * @param connectionProvider a EntityConnectionProvider instance
	 * @return a new {@link EntityComboBoxModel} instance
	 */
	static EntityComboBoxModel entityComboBoxModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		return new DefaultEntityComboBoxModel(entityType, connectionProvider);
	}

	/**
	 * Controls the foreign key filter for a {@link EntityComboBoxModel}
	 */
	interface ForeignKeyFilter {

		/**
		 * Filters the combo box model so that only items referencing the given keys via the given foreign key are visible.
		 * Note that this uses the {@link VisibleItems#predicate()} and replaces any previously set prediate.
		 * @param foreignKey the foreign key
		 * @param keys the keys, an empty Collection to clear the filter
		 * @see VisibleItems#predicate()
		 */
		void set(ForeignKey foreignKey, Collection<Entity.Key> keys);

		/**
		 * @param foreignKey the foreign key
		 * @return the keys currently used to filter the items of this model by foreign key, an empty collection for none
		 * @see #set(ForeignKey, Collection)
		 */
		Collection<Entity.Key> get(ForeignKey foreignKey);

		/**
		 * Controls whether foreign key filtering should be strict or not.
		 * When the filtering is strict only entities with the correct reference are included, that is,
		 * entities with null values for the given foreign key are filtered.
		 * Non-strict simply means that entities with null references are not filtered.
		 * @return the {@link State} controlling whether foreign key filtering should be strict
		 * @see #set(ForeignKey, Collection)
		 */
		State strict();

		/**
		 * Use this method to retrieve the default foreign key filter visible predicate if you
		 * want to add a custom {@link Predicate} to this model via {@link VisibleItems#predicate()}.
		 * <pre>
		 * {@code
		 *   Predicate fkPredicate = model.foreignKeyFilter().predicate();
		 *   model.items().visible().predicate().set(item -> fkPredicate.test(item) && ...);
		 * }
		 * </pre>
		 * @return the {@link Predicate} based on the foreign key filter entities
		 * @see #set(ForeignKey, Collection)
		 */
		Predicate<Entity> predicate();
	}
}