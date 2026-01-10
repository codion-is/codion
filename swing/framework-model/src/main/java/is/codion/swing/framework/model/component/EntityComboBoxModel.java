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
package is.codion.swing.framework.model.component;

import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.utilities.Configuration.booleanValue;

/**
 * <p>A ComboBoxModel based on an Entity, showing by default all the entities in the underlying table.</p>
 * <p>To filter a {@link EntityComboBoxModel} use {@link #filter()} to set a {@link Predicate} or configure {@link ForeignKey} based filtering.</p>
 * @see #builder()
 */
public interface EntityComboBoxModel extends FilterComboBoxModel<Entity> {

	/**
	 * Specifies whether entity combo box models handle entity edit events, by adding new entities, replacing updated and removing deleted ones
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see Builder#editEvents(boolean)
	 * @see is.codion.framework.model.EntityEditModel#EDIT_EVENTS
	 */
	PropertyValue<Boolean> EDIT_EVENTS =
					booleanValue(EntityComboBoxModel.class.getName() + ".editEvents", true);

	/**
	 * @return the connection provider used by this combo box model
	 */
	EntityConnectionProvider connectionProvider();

	/**
	 * @return the underlying {@link EntityDefinition}
	 */
	EntityDefinition entityDefinition();

	/**
	 * Selects the entity with the given primary key, whether included or filtered.
	 * If the entity is not available in the model this method returns silently without changing the selection.
	 * @param primaryKey the primary key of the entity to select
	 */
	void select(Entity.Key primaryKey);

	/**
	 * <p>Controls the condition supplier used when querying data, setting this to null reverts to the condition specifying all underlying entities.
	 * <p>The condition supplier may not return null, doing so will cause an exception when refreshing the model items.
	 * @return a value controlling the condition supplier
	 */
	Value<Supplier<Condition>> condition();

	/**
	 * @return the {@link Filter} instance
	 */
	Filter filter();

	/**
	 * Creates a {@link Value} linked to the selected entity via the value of the given attribute.
	 * @param <T> the attribute type
	 * @param attribute the attribute
	 * @return a {@link Value} for selecting items by attribute value
	 */
	<T> Value<T> createSelectorValue(Attribute<T> attribute);

	/**
	 * @return a {@link Builder.EntityTypeStep} instance
	 */
	static Builder.EntityTypeStep builder() {
		return DefaultEntityComboBoxModel.DefaultBuilder.ENTITY_TYPE;
	}

	/**
	 * Builds a {@link EntityComboBoxModel}.
	 */
	interface Builder {

		/**
		 * Provides a {@link ConnectionProviderStep}
		 */
		interface EntityTypeStep {

			/**
			 * @param entityType the type of the entity this combo box model should represent
			 * @return a new {@link ConnectionProviderStep} instance
			 */
			ConnectionProviderStep entityType(EntityType entityType);
		}

		/**
		 * Provides a {@link Builder}
		 */
		interface ConnectionProviderStep {

			/**
			 * @param connectionProvider a EntityConnectionProvider instance
			 * @return a new {@link EntityComboBoxModel.Builder} instance
			 */
			Builder connectionProvider(EntityConnectionProvider connectionProvider);
		}

		/**
		 * Specifies the {@link OrderBy} to use when selecting entities for this model.
		 * in the combo box model.
		 * @param orderBy the {@link OrderBy} to use when selecting
		 * @return this builder instance
		 */
		Builder orderBy(@Nullable OrderBy orderBy);

		/**
		 * Note that this comparator is not used if {@link #orderBy(OrderBy)} has been specified.
		 * @param comparator the comparator to use, null for unsorted
		 * @return this builder instance
		 */
		Builder comparator(@Nullable Comparator<Entity> comparator);

		/**
		 * <p>If {@code condition} is null, the default condition, specifying all underlying entities is used.
		 * <p>The condition supplier may not return null, doing so will cause an exception when refreshing the model items.
		 * @param condition the condition supplier to use when querying data, may not return null
		 * @return this builder instance
		 */
		Builder condition(@Nullable Supplier<Condition> condition);

		/**
		 * Specifies the attributes to include when selecting the entities to populate this model with.
		 * Note that the primary key attribute values are always included.
		 * An empty Collection indicates that all attributes should be selected.
		 * @param attributes the attributes to select, an empty Collection for all
		 * @return this builder instance
		 */
		Builder attributes(Collection<Attribute<?>> attributes);

