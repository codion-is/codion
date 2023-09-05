/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableConditionModel;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.ColumnCondition;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.ForeignKeyCondition;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.common.model.table.TableConditionModel.tableConditionModel;
import static is.codion.framework.db.condition.Condition.*;
import static java.util.Objects.requireNonNull;

/**
 * A default EntityTableConditionModel implementation
 */
final class DefaultEntityTableConditionModel<C extends Attribute<?>> implements EntityTableConditionModel<C> {

  private static final Supplier<Condition> NULL_CONDITION_SUPPLIER = () -> null;

  private final EntityType entityType;
  private final EntityConnectionProvider connectionProvider;
  private final TableConditionModel<C> conditionModel;
  private final Event<Condition> conditionChangedEvent = Event.event();
  private final Value<Supplier<Condition>> additionalConditionSupplier = Value.value(NULL_CONDITION_SUPPLIER, NULL_CONDITION_SUPPLIER);
  private final Value<Conjunction> conjunction = Value.value(Conjunction.AND, Conjunction.AND);

  DefaultEntityTableConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                                   ColumnConditionModel.Factory<C> conditionModelFactory) {
    this.entityType = requireNonNull(entityType, "entityType");
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    this.conditionModel = tableConditionModel(createConditionModels(entityType, conditionModelFactory));
    bindEvents();
  }

  @Override
  public EntityType entityType() {
    return entityType;
  }

  @Override
  public <T> boolean setEqualConditionValues(Attribute<T> attribute, Collection<T> values) {
    Condition condition = condition();
    ColumnConditionModel<Attribute<T>, T> columnConditionModel = (ColumnConditionModel<Attribute<T>, T>) conditionModel.conditionModels().get(attribute);
    if (columnConditionModel != null) {
      columnConditionModel.operator().set(Operator.EQUAL);
      columnConditionModel.setEqualValues(null);//because the equalValue could be a reference to the active entity which changes accordingly
      columnConditionModel.setEqualValues(values != null && values.isEmpty() ? null : values);//this then fails to register a changed equalValue
      columnConditionModel.enabled().set(!nullOrEmpty(values));
    }
    return !condition.equals(condition());
  }

  @Override
  public Condition condition() {
    Collection<Condition> conditions = conditionModel.conditionModels().values().stream()
            .filter(model -> model.enabled().get())
            .map(DefaultEntityTableConditionModel::condition)
            .collect(Collectors.toCollection(ArrayList::new));
    Condition additionalCondition = additionalConditionSupplier.get().get();
    if (additionalCondition != null) {
      conditions.add(additionalCondition);
    }

    return conditions.isEmpty() ? all(entityType) : combination(conjunction.get(), conditions);
  }

  @Override
  public Value<Supplier<Condition>> additionalConditionSupplier() {
    return additionalConditionSupplier;
  }

  @Override
  public Map<C, ColumnConditionModel<C, ?>> conditionModels() {
    return conditionModel.conditionModels();
  }

  @Override
  public <T> ColumnConditionModel<? extends C, T> conditionModel(C columnIdentifier) {
    return conditionModel.conditionModel(columnIdentifier);
  }

  @Override
  public <A extends Attribute<T>, T> ColumnConditionModel<A, T> attributeModel(A columnIdentifier) {
    return (ColumnConditionModel<A, T>) conditionModel((C) columnIdentifier);
  }

  @Override
  public boolean isEnabled() {
    return conditionModel.isEnabled();
  }

  @Override
  public boolean isEnabled(C columnIdentifier) {
    return conditionModel.isEnabled(columnIdentifier);
  }

  @Override
  public void addChangeListener(Runnable listener) {
    conditionModel.addChangeListener(listener);
  }

  @Override
  public void removeChangeListener(Runnable listener) {
    conditionModel.removeChangeListener(listener);
  }

  @Override
  public void clear() {
    conditionModel.clear();
  }

  @Override
  public Value<Conjunction> conjunction() {
    return conjunction;
  }

  @Override
  public void addChangeListener(Consumer<Condition> listener) {
    conditionChangedEvent.addDataListener(listener);
  }

  @Override
  public void removeChangeListener(Consumer<Condition> listener) {
    conditionChangedEvent.removeDataListener(listener);
  }

  private void bindEvents() {
    Runnable listener = () -> conditionChangedEvent.accept(condition());
    conditionModel.conditionModels().values().forEach(columnConditionModel ->
            columnConditionModel.addChangeListener(listener));
    additionalConditionSupplier.addListener(listener);
    conjunction.addListener(listener);
  }

  private Collection<ColumnConditionModel<C, ?>> createConditionModels(EntityType entityType,
                                                                       ColumnConditionModel.Factory<C> conditionModelFactory) {
    Collection<ColumnConditionModel<? extends C, ?>> models = new ArrayList<>();
    EntityDefinition definition = connectionProvider.entities().definition(entityType);
    definition.columnDefinitions().forEach(columnDefinition ->
            conditionModelFactory.createConditionModel((C) columnDefinition.attribute()).ifPresent(models::add));
    definition.foreignKeyDefinitions().forEach(foreignKeyDefinition ->
            conditionModelFactory.createConditionModel((C) foreignKeyDefinition.attribute()).ifPresent(models::add));

    return models.stream()
            .map(model -> (ColumnConditionModel<C, ?>) model)
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
    Collection<Entity> values = conditionModel.equalValues().get();
    ForeignKeyCondition.Builder builder = foreignKey(foreignKey);
    switch (conditionModel.operator().get()) {
      case EQUAL:
        return values.isEmpty() ? builder.isNull() : builder.in(values);
      case NOT_EQUAL:
        return values.isEmpty() ? builder.isNotNull() : builder.notIn(values);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + conditionModel.operator().get() + " for foreign key condition");
    }
  }

  private static <T> ColumnCondition<T> columnCondition(ColumnConditionModel<?, T> conditionModel) {
    ColumnCondition.Builder<T> builder = column((Column<T>) conditionModel.columnIdentifier());
    switch (conditionModel.operator().get()) {
      case EQUAL:
        return equalCondition(conditionModel, builder);
      case NOT_EQUAL:
        return notEqualCondition(conditionModel, builder);
      case LESS_THAN:
        return builder.lessThan(conditionModel.getUpperBound());
      case LESS_THAN_OR_EQUAL:
        return builder.lessThanOrEqualTo(conditionModel.getUpperBound());
      case GREATER_THAN:
        return builder.greaterThan(conditionModel.getLowerBound());
      case GREATER_THAN_OR_EQUAL:
        return builder.greaterThanOrEqualTo(conditionModel.getLowerBound());
      case BETWEEN_EXCLUSIVE:
        return builder.betweenExclusive(conditionModel.getLowerBound(), conditionModel.getUpperBound());
      case BETWEEN:
        return builder.between(conditionModel.getLowerBound(), conditionModel.getUpperBound());
      case NOT_BETWEEN_EXCLUSIVE:
        return builder.notBetweenExclusive(conditionModel.getLowerBound(), conditionModel.getUpperBound());
      case NOT_BETWEEN:
        return builder.notBetween(conditionModel.getLowerBound(), conditionModel.getUpperBound());
      default:
        throw new IllegalArgumentException("Unknown operator: " + conditionModel.operator().get());
    }
  }

  private static <T> ColumnCondition<T> equalCondition(ColumnConditionModel<?, T> conditionModel,
                                                       ColumnCondition.Builder<T> builder) {
    Collection<T> equalToValues = conditionModel.getEqualValues();
    Column<T> column = (Column<T>) conditionModel.columnIdentifier();
    if (equalToValues.isEmpty()) {
      return builder.isNull();
    }
    if (column.isString() && equalToValues.size() == 1) {
      return singleStringEqualCondition(conditionModel, builder, (String) equalToValues.iterator().next());
    }

    return builder.in(equalToValues);
  }

  private static <T> ColumnCondition<T> notEqualCondition(ColumnConditionModel<?, T> conditionModel,
                                                          ColumnCondition.Builder<T> builder) {
    Collection<T> equalToValues = conditionModel.getEqualValues();
    Column<T> column = (Column<T>) conditionModel.columnIdentifier();
    if (equalToValues.isEmpty()) {
      return builder.isNotNull();
    }
    if (column.isString() && equalToValues.size() == 1) {
      return singleStringNotEqualCondition(conditionModel, builder, (String) equalToValues.iterator().next());
    }

    return builder.notIn(equalToValues);
  }

  private static <T> ColumnCondition<T> singleStringEqualCondition(ColumnConditionModel<?, T> conditionModel,
                                                                   ColumnCondition.Builder<T> builder, String value) {
    boolean caseSensitive = conditionModel.caseSensitive().get();
    if (containsWildcards(value)) {
      return (ColumnCondition<T>) (caseSensitive ? builder.like(value) : builder.likeIgnoreCase(value));
    }

    return caseSensitive ? builder.equalTo((T) value) : (ColumnCondition<T>) builder.equalToIgnoreCase(value);
  }

  private static <T> ColumnCondition<T> singleStringNotEqualCondition(ColumnConditionModel<?, T> conditionModel,
                                                                      ColumnCondition.Builder<T> builder, String value) {
    boolean caseSensitive = conditionModel.caseSensitive().get();
    if (containsWildcards(value)) {
      return (ColumnCondition<T>) (caseSensitive ? builder.notLike(value) : builder.notLikeIgnoreCase(value));
    }

    return caseSensitive ? builder.notEqualTo((T) value) : (ColumnCondition<T>) builder.notEqualToIgnoreCase(value);
  }

  private static boolean containsWildcards(String value) {
    return value != null && (value.contains("%") || value.contains("_"));
  }
}
