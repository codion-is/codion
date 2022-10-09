/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.Operator;
import is.codion.common.Util;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.common.model.table.ColumnFilterModel;
import is.codion.common.value.Value;
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
import is.codion.framework.domain.property.Property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

/**
 * A default EntityTableConditionModel implementation
 */
final class DefaultEntityTableConditionModel implements EntityTableConditionModel {

  private final EntityType entityType;
  private final EntityConnectionProvider connectionProvider;
  private final Map<Attribute<?>, ColumnFilterModel<Entity, Attribute<?>, ?>> filterModels = new LinkedHashMap<>();
  private final Map<Attribute<?>, ColumnConditionModel<? extends Attribute<?>, ?>> conditionModels = new HashMap<>();
  private final Value<String> simpleConditionStringValue = Value.value();
  private final Event<Condition> conditionChangedEvent = Event.event();
  private Supplier<Condition> additionalConditionSupplier;
  private Conjunction conjunction = Conjunction.AND;

  DefaultEntityTableConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                                   FilterModelFactory filterModelFactory,
                                   ConditionModelFactory conditionModelFactory) {
    this.entityType = requireNonNull(entityType, "entityType");
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    createConditionModels(entityType, requireNonNull(conditionModelFactory, "conditionModelFactory"));
    createFilterModels(entityType, filterModelFactory);
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
  public <C extends Attribute<T>, T> ColumnFilterModel<Entity, C, T> filterModel(C attribute) {
    ColumnFilterModel<Entity, C, T> filterModel = (ColumnFilterModel<Entity, C, T>) filterModels.get(attribute);
    if (filterModel == null) {
      throw new IllegalArgumentException("No filter model available for attribute: " + attribute);
    }

    return filterModel;
  }

  @Override
  public void clearFilters() {
    filterModels.values().forEach(ColumnConditionModel::clearCondition);
  }

  @Override
  public Map<Attribute<?>, ColumnFilterModel<Entity, Attribute<?>, ?>> filterModels() {
    return unmodifiableMap(filterModels);
  }

  @Override
  public void refresh() {
    for (ColumnConditionModel<?, ?> model : conditionModels.values()) {
      if (model instanceof ForeignKeyConditionModel) {
        ((ForeignKeyConditionModel) model).refresh();
      }
    }
  }

  @Override
  public void clearConditions() {
    conditionModels.values().forEach(ColumnConditionModel::clearCondition);
  }

  @Override
  public Map<Attribute<?>, ColumnConditionModel<? extends Attribute<?>, ?>> conditionModels() {
    return unmodifiableMap(conditionModels);
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
  public boolean isConditionEnabled() {
    return conditionModels.values().stream()
            .anyMatch(ColumnConditionModel::isEnabled);
  }

  @Override
  public boolean isConditionEnabled(Attribute<?> attribute) {
    return conditionModels.containsKey(attribute) && conditionModels.get(attribute).isEnabled();
  }

  @Override
  public boolean isFilterEnabled() {
    return filterModels.values().stream()
            .anyMatch(ColumnConditionModel::isEnabled);
  }

  @Override
  public boolean isFilterEnabled(Attribute<?> attribute) {
    return filterModels.containsKey(attribute) && filterModels.get(attribute).isEnabled();
  }

  @Override
  public <T> boolean setEqualConditionValues(Attribute<T> attribute, Collection<T> values) {
    Condition condition = condition();
    ColumnConditionModel<Attribute<T>, T> conditionModel = (ColumnConditionModel<Attribute<T>, T>) conditionModels.get(attribute);
    if (conditionModel != null) {
      conditionModel.setOperator(Operator.EQUAL);
      conditionModel.setEqualValues(null);//because the equalValue could be a reference to the active entity which changes accordingly
      conditionModel.setEqualValues(values != null && values.isEmpty() ? null : values);//this then fails to register a changed equalValue
      conditionModel.setEnabled(!Util.nullOrEmpty(values));
    }
    return !condition.equals(condition());
  }

  @Override
  public <T> void setEqualFilterValue(Attribute<T> attribute, Comparable<T> value) {
    ColumnFilterModel<Entity, Attribute<?>, T> filterModel = (ColumnFilterModel<Entity, Attribute<?>, T>) filterModels.get(attribute);
    if (filterModel != null) {
      filterModel.setOperator(Operator.EQUAL);
      filterModel.setEqualValue((T) value);
      filterModel.setEnabled(value != null);
    }
  }

  @Override
  public Condition condition() {
    Collection<Condition> conditions = new ArrayList<>();
    for (ColumnConditionModel<? extends Attribute<?>, ?> conditionModel : conditionModels.values()) {
      if (conditionModel.isEnabled()) {
        if (conditionModel instanceof ForeignKeyConditionModel) {
          conditions.add(foreignKeyCondition((ForeignKeyConditionModel) conditionModel));
        }
        else {
          conditions.add(attributeCondition(conditionModel));
        }
      }
    }
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
  public Value<String> simpleConditionStringValue() {
    return simpleConditionStringValue;
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
  public void addConditionChangedListener(EventDataListener<Condition> listener) {
    conditionChangedEvent.addDataListener(listener);
  }

  @Override
  public void removeConditionChangedListener(EventDataListener<Condition> listener) {
    conditionChangedEvent.removeDataListener(listener);
  }

  private void bindEvents() {
    for (ColumnConditionModel<?, ?> conditionModel : conditionModels.values()) {
      conditionModel.addConditionChangedListener(() -> conditionChangedEvent.onEvent(condition()));
    }
    simpleConditionStringValue.addDataListener(conditionString -> {
      clearConditions();
      if (!Util.nullOrEmpty(conditionString)) {
        setConditionString(conditionString);
      }
    });
  }

  private void setConditionString(String searchString) {
    Collection<Attribute<String>> searchAttributes =
            connectionProvider.entities().definition(entityType).searchAttributes();
    conditionModels.values().stream()
            .filter(conditionModel -> searchAttributes.contains(conditionModel.columnIdentifier()))
            .map(conditionModel -> (ColumnConditionModel<Attribute<String>, String>) conditionModel)
            .forEach(conditionModel -> {
              conditionModel.caseSensitiveState().set(false);
              conditionModel.automaticWildcardValue().set(AutomaticWildcard.PREFIX_AND_POSTFIX);
              conditionModel.setEqualValue(searchString);
              conditionModel.setOperator(Operator.EQUAL);
              conditionModel.setEnabled(true);
            });
  }

  private void createFilterModels(EntityType entityType, FilterModelFactory filterModelProvider) {
    if (filterModelProvider != null) {
      for (Property<?> property : connectionProvider.entities().definition(entityType).properties()) {
        if (!property.isHidden()) {
          ColumnFilterModel<Entity, Attribute<?>, ?> filterModel = filterModelProvider.createFilterModel(property);
          if (filterModel != null) {
            filterModels.put(filterModel.columnIdentifier(), filterModel);
          }
        }
      }
    }
  }

  private void createConditionModels(EntityType entityType, ConditionModelFactory conditionModelFactory) {
    EntityDefinition definition = connectionProvider.entities().definition(entityType);
    for (ColumnProperty<?> columnProperty : definition.columnProperties()) {
      ColumnConditionModel<? extends Attribute<?>, ?> conditionModel = conditionModelFactory.createConditionModel(columnProperty.attribute());
      if (conditionModel != null) {
        conditionModels.put(conditionModel.columnIdentifier(), conditionModel);
      }
    }
    for (ForeignKeyProperty foreignKeyProperty :
            connectionProvider.entities().definition(entityType).foreignKeyProperties()) {
      ColumnConditionModel<ForeignKey, Entity> conditionModel = conditionModelFactory.createConditionModel(foreignKeyProperty.attribute());
      if (conditionModel != null) {
        conditionModels.put(conditionModel.columnIdentifier(), conditionModel);
      }
    }
  }

  private static Condition foreignKeyCondition(ForeignKeyConditionModel conditionModel) {
    ForeignKey foreignKey = conditionModel.columnIdentifier();
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
    boolean ignoreCase = !conditionModel.caseSensitiveState().get();
    AttributeCondition.Builder<T> builder = Condition.where(attribute);
    switch (conditionModel.getOperator()) {
      case EQUAL:
        if (attribute.isString() && ignoreCase) {
          return (AttributeCondition<T>) builder.equalToIgnoreCase((Collection<String>) equalToValues);
        }

        return builder.equalTo(equalToValues);
      case NOT_EQUAL:
        if (attribute.isString() && ignoreCase) {
          return (AttributeCondition<T>) builder.notEqualToIgnoreCase((Collection<String>) equalToValues);
        }

        return builder.notEqualTo(equalToValues);
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
