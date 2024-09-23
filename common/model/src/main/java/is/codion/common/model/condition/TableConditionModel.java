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
package is.codion.common.model.condition;

import is.codion.common.observer.Observer;
import is.codion.common.state.StateObserver;

import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * @param <C> the condition identifier type
 * @see #tableConditionModel(Collection)
 */
public interface TableConditionModel<C> {

	/**
	 * @return the available condition model identifiers
	 */
	Collection<C> identifiers();

	/**
	 * The condition model associated with {@code identifier}
	 * @param <T> the condition value type
	 * @param identifier the identifier for which to retrieve the {@link ConditionModel}
	 * @return the {@link ConditionModel} for the {@code identifier}
	 * @throws IllegalArgumentException in case no condition model exists for the given identifier
	 */
	<T> ConditionModel<C, T> get(C identifier);

		/**
	 * The condition model associated with {@code identifier}
	 * @param <T> the condition value type
	 * @param identifier the identifier for which to retrieve the {@link ConditionModel}
	 * @return the {@link ConditionModel} for the {@code identifier} or an empty Optional in case one is not available
	 */
	<T> Optional<ConditionModel<C, T>> optional(C identifier);

	/**
	 * Clears the search state of all the condition models, disables them and
	 * resets the operator to {@link is.codion.common.Operator#EQUAL}
	 */
	void clear();

	/**
	 * @return a {@link StateObserver} enabled when any of the underlying condition models are enabled
	 */
	StateObserver enabled();

	/**
	 * @return an observer notified each time the condition changes
	 */
	Observer<?> changed();

	/**
	 * Instantiates a new {@link TableConditionModel}
	 * @param conditionModels the condition models
	 * @param <C> the condition identifier type
	 * @return a new {@link TableConditionModel}
	 */
	static <C> TableConditionModel<C> tableConditionModel(Collection<ConditionModel<C, ?>> conditionModels) {
		return new DefaultTableConditionModel<>(requireNonNull(conditionModels));
	}
}
