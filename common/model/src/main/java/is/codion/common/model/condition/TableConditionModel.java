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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.model.condition;

import is.codion.common.observable.Observer;
import is.codion.common.state.ObservableState;
import is.codion.common.value.ValueSet;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Manages a set of {@link ConditionModel}s for table columns.
 * @param <C> the condition identifier type
 * @see #tableConditionModel(Supplier)
 */
public interface TableConditionModel<C> {

	/**
	 * @return an unmodifiable view of the available condition models
	 */
	Map<C, ConditionModel<?>> get();

	/**
	 * The condition model associated with {@code identifier}
	 * @param <T> the condition value type
	 * @param identifier the identifier for which to retrieve the {@link ConditionModel}
	 * @return the {@link ConditionModel} associated with the given identifier
	 * @throws IllegalArgumentException in case no condition model exists for the given identifier
	 */
	<T> ConditionModel<T> get(C identifier);

	/**
	 * The condition model associated with {@code identifier}
	 * @param <T> the condition value type
	 * @param identifier the identifier for which to retrieve the {@link ConditionModel}
	 * @return the {@link ConditionModel} for the {@code identifier} or an empty Optional in case one is not available
	 */
	<T> Optional<ConditionModel<T>> optional(C identifier);

	/**
	 * Clears the search state of all non-persistant condition models, disables them and
	 * resets the operator to {@link is.codion.common.Operator#EQUAL}.
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
	ValueSet<C> persist();

	/**
	 * Instantiates a new {@link TableConditionModel}
	 * @param conditionModelFactory supplies the condition models mapped to their respective column identifiers
	 * @param <C> the condition identifier type
	 * @return a new {@link TableConditionModel}
	 */
	static <C> TableConditionModel<C> tableConditionModel(Supplier<Map<C, ConditionModel<?>>> conditionModelFactory) {
		return new DefaultTableConditionModel<>(requireNonNull(conditionModelFactory));
	}
}