		/**
		 * @param includeNull if true then the null item is enabled using the default null item caption ({@link FilterComboBoxModel#NULL_CAPTION})
		 * @return this builder instance
		 * @see FilterComboBoxModel#NULL_CAPTION
		 */
		Builder includeNull(boolean includeNull);

		/**
		 * Enables the null item and sets the null item caption.
		 * @param nullCaption the null item caption
		 * @return this builder instance
		 */
		Builder nullCaption(@Nullable String nullCaption);

		/**
		 * @param entity the entity to select initially
		 * @return this builder
		 */
		Builder select(@Nullable Entity entity);

		/**
		 * @param editEvents controls whether this combo box model should handle entity edit events, by adding inserted items,
		 * updating any updated items and removing deleted ones
		 * @return this builder instance
		 * @see #EDIT_EVENTS
		 * @see is.codion.framework.model.EntityEditModel.EditEvents
		 */
		Builder editEvents(boolean editEvents);

		/**
		 * Specifies whether filtering the model affects the currently selected item.
		 * If true, the selection is cleared when the selected item is filtered from
		 * the model, otherwise the selected item can potentially represent a value
		 * which is not currently included in the model
		 * This is false by default.
		 * @param filterSelected if true then the selected item is cleared when filtered
		 * @return this builder instance
		 * @see IncludedItems#predicate()
		 */
		Builder filterSelected(boolean filterSelected);

		/**
		 * Links the given combo box model representing foreign key entities to this combo box model
		 * so that selection in the foreign key model filters this model.
		 * Note that {@code filterModel} is automatically refreshed each time this combo box model is refreshed.
		 * @param foreignKey the foreign key
		 * @param filterModel the combo box model filtering this model
		 * @see ForeignKeyFilter#link(EntityComboBoxModel)
		 */
		Builder filter(ForeignKey foreignKey, EntityComboBoxModel filterModel);

		/**
		 * @param item receives the selected item, note that this item may be null
		 * @return this builder instance
		 */
		Builder onItemSelected(Consumer<@Nullable Entity> item);

		/**
		 * Default false.
		 * @param refresh true if the model items should be refreshed on initialization
		 * @return this builder instance
		 */
		Builder refresh(boolean refresh);

		/**
		 * @return a new {@link EntityComboBoxModel} instance
		 */
		EntityComboBoxModel build();
	}

	/**
	 * Controls the filters for a {@link EntityComboBoxModel}
	 */
	interface Filter {

		/**
		 * Controls the additional filter predicate, which is tested for items that pass all foreign key filters
		 * @return the {@link Value} controlling the addition filter predicate
		 */
		Value<Predicate<Entity>> predicate();

		/**
		 * Returns a filter based on the given foreign key
		 * @param foreignKey the foreign key
		 * @return a foreign key filter
		 */
		ForeignKeyFilter get(ForeignKey foreignKey);
	}

	/**
	 * Controls a foreign key filter for a {@link EntityComboBoxModel}
	 */
	interface ForeignKeyFilter {

		/**
		 * Filters the combo box model so that only items referencing the given key are included.
		 * @param key the key to filter by
		 */
		void set(Entity.Key key);

		/**
		 * Filters the combo box model so that only items referencing the given keys are included.
		 * If {@code keys} is empty and {@link #strict()} filtering is enabled, all entities are filtered.
		 * @param keys the keys to filter by
		 */
		void set(Collection<Entity.Key> keys);

		/**
		 * @return the current filter keys
		 */
		Collection<Entity.Key> get();

		/**
		 * Clears and disables this foreign key filter
		 */
		void clear();

		/**
		 * Controls whether foreign key filtering should be strict or not.
		 * A strict foreign key filter filters all entities if no filter keys are specified and filters individual entities if the reference key is null.
		 * @return the {@link State} controlling whether foreign key filtering should be strict
		 * @see #set(Collection)
		 */
		State strict();

		/**
		 * Links the given combo box model representing foreign key entities to this combo box model
		 * so that selection in the foreign key model filters this model.
		 * Note that {@code filterModel} is automatically refreshed each time this combo box model is refreshed.
		 * @param filterModel the combo box model filtering this model
		 */
		void link(EntityComboBoxModel filterModel);
	}
}