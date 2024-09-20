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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

final class DefaultEntityQueryModel implements EntityQueryModel {

	private final EntityTableConditionModel conditionModel;
	private final Value<StateObserver> conditionEnabled;
	private final State conditionRequired = State.state();
	private final State conditionChanged = State.state();
	private final ValueSet<Attribute<?>> attributes = ValueSet.<Attribute<?>>builder()
					.validator(new AttributeValidator())
					.build();
	private final Value<OrderBy> orderBy;
	private final Value<Integer> limit = Value.value();

	private EntityConnection.Select refreshCondition;

	DefaultEntityQueryModel(EntityTableConditionModel conditionModel) {
		this.conditionModel = requireNonNull(conditionModel);
		this.conditionEnabled = Value.builder()
						.nonNull(conditionModel.enabled())
						.build();
		this.orderBy = createOrderBy();
		this.refreshCondition = createSelect(conditionModel);
		conditionModel.conditionChanged().addListener(this::onConditionChanged);
	}

	@Override
	public EntityType entityType() {
		return conditionModel.entityType();
	}

	@Override
	public List<Entity> query() throws DatabaseException {
		EntityConnection.Select select = createSelect(conditionModel);
		if (conditionRequired.get() && !conditionEnabled.get().get()) {
			updateRefreshSelect(select);

			return emptyList();
		}
		List<Entity> items = conditionModel.connectionProvider().connection().select(select);
		updateRefreshSelect(select);

		return items;
	}

	@Override
	public EntityTableConditionModel conditionModel() {
		return conditionModel;
	}

	@Override
	public ValueSet<Attribute<?>> attributes() {
		return attributes;
	}

	@Override
	public Value<Integer> limit() {
		return limit;
	}

	@Override
	public Value<OrderBy> orderBy() {
		return orderBy;
	}

	@Override
	public State conditionRequired() {
		return conditionRequired;
	}

	@Override
	public StateObserver conditionChanged() {
		return conditionChanged.observer();
	}

	@Override
	public Value<StateObserver> conditionEnabled() {
		return conditionEnabled;
	}

	private void updateRefreshSelect(EntityConnection.Select select) {
		refreshCondition = select;
		conditionChanged.set(false);
	}

	private EntityConnection.Select createSelect(EntityTableConditionModel conditionModel) {
		return EntityConnection.Select.where(conditionModel.where(Conjunction.AND))
						.having(conditionModel.having(Conjunction.AND))
						.attributes(attributes.get())
						.limit(limit.get())
						.orderBy(orderBy.get())
						.build();
	}

	private Value<OrderBy> createOrderBy() {
		EntityDefinition definition = conditionModel.connectionProvider().entities().definition(conditionModel.entityType());
		return definition.orderBy()
						.map(entityOrderBy -> Value.builder()
										.nonNull(entityOrderBy)
										.build())
						.orElse(Value.value());
	}

	private void onConditionChanged() {
		conditionChanged.set(!Objects.equals(refreshCondition, createSelect(conditionModel)));
	}

	private class AttributeValidator implements Value.Validator<Set<Attribute<?>>> {

		@Override
		public void validate(Set<Attribute<?>> attributes) {
			for (Attribute<?> attribute : attributes) {
				if (!attribute.entityType().equals(conditionModel.entityType())) {
					throw new IllegalArgumentException(attribute + " is not part of entity:  " + conditionModel.entityType());
				}
			}
		}
	}
}
