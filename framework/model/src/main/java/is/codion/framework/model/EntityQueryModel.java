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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.ValueSet;
import is.codion.common.utilities.Conjunction;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.utilities.Configuration.integerValue;

/**
 * Provides entities based on query conditions.
 * The default data source can be overridden by using {@link #dataSource()}.
 * {@snippet :
 * tableModel.queryModel().dataSource().set(queryModel -> {
 * 	 EntityConnection connection = queryModel.connectionProvider().connection();
 *
 *   return connection.select(Employee.NAME.equalTo("John"));
 * });
 *}
 * @see #entityQueryModel(EntityTableConditionModel)
 */
public interface EntityQueryModel {

	/**
	 * Specifes a default {@link #limit()} to set for query models.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: null
	 * </ul>
	 */
	PropertyValue<Integer> LIMIT = integerValue(EntityQueryModel.class.getName() + ".limit");

	/**
	 * @return the type of the entity this query model is based on
	 */
	EntityType entityType();

	/**
	 * @return the connection provider
	 */
	EntityConnectionProvider connectionProvider();

	/**
	 * Performs a query and returns the result.
	 * Note that if a query condition is required ({@link #conditionRequired()})
	 * and the condition is not enabled ({@link #conditionEnabled()}) an empty list is returned.
	 * @return entities selected from the database according to the query condition.
	 * @see #select()
	 */
	List<Entity> query();

	/**
	 * @return a {@link Select} instance based on the current state of this {@link EntityQueryModel}
	 */
	Select select();

	/**
	 * @return the {@link EntityTableConditionModel} instance used by this query model
	 */
	EntityTableConditionModel condition();

	/**
	 * Controls the additional WHERE condition, which can be used in conjunction with {@link #condition()}.
	 * The condition supplier may return null in case of no condition.
	 * Note that in order for the {@link #conditionChanged()} {@link ObservableState} to indicate
	 * a changed condition, the additional condition must be set via {@link AdditionalCondition#set(Object)},
	 * changing the return value of the underlying {@link Supplier} instance does not trigger a changed condition.
	 * @return the {@link AdditionalCondition} instance controlling the additional WHERE condition
	 */
	AdditionalCondition where();

	/**
	 * Controls the additional HAVING condition, which can be used in conjunction with {@link #condition()}.
	 * The condition supplier may return null in case of no condition.
	 * Note that in order for the {@link #conditionChanged()} {@link ObservableState} to indicate
	 * a changed condition, the additional condition must be set via {@link AdditionalCondition#set(Object)},
	 * changing the return value of the underlying {@link Supplier} instance does not trigger a changed condition.
	 * @return the {@link AdditionalCondition} instance controlling the additional HAVING condition
	 */
	AdditionalCondition having();

	/**
	 * Returns a {@link State} controlling whether this query model should query all underlying entities
	 * when no query condition has been set. Setting this value to 'true' prevents all rows from
	 * being fetched by accident, when no condition has been set, which is recommended for queries
	 * with a large underlying dataset.
	 * @return a {@link State} controlling whether this query model requires a query condition
	 * @see #conditionEnabled()
	 */
	State conditionRequired();

	/**
	 * This state is reset each time {@link #query()} returns successfully.
	 * @return an {@link ObservableState} indicating if the search condition has changed since last reset
	 */
	ObservableState conditionChanged();

	/**
	 * @return the {@link SelectAttributes} instance
	 */
	SelectAttributes attributes();

	/**
	 * Returns the {@link Value} controlling the maximum number of rows to fetch, a null value means all rows should be fetched
	 * @return the {@link Value} controlling the query limit
	 */
	Value<Integer> limit();

	/**
	 * Controls the order by clause to use when selecting the data for this model.
	 * Setting this value to null reverts back to the default order by
	 * for the underlying entity, if one has been specified
	 * @return the {@link Value} controlling the order by clause
	 * @see EntityDefinition#orderBy()
	 */
	Value<OrderBy> orderBy();

	/**
	 * It can be necessary to prevent the user from selecting too much data, when working with a large dataset.
	 * This can be done by enabling the {@link EntityQueryModel#conditionRequired()} {@link State}, which prevents a refresh as long as the
	 * {@link ObservableState} controlled via this method is disabled. The default {@link ObservableState} is simply {@link EntityTableConditionModel#enabled()}.
	 * Override for a more fine-grained control, such as requiring a specific column condition to be enabled.
	 * @return the {@link Value} controlling the {@link ObservableState} specifying if enough conditions are enabled for a safe refresh
	 * @see #conditionRequired()
	 */
	Value<ObservableState> conditionEnabled();

