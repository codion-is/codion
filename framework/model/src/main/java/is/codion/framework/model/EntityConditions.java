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
import is.codion.common.model.condition.ColumnConditions;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.observer.Mutable;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Factory for {@link EntityConditions} instances via
 * {@link EntityConditions#entityConditions(EntityType, EntityConnectionProvider)}
 */
public interface EntityConditions extends ColumnConditions<Attribute<?>> {

	/**
	 * @return the type of the entity this table condition model is based on
	 */
	EntityType entityType();

	/**
	 * @return the connection provider
	 */
	EntityConnectionProvider connectionProvider();

	/**
	 * Sets the EQUAL condition operand of the condition model associated with {@code attribute}.
	 * Enables the condition model in case {@code operand} is non-empty or disables it if {@code operand} is empty.
	 * @param attribute the attribute
	 * @param operand the search condition operand
	 * @param <T> the operand type
	 * @return true if the search state changed as a result of this method call, false otherwise
	 */
	<T> boolean setEqualOperand(Attribute<T> attribute, T operand);

	/**
	 * Sets the IN condition operands of the condition model associated with {@code attribute}.
	 * Enables the condition model in case {@code operands} is non-empty or disables it if {@code operands} is empty.
	 * @param attribute the attribute
	 * @param operands the search condition operands, an empty Collection for none
	 * @param <T> the value type
	 * @return true if the search state changed as a result of this method call, false otherwise
	 */
	<T> boolean setInOperands(Attribute<T> attribute, Collection<T> operands);

	/**
	 * Returns a WHERE condition based on enabled condition models which are based on non-aggregate function columns
	 * along with any {@link #additionalWhere()} condition.
	 * @param conjunction the conjunction to use in case of multiple enabled conditions
	 * @return the current WHERE condition based on the state of the underlying condition models
	 */
	Condition where(Conjunction conjunction);

	/**
	 * Returns a HAVING condition based on enabled condition models which are based on aggregate function columns
	 * along with any {@link #additionalHaving()} condition.
	 * @param conjunction the conjunction to use in case of multiple enabled conditions
	 * @return the current HAVING condition based on the state of the underlying condition models
	 */
	Condition having(Conjunction conjunction);

	/**
	 * Controls the additional WHERE condition.
	 * The condition supplier may return null in case of no condition.
	 * @return the {@link AdditionalCondition} instance controlling the additional WHERE condition
	 */
	AdditionalCondition additionalWhere();

	/**
	 * Controls the additional HAVING condition.
	 * The condition supplier may return null in case of no condition.
	 * @return the {@link AdditionalCondition} instance controlling the additional HAVING condition
	 */
	AdditionalCondition additionalHaving();

	/**
	 * Returns the {@link ConditionModel} associated with the given attribute.
	 * @param <T> the column value type
	 * @param attribute the attribute for which to retrieve the {@link ConditionModel}
	 * @return the {@link ConditionModel} associated with {@code attribute}
	 * @throws IllegalArgumentException in case no condition model exists for the given attribute
	 */
	<T> ConditionModel<T> attribute(Attribute<T> attribute);

	/**
	 * Creates a new {@link EntityConditions}
	 * @param entityType the underlying entity type
	 * @param connectionProvider a EntityConnectionProvider instance
	 * @return a new {@link EntityConditions} instance
	 */
	static EntityConditions entityConditions(EntityType entityType, EntityConnectionProvider connectionProvider) {
		return entityConditions(entityType, connectionProvider, new EntityColumnConditionFactory(connectionProvider));
	}

	/**
	 * Creates a new {@link EntityConditions}
	 * @param entityType the underlying entity type
	 * @param connectionProvider a EntityConnectionProvider instance
	 * @param columnConditionFactory provides the column condition models for this table condition model
	 * @return a new {@link EntityConditions} instance
	 */
	static EntityConditions entityConditions(EntityType entityType, EntityConnectionProvider connectionProvider,
																					 ColumnConditionFactory<Attribute<?>> columnConditionFactory) {
		return new DefaultEntityConditions(entityType, connectionProvider, columnConditionFactory);
	}

	/**
	 * Specifies an additional condition supplier.
	 */
	interface AdditionalCondition extends Mutable<Supplier<Condition>> {

		/**
		 * Default {@link Conjunction#AND}.
		 * @return the {@link Mutable} controlling the {@link Conjunction} to use when adding the additional condition
		 */
		Mutable<Conjunction> conjunction();
	}
}
