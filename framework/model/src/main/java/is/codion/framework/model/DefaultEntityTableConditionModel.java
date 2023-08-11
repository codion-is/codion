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
import is.codion.framework.db.criteria.AttributeCriteria;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.db.criteria.ForeignKeyCriteria;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Column;
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
import static is.codion.framework.db.criteria.Criteria.*;
import static java.util.Objects.requireNonNull;

/**
 * A default EntityTableConditionModel implementation
 */
final class DefaultEntityTableConditionModel<C extends Attribute<?>> implements EntityTableConditionModel<C> {

  private final EntityType entityType;
  private final EntityConnectionProvider connectionProvider;
  private final TableConditionModel<C> conditionModel;
  private final Event<Criteria> criteriaChangedEvent = Event.event();
  private Supplier<Criteria> additionalCriteriaSupplier;
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
    Criteria criteria = criteria();
    ColumnConditionModel<Attribute<T>, T> columnConditionModel = (ColumnConditionModel<Attribute<T>, T>) conditionModel.conditionModels().get(attribute);
    if (columnConditionModel != null) {
      columnConditionModel.setOperator(Operator.EQUAL);
      columnConditionModel.setEqualValues(null);//because the equalValue could be a reference to the active entity which changes accordingly
      columnConditionModel.setEqualValues(values != null && values.isEmpty() ? null : values);//this then fails to register a changed equalValue
      columnConditionModel.setEnabled(!nullOrEmpty(values));
    }
    return !criteria.equals(criteria());
  }

  @Override
  public Criteria criteria() {
    Collection<Criteria> criteria = conditionModel.conditionModels().values().stream()
            .filter(ColumnConditionModel::isEnabled)
            .map(DefaultEntityTableConditionModel::criteria)
            .collect(Collectors.toCollection(ArrayList::new));
    if (additionalCriteriaSupplier != null) {
      Criteria additionalCriteria = additionalCriteriaSupplier.get();
      if (additionalCriteria != null) {
        criteria.add(additionalCriteria);
      }
    }

    return criteria.isEmpty() ? all(entityType) : combination(conjunction, criteria);
  }

  @Override
  public Supplier<Criteria> getAdditionalCriteriaSupplier() {
    return additionalCriteriaSupplier;
  }

  @Override
  public void setAdditionalCriteriaSupplier(Supplier<Criteria> additionalCriteriaSupplier) {
    this.additionalCriteriaSupplier = additionalCriteriaSupplier;
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
  public void addChangeListener(Consumer<Criteria> listener) {
    criteriaChangedEvent.addDataListener(listener);
  }

  @Override
  public void removeChangeListener(Consumer<Criteria> listener) {
    criteriaChangedEvent.removeDataListener(listener);
  }

  private void bindEvents() {
    conditionModel.conditionModels().values().forEach(columnConditionModel ->
            columnConditionModel.addChangeListener(() -> criteriaChangedEvent.accept(criteria())));
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

  private static Criteria criteria(ColumnConditionModel<?, ?> conditionModel) {
    if (conditionModel.columnIdentifier() instanceof ForeignKey) {
      return foreignKeyCriteria((ColumnConditionModel<?, Entity>) conditionModel);
    }

    return attributeCriteria(conditionModel);
  }

  private static Criteria foreignKeyCriteria(ColumnConditionModel<?, Entity> conditionModel) {
    ForeignKey foreignKey = (ForeignKey) conditionModel.columnIdentifier();
    Collection<Entity> values = conditionModel.equalValues().get();
    ForeignKeyCriteria.Builder builder = foreignKey(foreignKey);
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

  private static <T> AttributeCriteria<T> attributeCriteria(ColumnConditionModel<?, T> conditionModel) {
    Column<T> attribute = (Column<T>) conditionModel.columnIdentifier();
    Collection<T> equalToValues = conditionModel.getEqualValues();
    boolean caseInsensitiveString = attribute.isString() && !conditionModel.caseSensitiveState().get();
    AttributeCriteria.Builder<T> builder = attribute(attribute);
    switch (conditionModel.getOperator()) {
      case EQUAL:
        return caseInsensitiveString ?
                (AttributeCriteria<T>) builder.inIgnoreCase((Collection<String>) equalToValues) :
                builder.in(equalToValues);
      case NOT_EQUAL:
        return caseInsensitiveString ?
                (AttributeCriteria<T>) builder.notInIgnoreCase((Collection<String>) equalToValues) :
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
