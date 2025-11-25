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

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.Conjunction;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Factory for {@link EntityTableConditionModel} instances via
 * {@link EntityTableConditionModel#entityTableConditionModel(EntityType, EntityConnectionProvider)}
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
	 * Returns the {@link ConditionModel} associated with the given foreignKey.
	 * @param foreignKey the foreignKey for which to retrieve the {@link ConditionModel}
	 * @return the {@link ConditionModel} associated with {@code foreignKey}
	 * @throws IllegalArgumentException in case no condition model exists for the given foreignKey
	 */
	ForeignKeyConditionModel get(ForeignKey foreignKey);

	/**
	 * Controls the additional WHERE condition. The condition supplier may return null in case of no condition.
	 * Note that in order for the {@link #changed()} {@link is.codion.common.reactive.observer.Observer} to indicate
	 * a changed condition, the additional condition must be set via {@link AdditionalCondition#set(Object)},
	 * changing the return value of the underlying {@link Supplier} instance does not trigger a changed condition.
	 * @return the {@link AdditionalCondition} instance controlling the additional WHERE condition
	 */
	AdditionalCondition where();

	/**
	 * Controls the additional WHERE condition. The condition supplier may return null in case of no condition.
	 * Note that in order for the {@link #changed()} {@link is.codion.common.reactive.observer.Observer} to indicate
	 * a changed condition, the additional condition must be set via {@link AdditionalCondition#set(Object)},
	 * changing the return value of the underlying {@link Supplier} instance does not trigger a changed condition.
	 * @return the {@link AdditionalCondition} instance controlling the additional HAVING condition
	 */
	AdditionalCondition having();

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
	 * Creates a new {@link EntityTableConditionModel}
	 * @param entityType the underlying entity type
	 * @param connectionProvider a EntityConnectionProvider instance
	 * @return a new {@link EntityTableConditionModel} instance
	 */
	static EntityTableConditionModel entityTableConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		return entityTableConditionModel(entityType, connectionProvider, new EntityConditionModelFactory(entityType, connectionProvider));
	}

	/**
	 * Creates a new {@link EntityTableConditionModel}
	 * @param entityType the underlying entity type
	 * @param connectionProvider a EntityConnectionProvider instance
	 * @param conditionModelFactory supplies the column condition models for this table condition model
	 * @return a new {@link EntityTableConditionModel} instance
	 */
	static EntityTableConditionModel entityTableConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider,
																														 Supplier<Map<Attribute<?>, ConditionModel<?>>> conditionModelFactory) {
		return new DefaultEntityTableConditionModel(entityType, connectionProvider, conditionModelFactory);
	}
}
