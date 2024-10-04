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
 * Copyright (c) 2016 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.ConditionModel.Operands;
import is.codion.common.model.condition.TableConditions;
import is.codion.common.observer.Mutable;
import is.codion.common.observer.Observer;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.ColumnCondition;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.model.condition.TableConditions.tableConditions;
import static is.codion.framework.domain.entity.condition.Condition.all;
import static is.codion.framework.domain.entity.condition.Condition.combination;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default {@link EntityConditions} implementation
 */
final class DefaultEntityConditions implements EntityConditions {

	private static final Supplier<Condition> NULL_CONDITION_SUPPLIER = () -> null;

	private final EntityDefinition entityDefinition;
	private final EntityConnectionProvider connectionProvider;
	private final TableConditions<Attribute<?>> tableConditions;
	private final Event<?> conditionChangedEvent = Event.event();
	private final AdditionalCondition additionalWhere = new DefaultAdditionalWhereCondition();
	private final AdditionalCondition additionalHaving = new DefaultAdditionalHavingCondition();
	private final NoneAggregateColumn noneAggregateColumn = new NoneAggregateColumn();
	private final AggregateColumn aggregateColumn = new AggregateColumn();

	DefaultEntityConditions(EntityType entityType, EntityConnectionProvider connectionProvider,
													ColumnConditionFactory<Attribute<?>> columnConditionFactory) {
		this.entityDefinition = connectionProvider.entities().definition(requireNonNull(entityType, "entityType"));
		this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
		this.tableConditions = tableConditions(createConditionModels(entityType, columnConditionFactory));
		bindEvents();
	}

	@Override
	public EntityType entityType() {
		return entityDefinition.entityType();
	}

	@Override
	public EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	@Override
	public <T> boolean setEqualOperand(Attribute<T> attribute, T operand) {
		requireNonNull(attribute);
		boolean aggregateColumn = attribute instanceof Column && entityDefinition.columns().definition((Column<?>) attribute).aggregate();
		Condition condition = aggregateColumn ? having(Conjunction.AND) : where(Conjunction.AND);
		tableConditions.optional(attribute)
						.ifPresent(conditionModel -> {
							conditionModel.operator().set(Operator.EQUAL);
							conditionModel.operands().equal().set(operand);
							conditionModel.enabled().set(operand != null);
						});

		return !condition.equals(aggregateColumn ? having(Conjunction.AND) : where(Conjunction.AND));
	}

	@Override
	public <T> boolean setInOperands(Attribute<T> attribute, Collection<T> operands) {
		requireNonNull(attribute);
		requireNonNull(operands);
		boolean aggregateColumn = attribute instanceof Column && entityDefinition.columns().definition((Column<?>) attribute).aggregate();
		Condition condition = aggregateColumn ? having(Conjunction.AND) : where(Conjunction.AND);
		tableConditions.optional(attribute)
						.map(conditionModel -> (ConditionModel<T>) conditionModel)
						.ifPresent(conditionModel -> {
							conditionModel.operator().set(Operator.IN);
							conditionModel.operands().in().set(operands);
							conditionModel.enabled().set(!operands.isEmpty());
						});

		return !condition.equals(aggregateColumn ? having(Conjunction.AND) : where(Conjunction.AND));
	}

	@Override
	public Condition where(Conjunction conjunction) {
		return createCondition(requireNonNull(conjunction), noneAggregateColumn, additionalWhere);
	}

	@Override
	public Condition having(Conjunction conjunction) {
		return createCondition(requireNonNull(conjunction), aggregateColumn, additionalHaving);
	}

	@Override
	public AdditionalCondition additionalWhere() {
		return additionalWhere;
	}

	@Override
	public AdditionalCondition additionalHaving() {
		return additionalHaving;
	}

	@Override
	public Map<Attribute<?>, ConditionModel<?>> get() {
		return tableConditions.get();
	}

	@Override
	public <T> ConditionModel<T> get(Attribute<?> identifier) {
		return tableConditions.get(requireNonNull(identifier));
	}

	@Override
	public <T> Optional<ConditionModel<T>> optional(Attribute<?> identifier) {
		return tableConditions.optional(requireNonNull(identifier));
	}

	@Override
	public <T> ConditionModel<T> attribute(Attribute<T> attribute) {
		return get(attribute);
	}

	@Override
	public StateObserver enabled() {
		return tableConditions.enabled();
	}

	@Override
	public Observer<?> changed() {
		return tableConditions.changed();
	}

	@Override
	public void clear() {
		tableConditions.clear();
	}

	private Condition createCondition(Conjunction conjunction, Predicate<Attribute<?>> columnType,
																		AdditionalCondition additional) {
		Condition columnConditions = columnConditions(conjunction, columnType);
		Condition additionalCondition = additional.get().get();
		if (additionalCondition == null) {
			return columnConditions;
		}

		return combination(additional.conjunction().get(), columnConditions, additionalCondition);
	}

