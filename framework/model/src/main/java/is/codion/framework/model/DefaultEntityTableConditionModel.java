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
 * Copyright (c) 2016 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.event.Event;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.ConditionModel.Operands;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.observer.Observer;
import is.codion.common.state.ObservableState;
import is.codion.common.utilities.Conjunction;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.ColumnCondition;
import is.codion.framework.domain.entity.condition.Condition;

import org.jspecify.annotations.Nullable;

import java.time.temporal.Temporal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.model.condition.TableConditionModel.tableConditionModel;
import static is.codion.framework.domain.entity.condition.Condition.all;
import static is.codion.framework.domain.entity.condition.Condition.combination;
import static java.time.temporal.ChronoField.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default {@link EntityTableConditionModel} implementation
 */
final class DefaultEntityTableConditionModel implements EntityTableConditionModel {

	private static final int MAX_MILLIS = 999;
	private static final int MAX_SECONDS_MINUTES = 59;

	private final EntityDefinition entityDefinition;
	private final EntityConnectionProvider connectionProvider;
	private final TableConditionModel<Attribute<?>> tableConditionModel;
	private final Event<?> conditionChangedEvent = Event.event();
	private final NoneAggregateColumn noneAggregateColumn = new NoneAggregateColumn();
	private final AggregateColumn aggregateColumn = new AggregateColumn();

	DefaultEntityTableConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider,
																	 Supplier<Map<Attribute<?>, ConditionModel<?>>> conditionModels) {
		this.entityDefinition = connectionProvider.entities().definition(requireNonNull(entityType));
		this.connectionProvider = requireNonNull(connectionProvider);
		this.tableConditionModel = tableConditionModel(conditionModels);
		bindEvents();
	}

	@Override
	public EntityType entityType() {
		return entityDefinition.type();
	}

	@Override
	public EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	@Override
	public Condition where(Conjunction conjunction) {
		return createCondition(conjunction, noneAggregateColumn);
	}

	@Override
	public Condition having(Conjunction conjunction) {
		return createCondition(conjunction, aggregateColumn);
	}

	@Override
	public TableConditionModel<Attribute<?>> conditionModel() {
		return tableConditionModel;
	}

	@Override
	public Map<Attribute<?>, ConditionModel<?>> get() {
		return tableConditionModel.get();
	}

	@Override
	public <T> Optional<ConditionModel<T>> optional(Attribute<T> attribute) {
		return tableConditionModel.optional(requireNonNull(attribute));
	}

	@Override
	public <T> ConditionModel<T> get(Column<T> column) {
		return tableConditionModel.get(column);
	}

	@Override
	public ForeignKeyConditionModel get(ForeignKey foreignKey) {
		ConditionModel<Entity> model = tableConditionModel.get(foreignKey);

		return (ForeignKeyConditionModel) model;
	}

	@Override
	public ObservableState enabled() {
		return tableConditionModel.enabled();
	}

	@Override
	public Observer<?> changed() {
		return tableConditionModel.changed();
	}

	@Override
	public ValueSet<Attribute<?>> persist() {
		return tableConditionModel.persist();
	}

	@Override
	public void clear() {
		tableConditionModel.clear();
	}

	private Condition createCondition(Conjunction conjunction, Predicate<Attribute<?>> columnType) {
		List<Condition> conditions = tableConditionModel.get().entrySet().stream()
						.filter(entry -> columnType.test(entry.getKey()))
						.filter(entry -> entry.getValue().enabled().is())
						.map(entry -> condition(entry.getValue(), entry.getKey()))
						.collect(toList());
		switch (conditions.size()) {
			case 0:
				return all(entityDefinition.type());
			case 1:
				return conditions.get(0);
			default:
				return combination(conjunction, conditions);
		}
	}

	private void bindEvents() {
		tableConditionModel.get().values()
						.forEach(conditionModel -> conditionModel.changed().addListener(conditionChangedEvent));
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
		switch (conditionModel.operator().getOrThrow()) {
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
		switch (conditionModel.operator().getOrThrow()) {
			case EQUAL:
				return equalCondition(conditionModel, column);
			case NOT_EQUAL:
				return notEqualCondition(conditionModel, column);
			case LESS_THAN:
				return column.lessThan(operands.upper().getOrThrow());
			case LESS_THAN_OR_EQUAL:
				return column.lessThanOrEqualTo(operands.upper().getOrThrow());
			case GREATER_THAN:
				return column.greaterThan(operands.lower().getOrThrow());
			case GREATER_THAN_OR_EQUAL:
				return column.greaterThanOrEqualTo(operands.lower().getOrThrow());
			case BETWEEN_EXCLUSIVE:
				return betweenExclusiveCondition(operands.lower().get(), operands.upper().get(), column);
			case BETWEEN:
				return betweenCondition(operands.lower().get(), operands.upper().get(), column);
			case NOT_BETWEEN_EXCLUSIVE:
				return notBetweenExclusiveCondition(operands.lower().get(), operands.upper().get(), column);
			case NOT_BETWEEN:
				return notBetweenCondition(operands.lower().get(), operands.upper().get(), column);
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
			return singleStringEqualCondition(conditionModel, column, conditionModel.operands().equalWithWildcards());
		}
		if (column.type().isCharacter()) {
			return singleCharacterEqualCondition(conditionModel, column, (Character) equalOperand);
		}
		if (column.type().isTemporal()) {
			return temporalEqualCondition(conditionModel, column, (Temporal) equalOperand, false);
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
			return singleStringNotEqualCondition(conditionModel, column, conditionModel.operands().equalWithWildcards());
		}
		if (column.type().isCharacter()) {
			return singleCharacterNotEqualCondition(conditionModel, column, (Character) equalOperand);
		}
		if (column.type().isTemporal()) {
			return temporalEqualCondition(conditionModel, column, (Temporal) equalOperand, true);
		}

		return column.notEqualTo(equalOperand);
	}

	private static <T> ColumnCondition<T> singleStringEqualCondition(ConditionModel<T> conditionModel,
																																	 Column<T> column, @Nullable String value) {
		boolean caseSensitive = conditionModel.caseSensitive().is();
		if (containsWildcards(value)) {
			return (ColumnCondition<T>) (caseSensitive ? column.like(value) : column.likeIgnoreCase(value));
		}

		return caseSensitive ? column.equalTo((T) value) : (ColumnCondition<T>) column.equalToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> singleCharacterEqualCondition(ConditionModel<T> conditionModel,
																																			Column<T> column, Character value) {
		return conditionModel.caseSensitive().is() ? column.equalTo((T) value) : (ColumnCondition<T>) column.equalToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> singleStringNotEqualCondition(ConditionModel<T> conditionModel,
																																			Column<T> column, @Nullable String value) {
		boolean caseSensitive = conditionModel.caseSensitive().is();
		if (containsWildcards(value)) {
			return (ColumnCondition<T>) (caseSensitive ? column.notLike(value) : column.notLikeIgnoreCase(value));
		}

		return caseSensitive ? column.notEqualTo((T) value) : (ColumnCondition<T>) column.notEqualToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> singleCharacterNotEqualCondition(ConditionModel<T> conditionModel,
																																				 Column<T> column, Character value) {
		return conditionModel.caseSensitive().is() ? column.notEqualTo((T) value) : (ColumnCondition<T>) column.notEqualToIgnoreCase(value);
	}

	private static <T> ColumnCondition<T> temporalEqualCondition(ConditionModel<T> conditionModel, Column<T> column,
																															 Temporal value, boolean notEqual) {
		String dateTimePattern = conditionModel.dateTimePattern().orElse(null);
		if (dateTimePattern != null && containsTime(column.type())) {
			// we assume the Temporal value is the result of parsing with the given dateTimePattern
			Temporal upperBound = createTemporalUpperBound(value, dateTimePattern);
			if (upperBound != null) {
				return notEqual ? notBetweenExclusiveCondition((T) value, (T) upperBound, column) : betweenCondition((T) value, (T) upperBound, column);
			}
		}

		return notEqual ? column.notEqualTo((T) value) : column.equalTo((T) value);
	}

	private static @Nullable Temporal createTemporalUpperBound(Temporal value, String dateTimePattern) {
		if (dateTimePattern.contains("SSS")) {//millisecond
			return null;// no need, same precision as column
		}
		else if (dateTimePattern.contains("ss")) {//second
			// between second.000 and second.999
			return value.with(MILLI_OF_SECOND, MAX_MILLIS);
		}
		else if (dateTimePattern.contains("mm")) {//minute
			// between minute.00 and minute.59.999
			return value.with(SECOND_OF_MINUTE, MAX_SECONDS_MINUTES)
							.with(MILLI_OF_SECOND, MAX_MILLIS);
		}
		else if (dateTimePattern.contains("HH")) {//hour
			// between hour.00 and hour.59.59.999
			return value.with(MINUTE_OF_HOUR, MAX_SECONDS_MINUTES)
							.with(SECOND_OF_MINUTE, MAX_SECONDS_MINUTES)
							.with(MILLI_OF_SECOND, MAX_MILLIS);
		}

		return null;
	}

	private static <T> boolean containsTime(Attribute.Type<T> type) {
		return type.isLocalTime() || type.isLocalDateTime() || type.isOffsetDateTime();
	}

	private static <T> ColumnCondition<T> betweenExclusiveCondition(@Nullable T lower, @Nullable T upper, Column<T> column) {
		if (lower == null && upper != null) {
			return column.lessThan(upper);
		}
		if (upper == null && lower != null) {
			return column.greaterThan(lower);
		}

		return column.betweenExclusive(lower, upper);
	}

	private static <T> ColumnCondition<T> betweenCondition(@Nullable T lower, @Nullable T upper, Column<T> column) {
		if (lower == null && upper != null) {
			return column.lessThanOrEqualTo(upper);
		}
		if (upper == null && lower != null) {
			return column.greaterThanOrEqualTo(lower);
		}

		return column.between(lower, upper);
	}

	private static <T> ColumnCondition<T> notBetweenExclusiveCondition(@Nullable T lower, @Nullable T upper, Column<T> column) {
		if (lower == null && upper != null) {
			return column.greaterThan(upper);
		}
		if (upper == null && lower != null) {
			return column.lessThan(lower);
		}

		return column.notBetweenExclusive(lower, upper);
	}

	private static <T> ColumnCondition<T> notBetweenCondition(@Nullable T lower, @Nullable T upper, Column<T> column) {
		if (lower == null && upper != null) {
			return column.greaterThanOrEqualTo(upper);
		}
		if (upper == null && lower != null) {
			return column.lessThanOrEqualTo(lower);
		}

		return column.notBetween(lower, upper);
	}

	private static <T> ColumnCondition<T> inCondition(ConditionModel<T> conditionModel, Column<T> column) {
		Set<T> operands = conditionModel.operands().in().get();
		if (operands.isEmpty()) {
			return column.isNull();
		}
		if (column.type().isString()) {
			Column<String> stringColumn = (Column<String>) column;
			Collection<String> inOperands = (Collection<String>) operands;

			return (ColumnCondition<T>) (conditionModel.caseSensitive().is() ?
							stringColumn.in(inOperands) :
							stringColumn.inIgnoreCase(inOperands));
		}

		return column.in(operands);
	}

	private static <T> ColumnCondition<T> notInCondition(ConditionModel<T> conditionModel, Column<T> column) {
		Set<T> operands = conditionModel.operands().in().get();
		if (operands.isEmpty()) {
			return column.isNotNull();
		}
		if (column.type().isString()) {
			Column<String> stringColumn = (Column<String>) column;
			Collection<String> inOperands = (Collection<String>) operands;

			return (ColumnCondition<T>) (conditionModel.caseSensitive().is() ?
							stringColumn.notIn(inOperands) :
							stringColumn.notInIgnoreCase(inOperands));
		}

		return column.notIn(operands);
	}

	private static boolean containsWildcards(@Nullable String value) {
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
}
