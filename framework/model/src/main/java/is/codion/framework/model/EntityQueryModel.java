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

import is.codion.common.Conjunction;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
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

/**
 * Provides entities fetched from a database.
 * The default query mechanism can be overridden by using {@link #query()}.
 * <pre>
 * {@code
 * Function<EntityQueryModel, List<Entity>> query = queryModel -> {
 *     EntityConnection connection = queryModel.connectionProvider().connection();
 *
 *     return connection.select(Employee.NAME.equalTo("John"));
 * };
 *
 * tableModel.queryModel().query().set(query);
 * }
 * </pre>
 * @see #entityQueryModel(EntityConditionModel)
 */
public interface EntityQueryModel extends Supplier<List<Entity>> {

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
	 */
	@Override
	List<Entity> get();

	/**
	 * @return the {@link EntityConditionModel} instance used by this query model
	 */
	EntityConditionModel conditions();

	/**
	 * Controls the additional WHERE condition, which can be used in conjunction with {@link #conditions()}.
	 * The condition supplier may return null in case of no condition.
	 * Note that in order for the {@link #conditionChanged()} {@link ObservableState} to indicate
	 * a changed condition, the additional condition must be set via {@link AdditionalCondition#set(Object)},
	 * changing the return value of the underlying {@link Supplier} instance does not trigger a changed condition.
	 * @return the {@link AdditionalCondition} instance controlling the additional WHERE condition
	 */
	AdditionalCondition where();

	/**
	 * Controls the additional HAVING condition, which can be used in conjunction with {@link #conditions()}.
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
	 * When using the default query mechanism, the {@link #conditionChanged()} state is reset after each successful query.
	 * @return an {@link ObservableState} indicating if the search condition has changed since last reset
	 */
	ObservableState conditionChanged();

	/**
	 * Returns the {@link ValueSet} controlling which attributes are included when querying entities.
	 * Note that an empty {@link ValueSet} indicates that the default select attributes should be used.
	 * @return the {@link ValueSet} controlling the selected attributes
	 */
	ValueSet<Attribute<?>> attributes();

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
	 * {@link ObservableState} controlled via this method is disabled. The default {@link ObservableState} is simply {@link EntityConditionModel#enabled()}.
	 * Override for a more fine grained control, such as requiring a specific column condition to be enabled.
	 * @return the {@link Value} controlling the {@link ObservableState} specifying if enough conditions are enabled for a safe refresh
	 * @see #conditionRequired()
	 */
	Value<ObservableState> conditionEnabled();

	/**
	 * A {@link Value} controlling the override query. Use this to replace the default query.
	 * @return the {@link Value} controlling the query override
	 */
	Value<Function<EntityQueryModel, List<Entity>>> query();

	/**
	 * @param entityConditionModel the {@link EntityConditionModel}
	 * @return a new {@link EntityQueryModel} instance based on the given {@link EntityConditionModel}
	 */
	static EntityQueryModel entityQueryModel(EntityConditionModel entityConditionModel) {
		return new DefaultEntityQueryModel(entityConditionModel);
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
}
