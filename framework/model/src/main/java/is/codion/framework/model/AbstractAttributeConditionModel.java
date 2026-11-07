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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.Operator;

import org.jspecify.annotations.Nullable;

import java.text.Format;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The base of the attribute condition models, delegating to an underlying condition model.
 * @param <T> the attribute type
 */
abstract class AbstractAttributeConditionModel<T> implements AttributeConditionModel<T> {

	final ConditionModel<T> condition;

	AbstractAttributeConditionModel(ConditionModel<T> condition) {
		this.condition = requireNonNull(condition);
	}

	@Override
	public final State caseSensitive() {
		return condition.caseSensitive();
	}

	@Override
	public final Optional<Format> format() {
		return condition.format();
	}

	@Override
	public final Optional<String> dateTimePattern() {
		return condition.dateTimePattern();
	}

	@Override
	public final State autoEnable() {
		return condition.autoEnable();
	}

	@Override
	public final State locked() {
		return condition.locked();
	}

	@Override
	public final Class<T> type() {
		return condition.type();
	}

	@Override
	public final List<Operator> operators() {
		return condition.operators();
	}

	@Override
	public final State enabled() {
		return condition.enabled();
	}

	@Override
	public final void clear() {
		condition.clear();
	}

	@Override
	public final Value<Operator> operator() {
		return condition.operator();
	}

	@Override
	public final Operands<T> operands() {
		return condition.operands();
	}

	@Override
	public final SetCondition<T> set() {
		return condition.set();
	}

	@Override
	public final boolean accepts(@Nullable Comparable<T> value) {
		return condition.accepts(value);
	}

	@Override
	public final Observer<?> changed() {
		return condition.changed();
	}

	@Override
	public final Optional<String> caption() {
		return condition.caption();
	}
}
