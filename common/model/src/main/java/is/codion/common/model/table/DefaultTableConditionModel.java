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
package is.codion.common.model.table;

import is.codion.common.event.Event;
import is.codion.common.observer.Observer;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableMap;
import static java.util.function.Function.identity;

final class DefaultTableConditionModel<C> implements TableConditionModel<C> {

	private final Map<C, ColumnConditionModel<C, ?>> conditionModels;
	private final Event<?> conditionChanged = Event.event();

	DefaultTableConditionModel(Collection<ColumnConditionModel<C, ?>> conditionModels) {
		this.conditionModels = initializeColumnConditionModels(conditionModels);
		this.conditionModels.values().forEach(conditionModel ->
						conditionModel.conditionChanged().addListener(conditionChanged));
	}

	@Override
	public void clear() {
		conditionModels.values().forEach(ColumnConditionModel::clear);
	}

	@Override
	public boolean enabled() {
		return conditionModels.values().stream()
						.anyMatch(model -> model.enabled().get());
	}

	@Override
	public boolean enabled(C identifier) {
		return conditionModels.containsKey(identifier) && conditionModels.get(identifier).enabled().get();
	}

	@Override
	public Map<C, ColumnConditionModel<C, ?>> conditionModels() {
		return conditionModels;
	}

	@Override
	public <T> ColumnConditionModel<C, T> conditionModel(C identifier) {
		ColumnConditionModel<C, T> conditionModel = (ColumnConditionModel<C, T>) conditionModels.get(identifier);
		if (conditionModel == null) {
			throw new IllegalArgumentException("No condition model available for column: " + identifier);
		}

		return conditionModel;
	}

	@Override
	public Observer<?> conditionChanged() {
		return conditionChanged.observer();
	}

	private Map<C, ColumnConditionModel<C, ?>> initializeColumnConditionModels(
					Collection<ColumnConditionModel<C, ?>> conditionModels) {
		return unmodifiableMap(conditionModels.stream()
						.collect(Collectors.toMap(ColumnConditionModel::identifier, identity())));
	}
}
