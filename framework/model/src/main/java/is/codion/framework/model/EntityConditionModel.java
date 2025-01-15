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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory for {@link EntityConditionModel} instances via
 * {@link EntityConditionModel#entityConditionModel(EntityType, EntityConnectionProvider)}
 */
public interface EntityConditionModel extends TableConditionModel<Attribute<?>> {

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
	 * Returns a WHERE condition based on enabled condition models which are based on non-aggregate function columns.
	 * @param conjunction the conjunction to use in case of multiple enabled conditions
	 * @return the current WHERE condition based on the state of the underlying condition models
	 */
	Condition where(Conjunction conjunction);

	/**
	 * Returns a HAVING condition based on enabled condition models which are based on aggregate function columns.
	 * @param conjunction the conjunction to use in case of multiple enabled conditions
	 * @return the current HAVING condition based on the state of the underlying condition models
	 */
	Condition having(Conjunction conjunction);

	/**
	 * Returns the {@link ConditionModel} associated with the given column.
	 * @param <T> the column value type
	 * @param column the column for which to retrieve the {@link ConditionModel}
	 * @return the {@link ConditionModel} associated with {@code column}
	 * @throws IllegalArgumentException in case no condition model exists for the given column
	 */
	<T> ConditionModel<T> column(Column<T> column);

	/**
	 * Returns the {@link ConditionModel} associated with the given foreignKey.
	 * @param foreignKey the foreignKey for which to retrieve the {@link ConditionModel}
	 * @return the {@link ConditionModel} associated with {@code foreignKey}
	 * @throws IllegalArgumentException in case no condition model exists for the given foreignKey
	 */
	ConditionModel<Entity> foreignKey(ForeignKey foreignKey);

	/**
	 * Creates a new {@link EntityConditionModel}
	 * @param entityType the underlying entity type
	 * @param connectionProvider a EntityConnectionProvider instance
	 * @return a new {@link EntityConditionModel} instance
	 */
	static EntityConditionModel entityConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		return entityConditionModel(entityType, connectionProvider, new AttributeConditionModelFactory(entityType, connectionProvider));
	}

	/**
	 * Creates a new {@link EntityConditionModel}
	 * @param entityType the underlying entity type
	 * @param connectionProvider a EntityConnectionProvider instance
	 * @param conditionModelFactory supplies the column condition models for this table condition model
	 * @return a new {@link EntityConditionModel} instance
	 */
	static EntityConditionModel entityConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider,
																									 Supplier<Map<Attribute<?>, ConditionModel<?>>> conditionModelFactory) {
		return new DefaultEntityConditionModel(entityType, connectionProvider, conditionModelFactory);
	}
}
