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
import is.codion.common.event.EventObserver;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableConditionModel;
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

import static is.codion.common.model.table.TableConditionModel.tableConditionModel;
import static is.codion.framework.domain.entity.condition.Condition.all;
import static is.codion.framework.domain.entity.condition.Condition.combination;
import static java.util.Objects.requireNonNull;

/**
 * A default EntityTableConditionModel implementation
 */
final class DefaultEntityTableConditionModel implements EntityTableConditionModel {

	private static final Supplier<Condition> NULL_CONDITION_SUPPLIER = () -> null;

	private final EntityDefinition entityDefinition;
	private final EntityConnectionProvider connectionProvider;
	private final TableConditionModel<Attribute<?>> conditionModel;
	private final Event<?> conditionChangedEvent = Event.event();
	private final Value<Supplier<Condition>> additionalWhere = Value.nonNull(NULL_CONDITION_SUPPLIER).build();
	private final Value<Supplier<Condition>> additionalHaving = Value.nonNull(NULL_CONDITION_SUPPLIER).build();
	private final NoneAggregatePredicate noneAggregatePredicate = new NoneAggregatePredicate();
	private final AggregatePredicate aggregatePredicate = new AggregatePredicate();

	DefaultEntityTableConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider,
																	 ColumnConditionModel.Factory<Attribute<?>> conditionModelFactory) {
		this.entityDefinition = connectionProvider.entities().definition(requireNonNull(entityType, "entityType"));
		this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
		this.conditionModel = tableConditionModel(createConditionModels(entityType, conditionModelFactory));
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
	public <T> boolean setInConditionValues(Attribute<T> attribute, Collection<T> values) {
		requireNonNull(attribute);
		requireNonNull(values);
		boolean aggregateColumn = attribute instanceof Column && entityDefinition.columns().definition((Column<?>) attribute).aggregate();
		Condition condition = aggregateColumn ? having(Conjunction.AND) : where(Conjunction.AND);
		ColumnConditionModel<Attribute<?>, T> columnConditionModel =
						(ColumnConditionModel<Attribute<?>, T>) conditionModel.conditionModels().get(attribute);
		if (columnConditionModel != null) {
			columnConditionModel.operator().set(Operator.IN);
			columnConditionModel.setInValues(values);
			columnConditionModel.enabled().set(!values.isEmpty());
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
	public Map<Attribute<?>, ColumnConditionModel<Attribute<?>, ?>> conditionModels() {
		return conditionModel.conditionModels();
	}

	@Override
	public <T> ColumnConditionModel<Attribute<?>, T> conditionModel(Attribute<?> columnIdentifier) {
		return conditionModel.conditionModel(columnIdentifier);
	}

	@Override
	public <T> ColumnConditionModel<Attribute<?>, T> attributeModel(Attribute<T> columnIdentifier) {
		return conditionModel(columnIdentifier);
	}

	@Override
	public boolean enabled() {
		return conditionModel.enabled();
	}

	@Override
	public boolean enabled(Attribute<?> columnIdentifier) {
		return conditionModel.enabled(columnIdentifier);
	}

	@Override
	public EventObserver<?> conditionChangedEvent() {
		return conditionModel.conditionChangedEvent();
	}

	@Override
	public void clear() {
		conditionModel.clear();
	}

	private Collection<Condition> conditions(Predicate<ColumnConditionModel<?, ?>> conditionModelTypePredicate, Condition additionalCondition) {
		List<Condition> conditions = conditionModel.conditionModels().values().stream()
						.filter(model -> model.enabled().get())
						.filter(conditionModelTypePredicate)
						.map(DefaultEntityTableConditionModel::condition)
						.collect(Collectors.toCollection(ArrayList::new));
		if (additionalCondition != null) {
			conditions.add(additionalCondition);
		}

		return conditions;
	}

	private void bindEvents() {
		conditionModel.conditionModels().values().forEach(columnConditionModel ->
						columnConditionModel.conditionChangedEvent().addListener(conditionChangedEvent));
		additionalWhere.addListener(conditionChangedEvent);
		additionalHaving.addListener(conditionChangedEvent);
	}

	private Collection<ColumnConditionModel<Attribute<?>, ?>> createConditionModels(EntityType entityType,
																																									ColumnConditionModel.Factory<Attribute<?>> conditionModelFactory) {
		Collection<ColumnConditionModel<Attribute<?>, ?>> models = new ArrayList<>();
		EntityDefinition definition = connectionProvider.entities().definition(entityType);
		definition.columns().definitions().forEach(columnDefinition ->
						conditionModelFactory.createConditionModel(columnDefinition.attribute())
										.ifPresent(model -> models.add((ColumnConditionModel<Attribute<?>, ?>) model)));
		definition.foreignKeys().definitions().forEach(foreignKeyDefinition ->
						conditionModelFactory.createConditionModel(foreignKeyDefinition.attribute())
										.ifPresent(model -> models.add((ColumnConditionModel<Attribute<?>, ?>) model)));

		return models.stream()
						.map(model -> (ColumnConditionModel<Attribute<?>, ?>) model)
						.collect(Collectors.toList());
	}

	private static Condition condition(ColumnConditionModel<?, ?> conditionModel) {
		if (conditionModel.columnIdentifier() instanceof ForeignKey) {
			return foreignKeyCondition((ColumnConditionModel<?, Entity>) conditionModel);
		}

		return columnCondition(conditionModel);
	}

	private static Condition foreignKeyCondition(ColumnConditionModel<?, Entity> conditionModel) {
		ForeignKey foreignKey = (ForeignKey) conditionModel.columnIdentifier();
		Entity equalValue = conditionModel.equalValue().get();
		Collection<Entity> inValues = conditionModel.inValues().get();
		switch (conditionModel.operator().get()) {
			case EQUAL:
				return equalValue == null ? foreignKey.isNull() : foreignKey.equalTo(equalValue);
			case IN:
				return inValues.isEmpty() ? foreignKey.isNull() : foreignKey.in(inValues);
			case NOT_EQUAL:
				return equalValue == null ? foreignKey.isNotNull() : foreignKey.notEqualTo(equalValue);
			case NOT_IN:
				return inValues.isEmpty() ? foreignKey.isNotNull() : foreignKey.notIn(inValues);
			default:
				throw new IllegalArgumentException("Unsupported operator: " + conditionModel.operator().get() + " for foreign key condition");
		}
	}

	private static <T> ColumnCondition<T> columnCondition(ColumnConditionModel<?, T> conditionModel) {
		Column<T> column = (Column<T>) conditionModel.columnIdentifier();
		switch (conditionModel.operator().get()) {
			case EQUAL:
				return equalCondition(conditionModel, column);
			case NOT_EQUAL:
				return notEqualCondition(conditionModel, column);
			case LESS_THAN:
				return column.lessThan(conditionModel.getUpperBound());
			case LESS_THAN_OR_EQUAL:
				return column.lessThanOrEqualTo(conditionModel.getUpperBound());
			case GREATER_THAN:
				return column.greaterThan(conditionModel.getLowerBound());
			case GREATER_THAN_OR_EQUAL:
				return column.greaterThanOrEqualTo(conditionModel.getLowerBound());
			case BETWEEN_EXCLUSIVE:
				return column.betweenExclusive(conditionModel.getLowerBound(), conditionModel.getUpperBound());
			case BETWEEN:
				return column.between(conditionModel.getLowerBound(), conditionModel.getUpperBound());
			case NOT_BETWEEN_EXCLUSIVE:
				return column.notBetweenExclusive(conditionModel.getLowerBound(), conditionModel.getUpperBound());
			case NOT_BETWEEN:
				return column.notBetween(conditionModel.getLowerBound(), conditionModel.getUpperBound());
			case IN:
				return inCondition(conditionModel, column);
			case NOT_IN:
				return notInCondition(conditionModel, column);
			default:
				throw new IllegalArgumentException("Unknown operator: " + conditionModel.operator().get());
		}
	}

	private static <T> ColumnCondition<T> equalCondition(ColumnConditionModel<?, T> conditionModel,
																											 Column<T> column) {
		T equalValue = conditionModel.getEqualValue();
		if (equalValue == null) {
			return column.isNull();
		}
		if (column.type().isString()) {
			return singleStringEqualCondition(conditionModel, column, (String) equalValue);
		}
		if (column.type().isCharacter()) {
			return singleCharacterEqualCondition(conditionModel, column, (Character) equalValue);
		}

		return column.equalTo(equalValue);
	}

	private static <T> ColumnCondition<T> notEqualCondition(ColumnConditionModel<?, T> conditionModel,
																													Column<T> column) {
		T equalValue = conditionModel.getEqualValue();
		if (equalValue == null) {
			return column.isNotNull();
		}
		if (column.type().isString()) {
			return singleStringNotEqualCondition(conditionModel, column, (String) equalValue);
		}
		if (column.type().isCharacter()) {
			return singleCharacterNotEqualCondition(conditionModel, column, (Character) equalValue);
		}

		return column.notEqualTo(equalValue);
	}

	private static <T> ColumnCondition<T> singleStringEqualCondition(ColumnConditionModel<?, T> conditionModel,
																																	 Column<T> column, String value) {
		boolean caseSensitive = conditionModel.caseSensitive().get();
		if (containsWildcards(value)) {
			return (ColumnCondition<T>) (caseSensitive ? column.like(value) : column.likeIgnoreCase(value));
		}

		return caseSensitive ? column.equalTo((T) value) : (ColumnCondition<T>) column.equalToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> singleCharacterEqualCondition(ColumnConditionModel<?, T> conditionModel,
																																			Column<T> column, Character value) {
		return conditionModel.caseSensitive().get() ? column.equalTo((T) value) : (ColumnCondition<T>) column.equalToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> singleStringNotEqualCondition(ColumnConditionModel<?, T> conditionModel,
																																			Column<T> column, String value) {
		boolean caseSensitive = conditionModel.caseSensitive().get();
		if (containsWildcards(value)) {
			return (ColumnCondition<T>) (caseSensitive ? column.notLike(value) : column.notLikeIgnoreCase(value));
		}

		return caseSensitive ? column.notEqualTo((T) value) : (ColumnCondition<T>) column.notEqualToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> singleCharacterNotEqualCondition(ColumnConditionModel<?, T> conditionModel,
																																				 Column<T> column, Character value) {
		return conditionModel.caseSensitive().get() ? column.notEqualTo((T) value) : (ColumnCondition<T>) column.notEqualToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> inCondition(ColumnConditionModel<?, T> conditionModel, Column<T> column) {
		if (column.type().isString()) {
			Column<String> stringColumn = (Column<String>) column;
			Collection<String> inValues = (Collection<String>) conditionModel.getInValues();

			return (ColumnCondition<T>) (conditionModel.caseSensitive().get() ?
							stringColumn.in(inValues) :
							stringColumn.inIgnoreCase(inValues));
		}

		return column.in(conditionModel.getInValues());
	}

	private static <T> ColumnCondition<T> notInCondition(ColumnConditionModel<?, T> conditionModel, Column<T> column) {
		if (column.type().isString()) {
			Column<String> stringColumn = (Column<String>) column;
			Collection<String> inValues = (Collection<String>) conditionModel.getInValues();

			return (ColumnCondition<T>) (conditionModel.caseSensitive().get() ?
							stringColumn.notIn(inValues) :
							stringColumn.notInIgnoreCase(inValues));
		}

		return column.notIn(conditionModel.getInValues());
	}

	private static boolean containsWildcards(String value) {
		return value != null && (value.contains("%") || value.contains("_"));
	}

	private final class AggregatePredicate implements Predicate<ColumnConditionModel<?, ?>> {

		@Override
		public boolean test(ColumnConditionModel<?, ?> conditionModel) {
			return (conditionModel.columnIdentifier() instanceof Column) &&
							entityDefinition.columns().definition((Column<?>) conditionModel.columnIdentifier()).aggregate();
		}
	}

	private final class NoneAggregatePredicate implements Predicate<ColumnConditionModel<?, ?>> {

		@Override
		public boolean test(ColumnConditionModel<?, ?> conditionModel) {
			return !(conditionModel.columnIdentifier() instanceof Column) ||
							!entityDefinition.columns().definition((Column<?>) conditionModel.columnIdentifier()).aggregate();
		}
	}
}
