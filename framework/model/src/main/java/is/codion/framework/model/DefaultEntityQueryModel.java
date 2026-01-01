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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.ValueSet;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

final class DefaultEntityQueryModel implements EntityQueryModel {

	private final EntityConditionModel conditionModel;
	private final EntityDefinition entityDefinition;
	private final Value<ObservableState> conditionEnabled;
	private final State conditionRequired = State.state();
	private final DefaultSelectAttributes attributes = new DefaultSelectAttributes();

	private final Value<OrderBy> orderBy;
	private final Value<Integer> limit = Value.nullable(LIMIT.get());
	private final Value<Function<EntityQueryModel, List<Entity>>> dataSource = Value.nonNull(new DefaultDataSource());

	DefaultEntityQueryModel(EntityConditionModel conditionModel) {
		this.conditionModel = requireNonNull(conditionModel);
		this.entityDefinition = conditionModel.connectionProvider().entities().definition(conditionModel.entityType());
		this.conditionEnabled = Value.nonNull(conditionModel.enabled());
		this.orderBy = entityDefinition.orderBy()
						.map(Value::nonNull)
						.orElse(Value.nullable());
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
	public List<Entity> query() {
		if (conditionRequired.is() && !conditionEnabled.getOrThrow().is()) {
			conditionModel.modified().reset();

			return emptyList();
		}

		List<Entity> entities = dataSource.getOrThrow().apply(this);
		conditionModel.modified().reset();

		return entities;
	}

	@Override
	public EntityConditionModel condition() {
		return conditionModel;
	}

	@Override
	public SelectAttributes attributes() {
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
	public Value<ObservableState> conditionEnabled() {
		return conditionEnabled;
	}

	@Override
	public Value<Function<EntityQueryModel, List<Entity>>> dataSource() {
		return dataSource;
	}

	@Override
	public Select select() {
		return Select
						.where(conditionModel.where())
						.having(conditionModel.having())
						.attributes(attributes.defaults.get())
						.include(attributes.include.get())
						.exclude(attributes.exclude.get())
						.limit(limit.get())
						.orderBy(orderBy.get())
						.build();
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

	private static final class DefaultDataSource implements Function<EntityQueryModel, List<Entity>> {

		@Override
		public List<Entity> apply(EntityQueryModel queryModel) {
			return queryModel.connectionProvider().connection().select(queryModel.select());
		}
	}

	private final class DefaultSelectAttributes implements SelectAttributes {

		private final AttributeValidator attributeValidator = new AttributeValidator();
		private final ValueSet<Attribute<?>> defaults = ValueSet.<Attribute<?>>builder()
						.validator(attributeValidator)
						.build();
		private final ValueSet<Attribute<?>> include = ValueSet.<Attribute<?>>builder()
						.validator(attributeValidator)
						.build();
		private final ValueSet<Attribute<?>> exclude = ValueSet.<Attribute<?>>builder()
						.validator(attributeValidator)
						.build();
		private final Map<Attribute<?>, State> included = new HashMap<>();
		private final Map<Attribute<?>, State> excluded = new HashMap<>();

		@Override
		public ValueSet<Attribute<?>> defaults() {
			return defaults;
		}

		@Override
		public ValueSet<Attribute<?>> include() {
			return include;
		}

		@Override
		public ValueSet<Attribute<?>> exclude() {
			return exclude;
		}

		@Override
		public State included(Attribute<?> attribute) {
			attributeValidator.validate(singleton(requireNonNull(attribute)));

			return included.computeIfAbsent(attribute, k -> State.contains(include, attribute));
		}

		@Override
		public State excluded(Attribute<?> attribute) {
			attributeValidator.validate(singleton(requireNonNull(attribute)));

			return excluded.computeIfAbsent(attribute, k -> State.contains(exclude, attribute));
		}
	}
}
