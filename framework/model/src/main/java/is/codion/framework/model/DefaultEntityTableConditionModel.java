/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.TableConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.AttributeCondition;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.ForeignKeyConditionBuilder;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

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

  private final EntityType entityType;
  private final EntityConnectionProvider connectionProvider;
  private final TableConditionModel<C> conditionModel;
  private final Event<Condition> conditionChangedEvent = Event.event();
  private Supplier<Condition> additionalConditionSupplier;
  private Conjunction conjunction = Conjunction.AND;

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
      columnConditionModel.setOperator(Operator.EQUAL);
      columnConditionModel.setEqualValues(null);//because the equalValue could be a reference to the active entity which changes accordingly
      columnConditionModel.setEqualValues(values != null && values.isEmpty() ? null : values);//this then fails to register a changed equalValue
      columnConditionModel.setEnabled(!nullOrEmpty(values));
    }
    return !condition.equals(condition());
  }

  @Override
  public Condition condition() {
    Collection<Condition> conditions = conditionModel.conditionModels().values().stream()
            .filter(ColumnConditionModel::isEnabled)
            .map(DefaultEntityTableConditionModel::condition)
            .collect(Collectors.toCollection(ArrayList::new));
    if (additionalConditionSupplier != null) {
      Condition additionalCondition = additionalConditionSupplier.get();
      if (additionalCondition != null) {
        conditions.add(additionalCondition);
      }
    }

    return conditions.isEmpty() ? all(entityType) : combination(conjunction, conditions);
  }

  @Override
  public Supplier<Condition> getAdditionalConditionSupplier() {
    return additionalConditionSupplier;
  }

  @Override
  public void setAdditionalConditionSupplier(Supplier<Condition> additionalConditionSupplier) {
    this.additionalConditionSupplier = additionalConditionSupplier;
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
  public Conjunction getConjunction() {
    return conjunction;
  }

  @Override
  public void setConjunction(Conjunction conjunction) {
    this.conjunction = conjunction;
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
    conditionModel.conditionModels().values().forEach(columnConditionModel ->
            columnConditionModel.addChangeListener(() -> conditionChangedEvent.accept(condition())));
  }

  private Collection<ColumnConditionModel<C, ?>> createConditionModels(EntityType entityType,
                                                                       ColumnConditionModel.Factory<C> conditionModelFactory) {
    Collection<ColumnConditionModel<? extends C, ?>> models = new ArrayList<>();
    EntityDefinition definition = connectionProvider.entities().definition(entityType);
    definition.columnProperties().forEach(columnProperty ->
            conditionModelFactory.createConditionModel((C) columnProperty.attribute()).ifPresent(models::add));
    definition.foreignKeyProperties().forEach(foreignKeyProperty ->
            conditionModelFactory.createConditionModel((C) foreignKeyProperty.attribute()).ifPresent(models::add));

    return models.stream()
            .map(model -> (ColumnConditionModel<C, ?>) model)
            .collect(Collectors.toList());
  }

  private static Condition condition(ColumnConditionModel<?, ?> conditionModel) {
    if (conditionModel.columnIdentifier() instanceof ForeignKey) {
      return foreignKeyCondition((ColumnConditionModel<?, Entity>) conditionModel);
    }

    return attributeCondition(conditionModel);
  }

  private static Condition foreignKeyCondition(ColumnConditionModel<?, Entity> conditionModel) {
    ForeignKey foreignKey = (ForeignKey) conditionModel.columnIdentifier();
    Collection<Entity> values = conditionModel.equalValues().get();
    ForeignKeyConditionBuilder builder = foreignKey(foreignKey);
    switch (conditionModel.getOperator()) {
      case EQUAL:
        if (values.isEmpty()) {
          return builder.isNull();
        }

        return builder.in(values);
      case NOT_EQUAL:
        if (values.isEmpty()) {
          return builder.isNotNull();
        }

        return builder.notIn(values);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + conditionModel.getOperator() + " for foreign key conditions");
    }
  }

  private static <T> AttributeCondition<T> attributeCondition(ColumnConditionModel<?, T> conditionModel) {
    Attribute<T> attribute = (Attribute<T>) conditionModel.columnIdentifier();
    Collection<T> equalToValues = conditionModel.getEqualValues();
    boolean caseInsensitiveString = attribute.isString() && !conditionModel.caseSensitiveState().get();
    AttributeCondition.Builder<T> builder = attribute(attribute);
    switch (conditionModel.getOperator()) {
      case EQUAL:
        return caseInsensitiveString ?
                (AttributeCondition<T>) builder.inIgnoreCase((Collection<String>) equalToValues) :
                builder.in(equalToValues);
      case NOT_EQUAL:
        return caseInsensitiveString ?
                (AttributeCondition<T>) builder.notInIgnoreCase((Collection<String>) equalToValues) :
                builder.notIn(equalToValues);
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
        throw new IllegalArgumentException("Unknown operator: " + conditionModel.getOperator());
    }
  }
}
