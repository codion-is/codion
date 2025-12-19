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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.Operator;
import is.codion.framework.domain.entity.attribute.Attribute;

import java.text.Format;
import java.util.List;
import java.util.Optional;

/**
 * @param <T> the attribute type
 */
public interface AttributeConditionModel<T> extends ConditionModel<T> {

	/**
	 * @return the attribute
	 */
	Attribute<T> attribute();

	/**
	 * @return the underlying condition model
	 */
	ConditionModel<T> condition();

	@Override
	default State caseSensitive() {
		return condition().caseSensitive();
	}

	@Override
	default Optional<Format> format() {
		return condition().format();
	}

	@Override
	default Optional<String> dateTimePattern() {
		return condition().dateTimePattern();
	}

	@Override
	default State autoEnable() {
		return condition().autoEnable();
	}

	@Override
	default State locked() {
		return condition().locked();
	}

	@Override
	default Class<T> valueClass() {
		return condition().valueClass();
	}

	@Override
	default List<Operator> operators() {
		return condition().operators();
	}

	@Override
	default State enabled() {
		return condition().enabled();
	}

	@Override
	default void clear() {
		condition().clear();
	}

	@Override
	default Value<Operator> operator() {
		return condition().operator();
	}

	@Override
	default Operands<T> operands() {
		return condition().operands();
	}

	@Override
	default SetCondition<T> set() {
		return condition().set();
	}

	@Override
	default boolean accepts(Comparable<T> value) {
		return condition().accepts(value);
	}
}
