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

import is.codion.common.event.Event;
import is.codion.common.observer.Observer;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;

final class DefaultTableConditionModel<C> implements TableConditionModel<C> {

	private static final StateObserver DISABLED = State.state().observer();

	private final Map<C, ConditionModel<C, ?>> conditions;
	private final StateObserver enabled;
	private final Event<?> conditionChanged = Event.event();

	DefaultTableConditionModel(Collection<ConditionModel<C, ?>> conditions) {
		this.conditions = initializeColumnConditions(conditions);
		this.enabled = State.or(conditions.stream()
						.map(ConditionModel::enabled)
						.collect(Collectors.toList()));
		this.conditions.values().forEach(condition ->
						condition.conditionChanged().addListener(conditionChanged));
	}

	@Override
	public void clear() {
		conditions.values().forEach(ConditionModel::clear);
	}

	@Override
	public StateObserver enabled() {
		return enabled;
	}

	@Override
	public StateObserver enabled(C identifier) {
		ConditionModel<C, ?> condition = conditions.get(requireNonNull(identifier));

		return condition == null ? DISABLED : condition.enabled();
	}

	@Override
	public Map<C, ConditionModel<C, ?>> conditions() {
		return conditions;
	}

	@Override
	public <T> ConditionModel<C, T> condition(C identifier) {
		ConditionModel<C, T> condition = (ConditionModel<C, T>) conditions.get(identifier);
		if (condition == null) {
			throw new IllegalArgumentException("No condition available for column: " + identifier);
		}

		return condition;
	}

	@Override
	public Observer<?> conditionChanged() {
		return conditionChanged.observer();
	}

	private Map<C, ConditionModel<C, ?>> initializeColumnConditions(
					Collection<ConditionModel<C, ?>> conditions) {
		return unmodifiableMap(conditions.stream()
						.collect(Collectors.toMap(ConditionModel::identifier, identity())));
	}
}
