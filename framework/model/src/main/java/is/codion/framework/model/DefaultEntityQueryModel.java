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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.framework.domain.entity.condition.Condition.combination;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

final class DefaultEntityQueryModel implements EntityQueryModel {

	private static final Supplier<Condition> NULL_CONDITION_SUPPLIER = () -> null;

	private final EntityTableConditionModel conditionModel;
	private final AdditionalCondition additionalWhere = new DefaultAdditionalCondition();
	private final AdditionalCondition additionalHaving = new DefaultAdditionalCondition();
	private final Value<ObservableState> conditionEnabled;
	private final State conditionRequired = State.state();
	private final State conditionChanged = State.state();
	private final ValueSet<Attribute<?>> attributes = ValueSet.<Attribute<?>>builder()
					.validator(new AttributeValidator())
					.build();
	private final Value<OrderBy> orderBy;
	private final Value<Integer> limit = Value.nullable(LIMIT.get());
	private final Value<Function<EntityQueryModel, List<Entity>>> query = Value.nonNull(new DefaultQuery());

	private volatile RefreshConditions refreshConditions;

	DefaultEntityQueryModel(EntityTableConditionModel conditionModel) {
		this.conditionModel = requireNonNull(conditionModel);
		this.conditionEnabled = Value.nonNull(conditionModel.enabled());
		this.orderBy = createOrderBy();
		resetConditionChanged();
		Runnable conditionListener = this::onConditionChanged;
		conditionModel.changed().addListener(conditionListener);
		additionalWhere.addListener(conditionListener);
		additionalWhere.conjunction().addListener(conditionListener);
		additionalHaving.addListener(conditionListener);
		additionalHaving.conjunction().addListener(conditionListener);
	}

	@Override
	public EntityType entityType() {
		return conditionModel.entityType();
	}

	@Override
	public EntityConnectionProvider connectionProvider() {
		return conditionModel.connectionProvider();
	}

	@Override
	public List<Entity> get() {
		return query.getOrThrow().apply(this);
	}

	@Override
	public EntityTableConditionModel conditions() {
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
		return Select.where(createCondition(conditionModel.where(Conjunction.AND), additionalWhere))
						.having(createCondition(conditionModel.having(Conjunction.AND), additionalHaving))
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
		EntityDefinition definition = conditionModel.connectionProvider().entities().definition(conditionModel.entityType());
		return definition.orderBy()
						.map(Value::nonNull)
						.orElse(Value.nullable());
	}

	private void resetConditionChanged() {
		refreshConditions = new RefreshConditions();
		conditionChanged.set(false);
	}

	private void onConditionChanged() {
		conditionChanged.set(!Objects.equals(refreshConditions, new RefreshConditions()));
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

	private final class DefaultQuery implements Function<EntityQueryModel, List<Entity>> {

		@Override
		public List<Entity> apply(EntityQueryModel queryModel) {
			Select select = createSelect();
			if (conditionRequired.get() && !conditionEnabled.getOrThrow().get()) {
				resetConditionChanged();

				return emptyList();
			}
			List<Entity> items = conditionModel.connectionProvider().connection().select(select);
			resetConditionChanged();

			return items;
		}
	}

	private final class RefreshConditions {

		private final Condition where;
		private final Condition having;

		private RefreshConditions() {
			this.where = createCondition(conditionModel.where(Conjunction.AND), additionalWhere);
			this.having = createCondition(conditionModel.having(Conjunction.AND), additionalHaving);
		}

		@Override
		public boolean equals(Object object) {
			if (object == null || getClass() != object.getClass()) {
				return false;
			}
			RefreshConditions that = (RefreshConditions) object;

			return Objects.equals(where, that.where) && Objects.equals(having, that.having);
		}

		@Override
		public int hashCode() {
			return Objects.hash(where, having);
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
