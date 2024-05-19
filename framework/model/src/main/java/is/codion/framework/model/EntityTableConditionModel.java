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
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * This interface defines filtering functionality, which refers to showing/hiding entities already available
 * in a table model and searching functionality, which refers to configuring the underlying query,
 * which then needs to be re-run.<br>
 * Factory for {@link EntityTableConditionModel} instances via
 * {@link EntityTableConditionModel#entityTableConditionModel(EntityType, EntityConnectionProvider, ColumnConditionModel.Factory)}
 */
public interface EntityTableConditionModel extends TableConditionModel<Attribute<?>> {

	/**
	 * @return the type of the entity this table condition model is based on
	 */
	EntityType entityType();

	/**
	 * @return the connection provider
	 */
	EntityConnectionProvider connectionProvider();

	/**
	 * Sets the EQUAL condition value of the condition model associated with {@code attribute}.
	 * Enables the condition model in case {@code value} is non-empty or disables it if {@code values is empty}.
	 * @param attribute the attribute
	 * @param value the search condition value
	 * @param <T> the value type
	 * @return true if the search state changed as a result of this method call, false otherwise
	 */
	<T> boolean setEqualConditionValue(Attribute<T> attribute, T value);

	/**
	 * Sets the IN condition values of the condition model associated with {@code attribute}.
	 * Enables the condition model in case {@code value} is non-empty or disables it if {@code values is empty}.
	 * @param attribute the attribute
	 * @param values the search condition values, an empty Collection for none
	 * @param <T> the value type
	 * @return true if the search state changed as a result of this method call, false otherwise
	 */
	<T> boolean setInConditionValues(Attribute<T> attribute, Collection<T> values);

	/**
	 * Returns a WHERE condition based on enabled condition models which are based on non-aggregate function columns.
	 * @param conjunction the conjunction to use in case of multiple enabled conditions
	 * @return the current where condition based on the state of the underlying condition models
	 */
	Condition where(Conjunction conjunction);

	/**
	 * Returns a HAVING condition based on enabled condition models which are based on aggregate function columns.
	 * @param conjunction the conjunction to use in case of multiple enabled conditions
	 * @return the current having condition based on the state of the underlying condition models
	 */
	Condition having(Conjunction conjunction);

	/**
	 * Controls the additional where condition.
	 * The condition supplier may return null in case of no condition.
	 * @return the value controlling the additional where condition
	 */
	Value<Supplier<Condition>> additionalWhere();

	/**
	 * Controls the additional having condition.
	 * The condition supplier may return null in case of no condition.
	 * @return the value controlling the additional having condition
	 */
	Value<Supplier<Condition>> additionalHaving();

	/**
	 * Returns the {@link ColumnConditionModel} associated with the given attribute.
	 * @param <T> the column value type
	 * @param attribute the attribute for which to retrieve the {@link ColumnConditionModel}
	 * @return the {@link ColumnConditionModel} associated with {@code attribute}
	 * @throws IllegalArgumentException in case no condition model exists for the given attribute
	 */
	<T> ColumnConditionModel<Attribute<?>, T> attributeModel(Attribute<T> attribute);

	/**
	 * Creates a new {@link EntityTableConditionModel}
	 * @param entityType the underlying entity type
	 * @param connectionProvider a EntityConnectionProvider instance
	 * @return a new {@link EntityTableConditionModel} instance
	 */
	static EntityTableConditionModel entityTableConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		return entityTableConditionModel(entityType, connectionProvider, new EntityConditionModelFactory(connectionProvider));
	}

	/**
	 * Creates a new {@link EntityTableConditionModel}
	 * @param entityType the underlying entity type
	 * @param connectionProvider a EntityConnectionProvider instance
	 * @param conditionModelFactory provides the column condition models for this table condition model
	 * @return a new {@link EntityTableConditionModel} instance
	 */
	static EntityTableConditionModel entityTableConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider,
																														 ColumnConditionModel.Factory<Attribute<?>> conditionModelFactory) {
		return new DefaultEntityTableConditionModel(entityType, connectionProvider, conditionModelFactory);
	}
}
