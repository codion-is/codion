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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Operator;
import is.codion.common.event.EventObserver;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.text.Format;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * An abstract base class for {@link ForeignKey} based {@link ColumnConditionModel}s.
 */
public abstract class AbstractForeignKeyConditionModel implements ColumnConditionModel<ForeignKey, Entity> {

	private final ColumnConditionModel<ForeignKey, Entity> conditionModel;

	protected AbstractForeignKeyConditionModel(ForeignKey foreignKey) {
		conditionModel = ColumnConditionModel.builder(requireNonNull(foreignKey), Entity.class)
						.operators(asList(Operator.EQUAL, Operator.NOT_EQUAL, Operator.IN, Operator.NOT_IN))
						.build();
	}

	@Override
	public final ForeignKey columnIdentifier() {
		return conditionModel.columnIdentifier();
	}

	@Override
	public final State caseSensitive() {
		return conditionModel.caseSensitive();
	}

	@Override
	public final Format format() {
		return conditionModel.format();
	}

	@Override
	public final String dateTimePattern() {
		return conditionModel.dateTimePattern();
	}

	@Override
	public final State locked() {
		return conditionModel.locked();
	}

	@Override
	public final Class<Entity> columnClass() {
		return conditionModel.columnClass();
	}

	@Override
	public final void setEqualValue(Entity value) {
		conditionModel.setEqualValue(value);
	}

	@Override
	public final Entity getEqualValue() {
		return conditionModel.getEqualValue();
	}

	@Override
	public final void setInValues(Collection<Entity> values) {
		conditionModel.setInValues(values);
	}

	@Override
	public final Collection<Entity> getInValues() {
		return conditionModel.getInValues();
	}

	@Override
	public final void setUpperBound(Entity value) {
		conditionModel.setUpperBound(value);
	}

	@Override
	public final Entity getUpperBound() {
		return conditionModel.getUpperBound();
	}

	@Override
	public final void setLowerBound(Entity value) {
		conditionModel.setLowerBound(value);
	}

	@Override
	public final Entity getLowerBound() {
		return conditionModel.getLowerBound();
	}

	@Override
	public final Value<Operator> operator() {
		return conditionModel.operator();
	}

	@Override
	public final List<Operator> operators() {
		return conditionModel.operators();
	}

	@Override
	public final char wildcard() {
		return conditionModel.wildcard();
	}

	@Override
	public final State enabled() {
		return conditionModel.enabled();
	}

	@Override
	public final Value<AutomaticWildcard> automaticWildcard() {
		return conditionModel.automaticWildcard();
	}

	@Override
	public final State autoEnable() {
		return conditionModel.autoEnable();
	}

	@Override
	public final void clear() {
		conditionModel.clear();
	}

	@Override
	public final boolean accepts(Comparable<Entity> columnValue) {
		return conditionModel.accepts(columnValue);
	}

	@Override
	public Value<Entity> equalValue() {
		return conditionModel.equalValue();
	}

	@Override
	public final ValueSet<Entity> inValues() {
		return conditionModel.inValues();
	}

	@Override
	public final Value<Entity> lowerBoundValue() {
		return conditionModel.lowerBoundValue();
	}

	@Override
	public final Value<Entity> upperBoundValue() {
		return conditionModel.upperBoundValue();
	}

	@Override
	public final EventObserver<?> conditionChangedEvent() {
		return conditionModel.conditionChangedEvent();
	}

	/**
	 * @return the search model controlling the in values
	 */
	public abstract EntitySearchModel inSearchModel();
}