	private Condition columnConditions(Conjunction conjunction, Predicate<Attribute<?>> columnType) {
		List<Condition> conditions = tableConditions.get().entrySet().stream()
						.filter(entry -> columnType.test(entry.getKey()))
						.filter(entry -> entry.getValue().enabled().get())
						.map(entry -> condition(entry.getValue(), entry.getKey()))
						.collect(toList());
		switch (conditions.size()) {
			case 0:
				return all(entityDefinition.entityType());
			case 1:
				return conditions.get(0);
			default:
				return combination(conjunction, conditions);
		}
	}

	private void bindEvents() {
		tableConditions.get().values()
						.forEach(conditionModel -> conditionModel.changed().addListener(conditionChangedEvent));
		additionalWhere.addListener(conditionChangedEvent);
		additionalHaving.addListener(conditionChangedEvent);
	}

	private Map<Attribute<?>, ConditionModel<?>> createConditionModels(EntityType entityType,
																																		 ColumnConditionFactory<Attribute<?>> columnConditionFactory) {
		Map<Attribute<?>, ConditionModel<?>> models = new HashMap<>();
		EntityDefinition definition = connectionProvider.entities().definition(entityType);
		definition.columns().definitions().forEach(columnDefinition ->
						columnConditionFactory.create(columnDefinition.attribute())
										.ifPresent(conditionModel -> models.put(columnDefinition.attribute(), conditionModel)));
		definition.foreignKeys().definitions().forEach(foreignKeyDefinition ->
						columnConditionFactory.create(foreignKeyDefinition.attribute())
										.ifPresent(conditionModel -> models.put(foreignKeyDefinition.attribute(), conditionModel)));

		return models;
	}

	private static Condition condition(ConditionModel<?> conditionModel, Attribute<?> identifier) {
		if (identifier instanceof ForeignKey) {
			return foreignKeyCondition((ConditionModel<Entity>) conditionModel, (ForeignKey) identifier);
		}

		return columnCondition(conditionModel, identifier);
	}

	private static Condition foreignKeyCondition(ConditionModel<Entity> conditionModel, ForeignKey foreignKey) {
		Entity equalOperand = conditionModel.operands().equal().get();
		Collection<Entity> inOperands = conditionModel.operands().in().get();
		switch (conditionModel.operator().get()) {
			case EQUAL:
				return equalOperand == null ? foreignKey.isNull() : foreignKey.equalTo(equalOperand);
			case IN:
				return inOperands.isEmpty() ? foreignKey.isNull() : foreignKey.in(inOperands);
			case NOT_EQUAL:
				return equalOperand == null ? foreignKey.isNotNull() : foreignKey.notEqualTo(equalOperand);
			case NOT_IN:
				return inOperands.isEmpty() ? foreignKey.isNotNull() : foreignKey.notIn(inOperands);
			default:
				throw new IllegalArgumentException("Unsupported operator: " + conditionModel.operator().get() + " for foreign key condition");
		}
	}

	private static <T> ColumnCondition<T> columnCondition(ConditionModel<T> conditionModel, Attribute<?> identifier) {
		Column<T> column = (Column<T>) identifier;
		Operands<T> operands = conditionModel.operands();
		switch (conditionModel.operator().get()) {
			case EQUAL:
				return equalCondition(conditionModel, column);
			case NOT_EQUAL:
				return notEqualCondition(conditionModel, column);
			case LESS_THAN:
				return column.lessThan(operands.upperBound().get());
			case LESS_THAN_OR_EQUAL:
				return column.lessThanOrEqualTo(operands.upperBound().get());
			case GREATER_THAN:
				return column.greaterThan(operands.lowerBound().get());
			case GREATER_THAN_OR_EQUAL:
				return column.greaterThanOrEqualTo(operands.lowerBound().get());
			case BETWEEN_EXCLUSIVE:
				return column.betweenExclusive(operands.lowerBound().get(), operands.upperBound().get());
			case BETWEEN:
				return column.between(operands.lowerBound().get(), operands.upperBound().get());
			case NOT_BETWEEN_EXCLUSIVE:
				return column.notBetweenExclusive(operands.lowerBound().get(), operands.upperBound().get());
			case NOT_BETWEEN:
				return column.notBetween(operands.lowerBound().get(), operands.upperBound().get());
			case IN:
				return inCondition(conditionModel, column);
			case NOT_IN:
				return notInCondition(conditionModel, column);
			default:
				throw new IllegalArgumentException("Unknown operator: " + conditionModel.operator().get());
		}
	}

	private static <T> ColumnCondition<T> equalCondition(ConditionModel<T> conditionModel,
																											 Column<T> column) {
		T equalOperand = conditionModel.operands().equal().get();
		if (equalOperand == null) {
			return column.isNull();
		}
		if (column.type().isString()) {
			return singleStringEqualCondition(conditionModel, column, (String) equalOperand);
		}
		if (column.type().isCharacter()) {
			return singleCharacterEqualCondition(conditionModel, column, (Character) equalOperand);
		}

		return column.equalTo(equalOperand);
	}

	private static <T> ColumnCondition<T> notEqualCondition(ConditionModel<T> conditionModel,
																													Column<T> column) {
		T equalOperand = conditionModel.operands().equal().get();
		if (equalOperand == null) {
			return column.isNotNull();
		}
		if (column.type().isString()) {
			return singleStringNotEqualCondition(conditionModel, column, (String) equalOperand);
		}
		if (column.type().isCharacter()) {
			return singleCharacterNotEqualCondition(conditionModel, column, (Character) equalOperand);
		}

		return column.notEqualTo(equalOperand);
	}

