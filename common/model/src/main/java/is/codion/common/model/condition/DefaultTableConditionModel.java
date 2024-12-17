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
import is.codion.common.observable.Observer;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

final class DefaultTableConditionModel<C> implements TableConditionModel<C> {

	private final Map<C, ConditionModel<?>> conditions;
	private final ValueSet<C> persist;
	private final ObservableState enabled;
	private final Event<?> changed = Event.event();

	DefaultTableConditionModel(Map<C, ConditionModel<?>> conditions) {
		this.conditions = unmodifiableMap(new HashMap<>(conditions));
		this.persist = ValueSet.<C>builder()
						.validator(new PersistValidator())
						.build();
		this.enabled = State.or(conditions.values().stream()
						.map(ConditionModel::enabled)
						.collect(Collectors.toList()));
		this.conditions.values().forEach(condition ->
						condition.changed().addListener(changed));
	}

	@Override
	public void clear() {
		conditions.entrySet().stream()
						.filter(entry -> !persist.contains(entry.getKey()))
						.map(Map.Entry::getValue)
						.forEach(ConditionModel::clear);
	}

	@Override
	public ObservableState enabled() {
		return enabled;
	}

	@Override
	public Map<C, ConditionModel<?>> get() {
		return conditions;
	}

	@Override
	public <T> ConditionModel<T> get(C identifier) {
		ConditionModel<T> condition = (ConditionModel<T>) conditions.get(requireNonNull(identifier));
		if (condition == null) {
			throw new IllegalArgumentException("No condition available for identifier: " + identifier);
		}

		return condition;
	}

	@Override
	public <T> Optional<ConditionModel<T>> optional(C identifier) {
		return Optional.ofNullable((ConditionModel<T>) conditions.get(requireNonNull(identifier)));
	}

	@Override
	public Observer<?> changed() {
		return changed.observer();
	}

	@Override
	public ValueSet<C> persist() {
		return persist;
	}

	private final class PersistValidator implements Value.Validator<Set<C>> {

		@Override
		public void validate(Set<C> identifiers) {
			if (!conditions.keySet().containsAll(identifiers)) {
				Set<C> missing = new HashSet<>(identifiers);
				missing.removeAll(conditions.keySet());

				throw new IllegalArgumentException("Unknown condition identifier(s): " + missing);
			}
		}
	}
}
