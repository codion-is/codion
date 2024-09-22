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
import is.codion.common.model.condition.TableConditionModel;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static is.codion.common.model.condition.TableConditionModel.tableConditionModel;
import static is.codion.framework.domain.entity.condition.Condition.all;
import static is.codion.framework.domain.entity.condition.Condition.combination;
import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityConditionModel} implementation
 */
final class DefaultEntityConditionModel implements EntityConditionModel {

	private static final Supplier<Condition> NULL_CONDITION_SUPPLIER = () -> null;

	private final EntityDefinition entityDefinition;
	private final EntityConnectionProvider connectionProvider;
	private final TableConditionModel<Attribute<?>> tableConditionModel;
	private final Event<?> conditionChangedEvent = Event.event();
	private final Value<Supplier<Condition>> additionalWhere = Value.builder()
					.nonNull(NULL_CONDITION_SUPPLIER)
					.build();
	private final Value<Supplier<Condition>> additionalHaving = Value.builder()
					.nonNull(NULL_CONDITION_SUPPLIER)
					.build();
	private final NoneAggregatePredicate noneAggregatePredicate = new NoneAggregatePredicate();
	private final AggregatePredicate aggregatePredicate = new AggregatePredicate();

	DefaultEntityConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider,
															ConditionModel.Factory<Attribute<?>> conditionModelFactory) {
		this.entityDefinition = connectionProvider.entities().definition(requireNonNull(entityType, "entityType"));
		this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
		this.tableConditionModel = tableConditionModel(createConditionModels(entityType, conditionModelFactory));
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
		ConditionModel<Attribute<?>, T> conditionModel =
						(ConditionModel<Attribute<?>, T>) tableConditionModel.conditions().get(attribute);
		if (conditionModel != null) {
			conditionModel.operator().set(Operator.EQUAL);
			conditionModel.operands().equal().set(operand);
			conditionModel.enabled().set(operand != null);
		}
		return !condition.equals(aggregateColumn ? having(Conjunction.AND) : where(Conjunction.AND));
	}

	@Override
	public <T> boolean setInOperands(Attribute<T> attribute, Collection<T> operands) {
		requireNonNull(attribute);
		requireNonNull(operands);
		boolean aggregateColumn = attribute instanceof Column && entityDefinition.columns().definition((Column<?>) attribute).aggregate();
		Condition condition = aggregateColumn ? having(Conjunction.AND) : where(Conjunction.AND);
		ConditionModel<Attribute<?>, T> conditionModel =
						(ConditionModel<Attribute<?>, T>) tableConditionModel.conditions().get(attribute);
		if (conditionModel != null) {
			conditionModel.operator().set(Operator.IN);
			conditionModel.operands().in().set(operands);
			conditionModel.enabled().set(!operands.isEmpty());
		}
		return !condition.equals(aggregateColumn ? having(Conjunction.AND) : where(Conjunction.AND));
	}

	@Override
	public Condition where(Conjunction conjunction) {
		requireNonNull(conjunction);
		Collection<Condition> conditions = conditions(noneAggregatePredicate, additionalWhere.get().get());

		return conditions.isEmpty() ? all(entityDefinition.entityType()) : combination(conjunction, conditions);
	}

	@Override
	public Condition having(Conjunction conjunction) {
		requireNonNull(conjunction);
		Collection<Condition> conditions = conditions(aggregatePredicate, additionalHaving.get().get());

		return conditions.isEmpty() ? all(entityDefinition.entityType()) : combination(conjunction, conditions);
	}

	@Override
	public Value<Supplier<Condition>> additionalWhere() {
		return additionalWhere;
	}

	@Override
	public Value<Supplier<Condition>> additionalHaving() {
		return additionalHaving;
	}

	@Override
	public Map<Attribute<?>, ConditionModel<Attribute<?>, ?>> conditions() {
		return tableConditionModel.conditions();
	}

	@Override
	public <T> ConditionModel<Attribute<?>, T> condition(Attribute<?> identifier) {
		return tableConditionModel.condition(identifier);
	}

	@Override
	public <T> ConditionModel<Attribute<?>, T> attributeCondition(Attribute<T> attribute) {
		return condition(attribute);
	}

	@Override
	public StateObserver enabled() {
		return tableConditionModel.enabled();
	}

	@Override
	public StateObserver enabled(Attribute<?> identifier) {
		return tableConditionModel.enabled(identifier);
	}

	@Override
	public Observer<?> changed() {
		return tableConditionModel.changed();
	}

	@Override
	public void clear() {
		tableConditionModel.clear();
	}

	private Collection<Condition> conditions(Predicate<ConditionModel<?, ?>> conditionModelTypePredicate, Condition additionalCondition) {
		List<Condition> conditions = tableConditionModel.conditions().values().stream()
						.filter(model -> model.enabled().get())
						.filter(conditionModelTypePredicate)
						.map(DefaultEntityConditionModel::condition)
						.collect(Collectors.toCollection(ArrayList::new));
		if (additionalCondition != null) {
			conditions.add(additionalCondition);
		}

		return conditions;
	}

	private void bindEvents() {
		tableConditionModel.conditions().values().forEach(columnConditionModel ->
						columnConditionModel.changed().addListener(conditionChangedEvent));
		additionalWhere.addListener(conditionChangedEvent);
		additionalHaving.addListener(conditionChangedEvent);
	}

	private Collection<ConditionModel<Attribute<?>, ?>> createConditionModels(EntityType entityType,
																																						ConditionModel.Factory<Attribute<?>> conditionModelFactory) {
		Collection<ConditionModel<Attribute<?>, ?>> models = new ArrayList<>();
		EntityDefinition definition = connectionProvider.entities().definition(entityType);
		definition.columns().definitions().forEach(columnDefinition ->
						conditionModelFactory.createConditionModel(columnDefinition.attribute())
										.ifPresent(models::add));
		definition.foreignKeys().definitions().forEach(foreignKeyDefinition ->
						conditionModelFactory.createConditionModel(foreignKeyDefinition.attribute())
										.ifPresent(models::add));

		return models.stream()
						.map(model -> (ConditionModel<Attribute<?>, ?>) model)
						.collect(Collectors.toList());
	}

	private static Condition condition(ConditionModel<?, ?> conditionModel) {
		if (conditionModel.identifier() instanceof ForeignKey) {
			return foreignKeyCondition((ConditionModel<?, Entity>) conditionModel);
		}

		return columnCondition(conditionModel);
	}

	private static Condition foreignKeyCondition(ConditionModel<?, Entity> conditionModel) {
		ForeignKey foreignKey = (ForeignKey) conditionModel.identifier();
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

	private static <T> ColumnCondition<T> columnCondition(ConditionModel<?, T> conditionModel) {
		Column<T> column = (Column<T>) conditionModel.identifier();
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

	private static <T> ColumnCondition<T> equalCondition(ConditionModel<?, T> conditionModel,
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

	private static <T> ColumnCondition<T> notEqualCondition(ConditionModel<?, T> conditionModel,
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

	private static <T> ColumnCondition<T> singleStringEqualCondition(ConditionModel<?, T> conditionModel,
																																	 Column<T> column, String value) {
		boolean caseSensitive = conditionModel.caseSensitive().get();
		if (containsWildcards(value)) {
			return (ColumnCondition<T>) (caseSensitive ? column.like(value) : column.likeIgnoreCase(value));
		}

		return caseSensitive ? column.equalTo((T) value) : (ColumnCondition<T>) column.equalToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> singleCharacterEqualCondition(ConditionModel<?, T> conditionModel,
																																			Column<T> column, Character value) {
		return conditionModel.caseSensitive().get() ? column.equalTo((T) value) : (ColumnCondition<T>) column.equalToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> singleStringNotEqualCondition(ConditionModel<?, T> conditionModel,
																																			Column<T> column, String value) {
		boolean caseSensitive = conditionModel.caseSensitive().get();
		if (containsWildcards(value)) {
			return (ColumnCondition<T>) (caseSensitive ? column.notLike(value) : column.notLikeIgnoreCase(value));
		}

		return caseSensitive ? column.notEqualTo((T) value) : (ColumnCondition<T>) column.notEqualToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> singleCharacterNotEqualCondition(ConditionModel<?, T> conditionModel,
																																				 Column<T> column, Character value) {
		return conditionModel.caseSensitive().get() ? column.notEqualTo((T) value) : (ColumnCondition<T>) column.notEqualToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> inCondition(ConditionModel<?, T> conditionModel, Column<T> column) {
		if (column.type().isString()) {
			Column<String> stringColumn = (Column<String>) column;
			Collection<String> inOperands = (Collection<String>) conditionModel.operands().in().get();

			return (ColumnCondition<T>) (conditionModel.caseSensitive().get() ?
							stringColumn.in(inOperands) :
							stringColumn.inIgnoreCase(inOperands));
		}

		return column.in(conditionModel.operands().in().get());
	}

	private static <T> ColumnCondition<T> notInCondition(ConditionModel<?, T> conditionModel, Column<T> column) {
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

	private final class AggregatePredicate implements Predicate<ConditionModel<?, ?>> {

		@Override
		public boolean test(ConditionModel<?, ?> conditionModel) {
			return (conditionModel.identifier() instanceof Column) &&
							entityDefinition.columns().definition((Column<?>) conditionModel.identifier()).aggregate();
		}
	}

	private final class NoneAggregatePredicate implements Predicate<ConditionModel<?, ?>> {

		@Override
		public boolean test(ConditionModel<?, ?> conditionModel) {
			return !(conditionModel.identifier() instanceof Column) ||
							!entityDefinition.columns().definition((Column<?>) conditionModel.identifier()).aggregate();
		}
	}
}
