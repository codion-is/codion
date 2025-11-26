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
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.Conjunction;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Use {@link EntityConditionModel#builder()} for instance.
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
	 * Returns a WHERE condition based on enabled condition models which are based on non-aggregate function columns.
	 * Uses the conjunction managed by {@link #conjunction()}
	 * @return the current WHERE condition based on the state of the underlying condition models
	 */
	Condition where();

	/**
	 * Returns a HAVING condition based on enabled condition models which are based on aggregate function columns.
	 * Uses the conjunction managed by {@link #conjunction()}
	 * @return the current HAVING condition based on the state of the underlying condition models
	 */
	Condition having();

	/**
	 * Default {@link Conjunction#AND}
	 * @return the {@link Value} managing the conjunction to use in case of multiple conditions
	 */
	Value<Conjunction> conjunction();

	/**
	 * Returns the {@link ConditionModel} associated with the given column.
	 * @param <T> the column value type
	 * @param column the column for which to retrieve the {@link ConditionModel}
	 * @return the {@link ConditionModel} associated with {@code column}
	 * @throws IllegalArgumentException in case no condition model exists for the given column
	 */
	<T> ConditionModel<T> get(Column<T> column);

	/**
	 * Returns the {@link ConditionModel} associated with the given foreignKey.
	 * @param foreignKey the foreignKey for which to retrieve the {@link ConditionModel}
	 * @return the {@link ConditionModel} associated with {@code foreignKey}
	 * @throws IllegalArgumentException in case no condition model exists for the given foreignKey
	 */
	ForeignKeyConditionModel get(ForeignKey foreignKey);

	/**
	 * @return the {@link AdditionalConditions} instance, for managing additional conditions
	 */
	AdditionalConditions additional();

	/**
	 * @return the {@link Modified} instance
	 * @see Modified#reset()
	 */
	Modified modified();

	/**
	 * Indicates if the condition has changed since the last call to {@link #reset()}
	 */
	interface Modified extends ObservableState {

		/**
		 * Resets the modified state according to the current condition state.
		 */
		void reset();
	}

	/**
	 * Manages the additional WHERE and HAVING conditions.
	 */
	interface AdditionalConditions {

		/**
		 * Controls the additional WHERE condition. The condition supplier may return null in case of no condition.
		 * Note that in order for the {@link #changed()} {@link is.codion.common.reactive.observer.Observer} to indicate
		 * a changed condition, the additional condition must be set via {@link ConditionValue#set(Object)},
		 * changing the return value of the underlying {@link Supplier} instance does not trigger a changed condition.
		 * @return the {@link ConditionValue} instance controlling the additional WHERE condition
		 */
		ConditionValue where();

		/**
		 * Controls the additional WHERE condition. The condition supplier may return null in case of no condition.
		 * Note that in order for the {@link #changed()} {@link is.codion.common.reactive.observer.Observer} to indicate
		 * a changed condition, the additional condition must be set via {@link ConditionValue#set(Object)},
		 * changing the return value of the underlying {@link Supplier} instance does not trigger a changed condition.
		 * @return the {@link ConditionValue} instance controlling the additional HAVING condition
		 */
		ConditionValue having();
	}

	/**
	 * Manages an additional condition supplier.
	 */
	interface ConditionValue extends Value<Supplier<Condition>> {

		/**
		 * Default {@link Conjunction#AND}.
		 * @return the {@link Value} controlling the {@link Conjunction} to use when adding the additional condition
		 */
		Value<Conjunction> conjunction();
	}

	/**
	 * Builds an {@link EntityConditionModel}
	 */
	interface Builder {

		/**
		 * The first step in building an {@link EntityConditionModel}
		 */
		interface EntityTypeStep {

			/**
			 * @param entityType the underlying entity type
			 * @return the {@link ConnectionProviderStep}
			 */
			ConnectionProviderStep entityType(EntityType entityType);
		}

		/**
		 * The second step in building an {@link EntityConditionModel}
		 */
		interface ConnectionProviderStep {

			/**
			 * @param connectionProvider a {@link EntityConnectionProvider} instance
			 * @return the {@link Builder}
			 */
			Builder connectionProvider(EntityConnectionProvider connectionProvider);
		}

		/**
		 * @param conditions supplies the column condition models for this condition model
		 * @return this builder
		 */
		Builder conditions(Supplier<Map<Attribute<?>, ConditionModel<?>>> conditions);

		/**
		 * @return a new {@link EntityConditionModel} instance
		 */
		EntityConditionModel build();
	}

	/**
	 * @return a {@link Builder.EntityTypeStep}
	 */
	static Builder.EntityTypeStep builder() {
		return DefaultEntityConditionModel.DefaultBuilder.ENTITY_TYPE_STEP;
	}
}
