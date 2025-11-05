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
import is.codion.common.observer.Observer;
import is.codion.common.state.ObservableState;
import is.codion.common.utilities.Conjunction;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Factory for {@link EntityTableConditionModel} instances via
 * {@link EntityTableConditionModel#entityTableConditionModel(EntityType, EntityConnectionProvider)}
 */
public interface EntityTableConditionModel {

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
	 * @return an unmodifiable view of the available condition models
	 */
	Map<Attribute<?>, ConditionModel<?>> get();

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
	 * The condition model associated with {@code attribute}
	 * @param <T> the condition value type
	 * @param attribute the attribute for which to retrieve the {@link ConditionModel}
	 * @return the {@link ConditionModel} for the {@code attribute} or an empty Optional in case one is not available
	 */
	<T> Optional<ConditionModel<T>> optional(Attribute<T> attribute);

	/**
	 * Clears the search state of all non-persistant condition models, disables them and
	 * resets the operator to the inital one.
	 * @see #persist()
	 */
	void clear();

	/**
	 * @return an {@link ObservableState} enabled when any of the underlying condition models are enabled
	 */
	ObservableState enabled();

	/**
	 * @return an observer notified each time the condition changes
	 */
	Observer<?> changed();

	/**
	 * @return a {@link ValueSet} controlling the identifiers of conditions which should persist when this condition model is cleared
	 * @see #clear()
	 */
	ValueSet<Attribute<?>> persist();

	/**
	 * @return the underlying {@link TableConditionModel}
	 */
	TableConditionModel<Attribute<?>> conditionModel();

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
