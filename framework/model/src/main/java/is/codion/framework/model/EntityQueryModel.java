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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;

import java.util.List;

/**
 * Handles querying entities.
 * @see #entityQueryModel(EntityTableConditionModel)
 */
public interface EntityQueryModel {

	/**
	 * @return the type of the entity this table query model is based on
	 */
	EntityType entityType();

	/**
	 * Performs a query and returns the result.
	 * Note that if a query condition is required ({@link #conditionRequired()})
	 * and the condition is not enabled ({@link #conditionEnabled()}) an empty list is returned.
	 * @return the query result
	 * @throws DatabaseException in case of an exception
	 */
	List<Entity> query() throws DatabaseException;

	/**
	 * @return the {@link EntityTableConditionModel} instance used by this query model
	 */
	EntityTableConditionModel conditionModel();

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
	 * @return a {@link StateObserver} indicating if the search condition has changed since last query
	 */
	StateObserver conditionChanged();

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
	 * {@link StateObserver} controlled via this method is disabled. The default {@link StateObserver} is simply {@link EntityTableConditionModel#enabled()}.
	 * Override for a more fine grained control, such as requiring a specific column condition to be enabled.
	 * @return the {@link Value} controlling the {@link StateObserver} specifying if enough conditions are enabled for a safe refresh
	 * @see #conditionRequired()
	 */
	Value<StateObserver> conditionEnabled();

	/**
	 * @param conditionModel the condition model
	 * @return a new {@link EntityQueryModel} instance based on the given condition model
	 */
	static EntityQueryModel entityQueryModel(EntityTableConditionModel conditionModel) {
		return new DefaultEntityQueryModel(conditionModel);
	}
}
