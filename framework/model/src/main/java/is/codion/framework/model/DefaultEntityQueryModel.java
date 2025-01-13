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
 * Copyright (c) 2024 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.framework.domain.entity.condition.Condition.combination;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

final class DefaultEntityQueryModel implements EntityQueryModel {

	private static final Supplier<Condition> NULL_CONDITION_SUPPLIER = () -> null;

	private final EntityConditionModel entityConditionModel;
	private final AdditionalCondition additionalWhere = new DefaultAdditionalCondition();
	private final AdditionalCondition additionalHaving = new DefaultAdditionalCondition();
	private final Value<ObservableState> conditionEnabled;
	private final State conditionRequired = State.state();
	private final State conditionChanged = State.state();
	private final ValueSet<Attribute<?>> attributes = ValueSet.<Attribute<?>>builder()
					.validator(new AttributeValidator())
					.build();
	private final Value<OrderBy> orderBy;
	private final Value<Integer> limit = Value.nullable();
	private final Value<Function<EntityQueryModel, List<Entity>>> query = Value.nonNull(new DefaultQuery());
	private final List<Condition> refreshConditions = new ArrayList<>(2);

	DefaultEntityQueryModel(EntityConditionModel entityConditionModel) {
		this.entityConditionModel = requireNonNull(entityConditionModel);
		this.conditionEnabled = Value.nonNull(entityConditionModel.enabled());
		this.orderBy = createOrderBy();
		resetConditionChanged(refreshConditions());
		Runnable conditionListener = this::onConditionChanged;
		entityConditionModel.changed().addListener(conditionListener);
		additionalWhere.addListener(conditionListener);
		additionalWhere.conjunction().addListener(conditionListener);
		additionalHaving.addListener(conditionListener);
		additionalHaving.conjunction().addListener(conditionListener);
	}

	@Override
	public EntityType entityType() {
		return entityConditionModel.entityType();
	}

	@Override
	public EntityConnectionProvider connectionProvider() {
		return entityConditionModel.connectionProvider();
	}

	@Override
	public List<Entity> get() {
		return query.getOrThrow().apply(this);
	}

	@Override
	public EntityConditionModel conditions() {
		return entityConditionModel;
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
	public ObservableState conditionChanged() {
		return conditionChanged.observable();
	}

	@Override
	public Value<ObservableState> conditionEnabled() {
		return conditionEnabled;
	}

	@Override
	public Value<Function<EntityQueryModel, List<Entity>>> query() {
		return query;
	}

	@Override
	public AdditionalCondition where() {
		return additionalWhere;
	}

	@Override
	public AdditionalCondition having() {
		return additionalHaving;
	}

	Select createSelect() {
		return Select.where(createCondition(entityConditionModel.where(Conjunction.AND), additionalWhere))
						.having(createCondition(entityConditionModel.having(Conjunction.AND), additionalHaving))
						.attributes(attributes.get())
						.limit(limit.get())
						.orderBy(orderBy.get())
						.build();
	}

	private static Condition createCondition(Condition entityCondition, AdditionalCondition additional) {
		return additional.optional()
						.map(Supplier::get)
						.map(condition -> combination(additional.conjunction().getOrThrow(), entityCondition, condition))
						.map(Condition.class::cast)
						.orElse(entityCondition);
	}

	private Value<OrderBy> createOrderBy() {
		EntityDefinition definition = entityConditionModel.connectionProvider().entities().definition(entityConditionModel.entityType());
		return definition.orderBy()
						.map(Value::nonNull)
						.orElse(Value.nullable());
	}

	private void resetConditionChanged(List<Condition> conditions) {
		refreshConditions.clear();
		refreshConditions.addAll(conditions);
		conditionChanged.set(false);
	}

	private void onConditionChanged() {
		conditionChanged.set(!Objects.equals(refreshConditions, refreshConditions()));
	}

	private List<Condition> refreshConditions() {
		return asList(
						createCondition(entityConditionModel.where(Conjunction.AND), additionalWhere),
						createCondition(entityConditionModel.having(Conjunction.AND), additionalHaving));
	}

	private class AttributeValidator implements Value.Validator<Set<Attribute<?>>> {

		@Override
		public void validate(Set<Attribute<?>> attributes) {
			for (Attribute<?> attribute : attributes) {
				if (!attribute.entityType().equals(entityConditionModel.entityType())) {
					throw new IllegalArgumentException(attribute + " is not part of entity:  " + entityConditionModel.entityType());
				}
			}
		}
	}

	private final class DefaultQuery implements Function<EntityQueryModel, List<Entity>> {

		@Override
		public List<Entity> apply(EntityQueryModel queryModel) {
			Select select = createSelect();
			if (conditionRequired.get() && !conditionEnabled.getOrThrow().get()) {
				resetConditionChanged(refreshConditions());

				return emptyList();
			}
			List<Entity> items = entityConditionModel.connectionProvider().connection().select(select);
			resetConditionChanged(refreshConditions());

			return items;
		}
	}

	private static final class DefaultAdditionalCondition extends AbstractValue<Supplier<Condition>> implements AdditionalCondition {

		private Supplier<Condition> condition = NULL_CONDITION_SUPPLIER;

		private final Value<Conjunction> conjunction = Value.nonNull(Conjunction.AND);

		private DefaultAdditionalCondition() {
			super(NULL_CONDITION_SUPPLIER, Notify.WHEN_SET);
		}

		@Override
		public Value<Conjunction> conjunction() {
			return conjunction;
		}

		@Override
		protected Supplier<Condition> getValue() {
			return condition;
		}

		@Override
		protected void setValue(Supplier<Condition> condition) {
			this.condition = condition;
		}
	}
}