	/**
	 * A {@link Value} controlling the data source. Use this to replace the default one.
	 * @return the {@link Value} controlling the data source
	 */
	Value<Function<EntityQueryModel, List<Entity>>> dataSource();

	/**
	 * @param conditionModel the {@link EntityTableConditionModel}
	 * @return a new {@link EntityQueryModel} instance based on the given {@link EntityTableConditionModel}
	 */
	static EntityQueryModel entityQueryModel(EntityTableConditionModel conditionModel) {
		return new DefaultEntityQueryModel(conditionModel);
	}

	/**
	 * Specifies an additional condition supplier.
	 */
	interface AdditionalCondition extends Value<Supplier<Condition>> {

		/**
		 * Default {@link Conjunction#AND}.
		 * @return the {@link Value} controlling the {@link Conjunction} to use when adding the additional condition
		 */
		Value<Conjunction> conjunction();
	}

	/**
	 * Manages the attributes to include and exclude when querying.
	 * <p>
	 * The final set of attributes is computed as: {@code (defaults ∪ included) \ excluded}
	 * <p>
	 * Provides runtime configuration of which attributes to load:
	 * <ul>
	 *   <li>{@link #defaults()} - Override the base set of attributes (normally those with {@code .selected(true)})</li>
	 *   <li>{@link #include()} - Add attributes beyond the defaults (e.g., lazy-loaded columns)</li>
	 *   <li>{@link #exclude()} - Remove attributes from the final set</li>
	 * </ul>
	 */
	interface SelectAttributes {

		/**
		 * Returns the {@link ValueSet} controlling the default attributes to use as the base set when querying entities.
		 * <p>
		 * When empty, the entity definition's default selected attributes (those marked with {@code .selected(true)})
		 * are used. When non-empty, these attributes completely replace the entity definition defaults.
		 * <p>
		 * This allows complete control over the base attribute set, rather than just adding to or removing from defaults.
		 * <p>
		 * The final attribute set is: {@code (defaults ∪ included) \ excluded}
		 * {@snippet :
		 * // Replace defaults with a minimal set
		 * EntityQueryModel queryModel = tableModel.queryModel();
		 * queryModel.attributes().defaults().set(Employee.ID, Employee.NAME);
		 * tableModel.items().refresh();
		 *
		 * // Revert to entity definition defaults
		 * queryModel.attributes().defaults().clear();
		 * tableModel.items().refresh();
		 *}
		 * @return the {@link ValueSet} controlling the default base attributes
		 * @see #include()
		 * @see #exclude()
		 */
		ValueSet<Attribute<?>> defaults();

		/**
		 * Returns the {@link ValueSet} controlling additional attributes to include when querying entities,
		 * beyond the default selected attributes.
		 * <p>
		 * An empty {@link ValueSet} means no additional attributes will be included - only the defaults
		 * (those marked with {@code .selected(true)}) will be loaded.
		 * <p>
		 * This is particularly useful for including lazy-loaded attributes on-demand without having to
		 * replace the entire default attribute set.
		 * <p>
		 * The final attribute set is: {@code (defaults ∪ included) \ excluded}
		 * {@snippet :
		 * // Include a lazy blob column on-demand
		 * EntityQueryModel queryModel = tableModel.queryModel();
		 * queryModel.attributes().include().add(Country.FLAG);
		 * tableModel.items().refresh();
		 *
		 * // Remove the lazy attribute
		 * queryModel.attributes().include().remove(Country.FLAG);
		 * tableModel.items().refresh();
		 *}
		 * @return the {@link ValueSet} controlling additional attributes to include
		 * @see #exclude()
		 */
		ValueSet<Attribute<?>> include();

		/**
		 * Returns the {@link ValueSet} controlling attributes to exclude when querying entities.
		 * <p>
		 * These attributes are removed from the final set after applying defaults and {@link #include()}.
		 * <p>
		 * Note that primary key attributes are always included regardless of exclusions,
		 * and attributes required by included foreign keys cannot be excluded.
		 * <p>
		 * The final attribute set is: {@code (defaults ∪ included) \ excluded}
		 * {@snippet :
		 * // Exclude expensive computed columns
		 * EntityQueryModel queryModel = tableModel.queryModel();
		 * queryModel.attributes().exclude().add(Employee.COMPUTED_BONUS);
		 * tableModel.items().refresh();
		 *
		 * // Re-include the column
		 * queryModel.attributes().exclude().remove(Employee.COMPUTED_BONUS);
		 * tableModel.items().refresh();
		 *}
		 * @return the {@link ValueSet} controlling attributes to exclude
		 * @see #include()
		 */
		ValueSet<Attribute<?>> exclude();
	}
}
