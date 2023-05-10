/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.AttributeCondition;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.ForeignKeyConditionBuilder;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * A default EntityTableConditionModel implementation
 */
final class DefaultEntityTableConditionModel implements EntityTableConditionModel {

  private final EntityType entityType;
  private final EntityConnectionProvider connectionProvider;
  private final Map<Attribute<?>, ColumnConditionModel<? extends Attribute<?>, ?>> conditionModels;
  private final Event<Condition> conditionChangedEvent = Event.event();
  private Supplier<Condition> additionalConditionSupplier;
  private Conjunction conjunction = Conjunction.AND;

  DefaultEntityTableConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                                   ColumnConditionModel.Factory<Attribute<?>> conditionModelFactory) {
    this.entityType = requireNonNull(entityType, "entityType");
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    this.conditionModels = createConditionModels(entityType, requireNonNull(conditionModelFactory, "conditionModelFactory"));
    bindEvents();
  }

  @Override
  public EntityType entityType() {
    return entityType;
  }

  @Override
  public EntityDefinition entityDefinition() {
    return connectionProvider.entities().definition(entityType);
  }

  @Override
  public void clear() {
    conditionModels.values().forEach(ColumnConditionModel::clearCondition);
  }

  @Override
  public Map<Attribute<?>, ColumnConditionModel<? extends Attribute<?>, ?>> conditionModels() {
    return conditionModels;
  }

  @Override
  public <C extends Attribute<T>, T> ColumnConditionModel<C, T> conditionModel(C attribute) {
    ColumnConditionModel<C, T> conditionModel = (ColumnConditionModel<C, T>) conditionModels.get(attribute);
    if (conditionModel == null) {
      throw new IllegalArgumentException("No condition model available for attribute: " + attribute);
    }

    return conditionModel;
  }

  @Override
  public boolean isEnabled() {
    return conditionModels.values().stream()
            .anyMatch(ColumnConditionModel::isEnabled);
  }

  @Override
  public boolean isEnabled(Attribute<?> attribute) {
    return conditionModels.containsKey(attribute) && conditionModels.get(attribute).isEnabled();
  }

  @Override
  public <T> boolean setEqualConditionValues(Attribute<T> attribute, Collection<T> values) {
    Condition condition = condition();
    ColumnConditionModel<Attribute<T>, T> conditionModel = (ColumnConditionModel<Attribute<T>, T>) conditionModels.get(attribute);
    if (conditionModel != null) {
      conditionModel.setOperator(Operator.EQUAL);
      conditionModel.setEqualValues(null);//because the equalValue could be a reference to the active entity which changes accordingly
      conditionModel.setEqualValues(values != null && values.isEmpty() ? null : values);//this then fails to register a changed equalValue
      conditionModel.setEnabled(!nullOrEmpty(values));
    }
    return !condition.equals(condition());
  }

  @Override
  public Condition condition() {
    Collection<Condition> conditions = conditionModels.values().stream()
            .filter(ColumnConditionModel::isEnabled)
            .map(DefaultEntityTableConditionModel::condition)
            .collect(Collectors.toCollection(ArrayList::new));
    if (additionalConditionSupplier != null) {
      Condition additionalCondition = additionalConditionSupplier.get();
      if (additionalCondition != null) {
        conditions.add(additionalCondition);
      }
    }

    return conditions.isEmpty() ? Condition.condition(entityType) : Condition.combination(conjunction, conditions);
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
  public Conjunction getConjunction() {
    return conjunction;
  }

  @Override
  public void setConjunction(Conjunction conjunction) {
    this.conjunction = conjunction;
  }

  @Override
  public void addChangeListener(EventDataListener<Condition> listener) {
    conditionChangedEvent.addDataListener(listener);
  }

  @Override
  public void removeChangeListener(EventDataListener<Condition> listener) {
    conditionChangedEvent.removeDataListener(listener);
  }

  private void bindEvents() {
    conditionModels.values().forEach(conditionModel ->
            conditionModel.addChangeListener(() -> conditionChangedEvent.onEvent(condition())));
  }

  private Map<Attribute<?>, ColumnConditionModel<? extends Attribute<?>, ?>> createConditionModels(EntityType entityType,
                                                                                                   ColumnConditionModel.Factory<Attribute<?>> conditionModelFactory) {
    Map<Attribute<?>, ColumnConditionModel<? extends Attribute<?>, ?>> models = new HashMap<>();
    EntityDefinition definition = connectionProvider.entities().definition(entityType);
    for (ColumnProperty<?> columnProperty : definition.columnProperties()) {
      ColumnConditionModel<? extends Attribute<?>, ?> conditionModel = conditionModelFactory.createConditionModel(columnProperty.attribute());
      if (conditionModel != null) {
        models.put(conditionModel.columnIdentifier(), conditionModel);
      }
    }
    for (ForeignKeyProperty foreignKeyProperty :
            connectionProvider.entities().definition(entityType).foreignKeyProperties()) {
      ColumnConditionModel<? extends Attribute<?>, ?> conditionModel = conditionModelFactory.createConditionModel(foreignKeyProperty.attribute());
      if (conditionModel != null) {
        models.put(conditionModel.columnIdentifier(), conditionModel);
      }
    }

    return unmodifiableMap(models);
  }

  private static Condition condition(ColumnConditionModel<?, ?> conditionModel) {
    if (conditionModel.columnIdentifier() instanceof ForeignKey) {
      return foreignKeyCondition((ColumnConditionModel<?, Entity>) conditionModel);
    }

    return attributeCondition(conditionModel);
  }

  private static Condition foreignKeyCondition(ColumnConditionModel<?, Entity> conditionModel) {
    ForeignKey foreignKey = (ForeignKey) conditionModel.columnIdentifier();
    Collection<Entity> values = conditionModel.equalValueSet().get();
    ForeignKeyConditionBuilder builder = Condition.where(foreignKey);
    switch (conditionModel.getOperator()) {
      case EQUAL:
        return builder.equalTo(values);
      case NOT_EQUAL:
        return builder.notEqualTo(values);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + conditionModel.getOperator() + " for foreign key conditions");
    }
  }

  private static <T> AttributeCondition<T> attributeCondition(ColumnConditionModel<?, T> conditionModel) {
    Attribute<T> attribute = (Attribute<T>) conditionModel.columnIdentifier();
    Collection<T> equalToValues = conditionModel.getEqualValues();
    boolean caseInsensitiveString = attribute.isString() && !conditionModel.caseSensitiveState().get();
    AttributeCondition.Builder<T> builder = Condition.where(attribute);
    switch (conditionModel.getOperator()) {
      case EQUAL:
        return caseInsensitiveString ? (AttributeCondition<T>) builder.equalToIgnoreCase((Collection<String>) equalToValues) : builder.equalTo(equalToValues);
      case NOT_EQUAL:
        return caseInsensitiveString ? (AttributeCondition<T>) builder.notEqualToIgnoreCase((Collection<String>) equalToValues) : builder.notEqualTo(equalToValues);
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
