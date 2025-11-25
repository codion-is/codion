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

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.ConditionModel.Operands;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.ValueSet;
import is.codion.common.utilities.Conjunction;
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
import java.util.Objects;
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

final class DefaultEntityConditionModel implements EntityConditionModel {

	private static final int MAX_MILLIS = 999;
	private static final int MAX_SECONDS_MINUTES = 59;

	private static final Supplier<@Nullable Condition> NULL_CONDITION_SUPPLIER = () -> null;

	private final EntityDefinition entityDefinition;
	private final EntityConnectionProvider connectionProvider;
	private final TableConditionModel<Attribute<?>> conditionModel;
	private final Value<Conjunction> conjunction = Value.builder()
					.nonNull(Conjunction.AND)
					.build();
	private final Event<?> changed = Event.event();
	private final DefaultAdditional additional = new DefaultAdditional();
	private final NoneAggregateColumn noneAggregateColumn = new NoneAggregateColumn();
	private final AggregateColumn aggregateColumn = new AggregateColumn();
	private final DefaultModified modified;

	DefaultEntityConditionModel(DefaultBuilder builder) {
		this.entityDefinition = builder.connectionProvider.entities().definition(builder.entityType);
		this.connectionProvider = builder.connectionProvider;
		this.conditionModel = tableConditionModel(builder.conditionFactory);
		this.modified = new DefaultModified();
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
	public Condition where() {
		return createCondition(noneAggregateColumn, additional.where);
	}

	@Override
	public Condition having() {
		return createCondition(aggregateColumn, additional.having);
	}

	@Override
	public Value<Conjunction> conjunction() {
		return conjunction;
	}

	@Override
	public Map<Attribute<?>, ConditionModel<?>> get() {
		return conditionModel.get();
	}

	@Override
	public <T> ConditionModel<T> get(Attribute<?> attribute) {
		return conditionModel.get(attribute);
	}

	@Override
	public <T> Optional<ConditionModel<T>> optional(Attribute<?> attribute) {
		return conditionModel.optional(requireNonNull(attribute));
	}

	@Override
	public ForeignKeyConditionModel get(ForeignKey foreignKey) {
		return (ForeignKeyConditionModel) conditionModel.<Entity>get(foreignKey);
	}

	@Override
	public ObservableState enabled() {
		return conditionModel.enabled();
	}

	@Override
	public Observer<?> changed() {
		return changed.observer();
	}

	@Override
	public ValueSet<Attribute<?>> persist() {
		return conditionModel.persist();
	}

	@Override
	public void clear() {
		conditionModel.clear();
	}

	@Override
	public AdditionalConditions additional() {
		return additional;
	}

	@Override
	public Modified modified() {
		return modified;
	}

	private Condition createCondition(Predicate<Attribute<?>> columnType, ConditionValue additionalCondition) {
		List<Condition> conditions = conditionModel.get().entrySet().stream()
						.filter(entry -> columnType.test(entry.getKey()))
						.filter(entry -> entry.getValue().enabled().is())
						.map(entry -> condition(entry.getValue(), entry.getKey()))
						.collect(toList());
		Condition tableCondition;
		switch (conditions.size()) {
			case 0:
				tableCondition = all(entityDefinition.type());
				break;
			case 1:
				tableCondition = conditions.get(0);
				break;
			default:
				tableCondition = combination(conjunction.getOrThrow(), conditions);
				break;
		}

		return additionalCondition.optional()
						.map(Supplier::get)
						.filter(Objects::nonNull)
						.map(condition -> combination(additionalCondition.conjunction().getOrThrow(), tableCondition, condition))
						.map(Condition.class::cast)
						.orElse(tableCondition);
	}

	private void bindEvents() {
		conditionModel.changed().addListener(changed);
		additional.where.addListener(changed);
		additional.where.conjunction().addListener(changed);
		additional.having.addListener(changed);
		additional.having.conjunction().addListener(changed);
		conjunction.addListener(changed);
		changed.addListener(modified::set);
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

	static final class DefaultBuilder implements Builder {

		static final EntityTypeStep ENTITY_TYPE_STEP = new DefaultEntityTypeStep();

		private final EntityType entityType;
		private final EntityConnectionProvider connectionProvider;

		private Supplier<Map<Attribute<?>, ConditionModel<?>>> conditionFactory;

		private DefaultBuilder(EntityType entityType, EntityConnectionProvider connectionProvider) {
			this.entityType = entityType;
			this.connectionProvider = connectionProvider;
			this.conditionFactory = new EntityConditions(entityType, connectionProvider);
		}

		private static final class DefaultEntityTypeStep implements EntityTypeStep {

			@Override
			public ConnectionProviderStep entityType(EntityType entityType) {
				return new DefaultConnectionProviderStep(requireNonNull(entityType));
			}
		}

		private static final class DefaultConnectionProviderStep implements ConnectionProviderStep {

			private final EntityType entityType;

			private DefaultConnectionProviderStep(EntityType entityType) {
				this.entityType = entityType;
			}

			@Override
			public Builder connectionProvider(EntityConnectionProvider connectionProvider) {
				return new DefaultBuilder(entityType, requireNonNull(connectionProvider));
			}
		}

		@Override
		public Builder conditions(Supplier<Map<Attribute<?>, ConditionModel<?>>> conditions) {
			this.conditionFactory = requireNonNull(conditions);
			return this;
		}

		@Override
		public EntityConditionModel build() {
			return new DefaultEntityConditionModel(this);
		}
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

	private final class DefaultModified implements Modified {

		private final State modified = State.state();

		private ConditionState conditionState;

		private DefaultModified() {
			this.conditionState = new ConditionState();
		}

		@Override
		public boolean is() {
			return modified.is();
		}

		@Override
		public ObservableState not() {
			return modified.not();
		}

		@Override
		public Observer<Boolean> observer() {
			return modified.observer();
		}

		@Override
		public void reset() {
			conditionState = new ConditionState();
			modified.set(false);
		}

		private void set() {
			modified.set(!Objects.equals(conditionState, new ConditionState()));
		}
	}

	private final class ConditionState {

		private final Condition where;
		private final Condition having;

		private ConditionState() {
			this.where = where();
			this.having = having();
		}

		@Override
		public boolean equals(Object object) {
			if (object == null || getClass() != object.getClass()) {
				return false;
			}
			ConditionState that = (ConditionState) object;

			return Objects.equals(where, that.where) && Objects.equals(having, that.having);
		}

		@Override
		public int hashCode() {
			return Objects.hash(where, having);
		}
	}

	private static final class DefaultAdditional implements AdditionalConditions {

		private final ConditionValue where = new DefaultAdditionalCondition();
		private final ConditionValue having = new DefaultAdditionalCondition();

		@Override
		public ConditionValue where() {
			return where;
		}

		@Override
		public ConditionValue having() {
			return having;
		}
	}

	private static final class DefaultAdditionalCondition extends AbstractValue<Supplier<Condition>> implements ConditionValue {

		private Supplier<Condition> condition = NULL_CONDITION_SUPPLIER;

		private final Value<Conjunction> conjunction = Value.nonNull(Conjunction.AND);

		private DefaultAdditionalCondition() {
			super(NULL_CONDITION_SUPPLIER, Notify.SET);
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
