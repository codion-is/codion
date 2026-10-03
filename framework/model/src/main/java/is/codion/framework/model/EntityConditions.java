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
package is.codion.framework.model;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditions;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.Conjunction;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.common.utilities.Configuration.booleanValue;

/**
 * Manages the condition models associated with an entity's attributes, providing the combined
 * WHERE and HAVING conditions used when querying entities.
 * Use {@link EntityConditions#builder()} for an instance.
 */
public interface EntityConditions extends TableConditions<Attribute<?>> {

	/**
	 * Specifies whether the negative operators {@link is.codion.common.utilities.Operator#NOT_EQUAL},
	 * {@link is.codion.common.utilities.Operator#NOT_IN}, {@link is.codion.common.utilities.Operator#NOT_BETWEEN} and
	 * {@link is.codion.common.utilities.Operator#NOT_BETWEEN_EXCLUSIVE} include null values when translated to a query condition,
	 * matching exactly the rows their positive counterparts do not, the way a {@link ConditionModel} used as a filter does.
	 * If false the condition follows SQL, where no comparison to null holds, excluding rows without a value.
	 * Note that this only affects conditions based on nullable columns and foreign keys, and only the conditions created
	 * by this model, conditions created via {@link Column} and {@link ForeignKey} are always plain SQL.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see ColumnConditionModel.Builder#negationIncludesNull(boolean)
	 * @see ForeignKeyConditionModel.Builder#negationIncludesNull(boolean)
	 */
	PropertyValue<Boolean> NEGATION_INCLUDES_NULL = booleanValue(EntityConditions.class.getName() + ".negationIncludesNull", true);

	/**
	 * @return the type of the entity this table condition model is based on
	 */
	EntityType entityType();

	/**
	 * @return the connection
	 */
	EntityConnection connection();

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
		 * Controls the additional HAVING condition. The condition supplier may return null in case of no condition.
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
	 * Builds an {@link EntityConditions}
	 */
	interface Builder {

		/**
		 * The first step in building an {@link EntityConditions}
		 */
		interface EntityTypeStep {

			/**
			 * @param entityType the underlying entity type
			 * @return the {@link ConnectionStep}
			 */
			ConnectionStep entityType(EntityType entityType);
		}

		/**
		 * The second step in building an {@link EntityConditions}
		 */
		interface ConnectionStep {

			/**
			 * @param connection a {@link EntityConnection} instance
			 * @return the {@link Builder}
			 */
			Builder connection(EntityConnection connection);
		}

		/**
		 * Excludes the given columns and foreign keys, no condition model being created for them.
		 * @param attributes the columns and foreign keys to exclude
		 * @return this builder
		 */
		Builder exclude(Attribute<?>... attributes);

		/**
		 * Configures the condition model for the given column, the builder received being initialized with the
		 * column defaults. Replaces any previous configuration of the given column.
		 * @param column the column
		 * @param condition configures the condition model builder
		 * @param <T> the column type
		 * @return this builder
		 */
		<T> Builder condition(Column<T> column, Consumer<ColumnConditionModel.Builder<T>> condition);

		/**
		 * Configures the condition model for the given foreign key, the builder received being initialized with the
		 * foreign key defaults, the caption included. Replaces any previous configuration of the given foreign key.
		 * @param foreignKey the foreign key
		 * @param condition configures the condition model builder
		 * @return this builder
		 */
		Builder condition(ForeignKey foreignKey, Consumer<ForeignKeyConditionModel.Builder> condition);

		/**
		 * @return a new {@link EntityConditions} instance
		 * @throws IllegalArgumentException in case an excluded or configured attribute is not a column or foreign key
		 * of the underlying entity, or is both excluded and configured
		 */
		EntityConditions build();
	}

	/**
	 * @return a {@link Builder.EntityTypeStep}
	 */
	static Builder.EntityTypeStep builder() {
		return DefaultEntityConditions.DefaultBuilder.ENTITY_TYPE_STEP;
	}
}