	private static <T> ColumnCondition<T> singleStringEqualCondition(ConditionModel<T> conditionModel,
																																	 Column<T> column, String value) {
		boolean caseSensitive = conditionModel.caseSensitive().get();
		if (containsWildcards(value)) {
			return (ColumnCondition<T>) (caseSensitive ? column.like(value) : column.likeIgnoreCase(value));
		}

		return caseSensitive ? column.equalTo((T) value) : (ColumnCondition<T>) column.equalToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> singleCharacterEqualCondition(ConditionModel<T> conditionModel,
																																			Column<T> column, Character value) {
		return conditionModel.caseSensitive().get() ? column.equalTo((T) value) : (ColumnCondition<T>) column.equalToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> singleStringNotEqualCondition(ConditionModel<T> conditionModel,
																																			Column<T> column, String value) {
		boolean caseSensitive = conditionModel.caseSensitive().get();
		if (containsWildcards(value)) {
			return (ColumnCondition<T>) (caseSensitive ? column.notLike(value) : column.notLikeIgnoreCase(value));
		}

		return caseSensitive ? column.notEqualTo((T) value) : (ColumnCondition<T>) column.notEqualToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> singleCharacterNotEqualCondition(ConditionModel<T> conditionModel,
																																				 Column<T> column, Character value) {
		return conditionModel.caseSensitive().get() ? column.notEqualTo((T) value) : (ColumnCondition<T>) column.notEqualToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> inCondition(ConditionModel<T> conditionModel, Column<T> column) {
		if (column.type().isString()) {
			Column<String> stringColumn = (Column<String>) column;
			Collection<String> inOperands = (Collection<String>) conditionModel.operands().in().get();

			return (ColumnCondition<T>) (conditionModel.caseSensitive().get() ?
							stringColumn.in(inOperands) :
							stringColumn.inIgnoreCase(inOperands));
		}

		return column.in(conditionModel.operands().in().get());
	}

	private static <T> ColumnCondition<T> notInCondition(ConditionModel<T> conditionModel, Column<T> column) {
		if (column.type().isString()) {
			Column<String> stringColumn = (Column<String>) column;
			Collection<String> inOperands = (Collection<String>) conditionModel.operands().in().get();

			return (ColumnCondition<T>) (conditionModel.caseSensitive().get() ?
							stringColumn.notIn(inOperands) :
							stringColumn.notInIgnoreCase(inOperands));
		}

		return column.notIn(conditionModel.operands().in().get());
	}

	private static boolean containsWildcards(String value) {
		return value != null && (value.contains("%") || value.contains("_"));
	}

	private final class AggregateColumn implements Predicate<Attribute<?>> {

		@Override
		public boolean test(Attribute<?> attribute) {
			return (attribute instanceof Column) &&
							entityDefinition.columns().definition((Column<?>) attribute).aggregate();
		}
	}

	private final class NoneAggregateColumn implements Predicate<Attribute<?>> {

		@Override
		public boolean test(Attribute<?> attribute) {
			return !(attribute instanceof Column) ||
							!entityDefinition.columns().definition((Column<?>) attribute).aggregate();
		}
	}

	private static final class MutableConjunction implements Mutable<Conjunction> {

		private final Value<Conjunction> value = Value.builder()
						.nonNull(Conjunction.AND)
						.build();

		@Override
		public void set(Conjunction conjunction) {
			value.set(conjunction);
		}

		@Override
		public Conjunction get() {
			return value.get();
		}

		@Override
		public Observer<Conjunction> observer() {
			return value.observer();
		}
	}

	private static final class DefaultAdditionalWhereCondition implements AdditionalCondition {

		private final Value<Supplier<Condition>> value = Value.builder()
						.nonNull(NULL_CONDITION_SUPPLIER)
						.build();
		private final Mutable<Conjunction> conjunction = new MutableConjunction();

		@Override
		public Mutable<Conjunction> conjunction() {
			return conjunction;
		}

		@Override
		public void set(Supplier<Condition> condition) {
			value.set(condition);
		}

		@Override
		public Supplier<Condition> get() {
			return value.get();
		}

		@Override
		public Observer<Supplier<Condition>> observer() {
			return value.observer();
		}
	}

	private static final class DefaultAdditionalHavingCondition implements AdditionalCondition {

		private final Value<Supplier<Condition>> value = Value.builder()
						.nonNull(NULL_CONDITION_SUPPLIER)
						.build();
		private final Mutable<Conjunction> conjunction = new MutableConjunction();

		@Override
		public Mutable<Conjunction> conjunction() {
			return conjunction;
		}

		@Override
		public void set(Supplier<Condition> condition) {
			value.set(condition);
		}

		@Override
		public Supplier<Condition> get() {
			return value.get();
		}

		@Override
		public Observer<Supplier<Condition>> observer() {
			return value.observer();
		}
	}
}
