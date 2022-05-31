/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.Operator;
import is.codion.common.Util;
import is.codion.common.event.EventListener;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.common.model.table.ColumnFilterModel;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.AttributeCondition;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Conditions;
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
import static java.util.stream.Collectors.joining;

/**
 * A default EntityTableConditionModel implementation
 */
public final class DefaultEntityTableConditionModel implements EntityTableConditionModel {

  private final State conditionChangedState = State.state();
  private final EntityType entityType;
  private final EntityConnectionProvider connectionProvider;
  private final Map<Attribute<?>, ColumnFilterModel<Entity, Attribute<?>, ?>> filterModels = new LinkedHashMap<>();
  private final Map<Attribute<?>, ColumnConditionModel<? extends Attribute<?>, ?>> conditionModels = new HashMap<>();
  private final Value<String> simpleConditionStringValue = Value.value();
  private Supplier<Condition> additionalConditionSupplier;
  private Conjunction conjunction = Conjunction.AND;
  private String rememberedCondition = "";

  /**
   * Instantiates a new DefaultEntityTableConditionModel
   * @param entityType the underlying entity type
   * @param connectionProvider a EntityConnectionProvider instance
   * @param filterModelFactory provides the column filter models for this table condition model, null if not required
   * @param conditionModelFactory provides the column condition models for this table condition model
   */
  public DefaultEntityTableConditionModel(EntityType entityType, EntityConnectionProvider connectionProvider,
                                          FilterModelFactory filterModelFactory,
                                          ConditionModelFactory conditionModelFactory) {
    this.entityType = requireNonNull(entityType, "entityType");
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    initializeConditionModels(entityType, requireNonNull(conditionModelFactory, "conditionModelFactory"));
    initializeFilterModels(entityType, filterModelFactory);
    rememberCondition();
    bindEvents();
  }

  @Override
  public EntityType getEntityType() {
    return entityType;
  }

  @Override
  public EntityDefinition getEntityDefinition() {
    return connectionProvider.getEntities().getDefinition(entityType);
  }

  @Override
  public void rememberCondition() {
    rememberedCondition = getConditionsString();
    conditionChangedState.set(false);
  }

  @Override
  public boolean hasConditionChanged() {
    return conditionChangedState.get();
  }

  @Override
  public <C extends Attribute<T>, T> ColumnFilterModel<Entity, C, T> getFilterModel(C attribute) {
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
  public Map<Attribute<?>, ColumnFilterModel<Entity, Attribute<?>, ?>> getFilterModels() {
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
  public Map<Attribute<?>, ColumnConditionModel<? extends Attribute<?>, ?>> getConditionModels() {
    return unmodifiableMap(conditionModels);
  }

  @Override
  public <C extends Attribute<T>, T> ColumnConditionModel<C, T> getConditionModel(C attribute) {
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
    String conditionsString = getConditionsString();
    ColumnConditionModel<Attribute<T>, T> conditionModel = (ColumnConditionModel<Attribute<T>, T>) conditionModels.get(attribute);
    if (conditionModel != null) {
      conditionModel.setOperator(Operator.EQUAL);
      conditionModel.setEnabled(!Util.nullOrEmpty(values));
      conditionModel.setEqualValues(null);//because the equalValue could be a reference to the active entity which changes accordingly
      conditionModel.setEqualValues(values != null && values.isEmpty() ? null : values);//this then fails to register a changed equalValue
    }
    return !conditionsString.equals(getConditionsString());
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
  public Condition getCondition() {
    Collection<Condition> conditions = new ArrayList<>();
    for (ColumnConditionModel<? extends Attribute<?>, ?> conditionModel : conditionModels.values()) {
      if (conditionModel.isEnabled()) {
        if (conditionModel instanceof ForeignKeyConditionModel) {
          conditions.add(getForeignKeyCondition((ForeignKeyConditionModel) conditionModel));
        }
        else {
          conditions.add(getAttributeCondition(conditionModel));
        }
      }
    }
    if (additionalConditionSupplier != null) {
      conditions.add(additionalConditionSupplier.get());
    }
    Condition.Combination conditionCombination = Conditions.combination(conjunction, conditions);

    return conditionCombination.getConditions().isEmpty() ? Conditions.condition(entityType) : conditionCombination;
  }

  @Override
  public Supplier<Condition> getAdditionalConditionSupplier() {
    return additionalConditionSupplier;
  }

  @Override
  public void setAdditionalConditionSupplier(Supplier<Condition> conditionSupplier) {
    this.additionalConditionSupplier = conditionSupplier;
  }

  @Override
  public Value<String> getSimpleConditionStringValue() {
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
  public StateObserver getConditionChangedObserver() {
    return conditionChangedState.getObserver();
  }

  @Override
  public void addConditionChangedListener(EventListener listener) {
    conditionChangedState.addListener(listener);
  }

  @Override
  public void removeConditionChangedListener(EventListener listener) {
    conditionChangedState.removeListener(listener);
  }

  private void bindEvents() {
    for (ColumnConditionModel<?, ?> conditionModel : conditionModels.values()) {
      conditionModel.addConditionChangedListener(() ->
              conditionChangedState.set(!rememberedCondition.equals(getConditionsString())));
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
            connectionProvider.getEntities().getDefinition(entityType).getSearchAttributes();
    conditionModels.values().stream()
            .filter(conditionModel -> searchAttributes.contains(conditionModel.getColumnIdentifier()))
            .map(conditionModel -> (ColumnConditionModel<Attribute<String>, String>) conditionModel)
            .forEach(conditionModel -> {
              conditionModel.getCaseSensitiveState().set(false);
              conditionModel.getAutomaticWildcardValue().set(AutomaticWildcard.PREFIX_AND_POSTFIX);
              conditionModel.setEqualValue(searchString);
              conditionModel.setOperator(Operator.EQUAL);
              conditionModel.setEnabled(true);
            });
  }

  /**
   * @return a String representing the current state of the condition models
   */
  private String getConditionsString() {
    return conditionModels.values().stream()
            .map(DefaultEntityTableConditionModel::toString)
            .collect(joining());
  }

  private void initializeFilterModels(EntityType entityType, FilterModelFactory filterModelProvider) {
    if (filterModelProvider != null) {
      for (Property<?> property : connectionProvider.getEntities().getDefinition(entityType).getProperties()) {
        if (!property.isHidden()) {
          ColumnFilterModel<Entity, Attribute<?>, ?> filterModel = filterModelProvider.createFilterModel(property);
          if (filterModel != null) {
            filterModels.put(filterModel.getColumnIdentifier(), filterModel);
          }
        }
      }
    }
  }

  private void initializeConditionModels(EntityType entityType, ConditionModelFactory conditionModelFactory) {
    EntityDefinition definition = connectionProvider.getEntities().getDefinition(entityType);
    for (ColumnProperty<?> columnProperty : definition.getColumnProperties()) {
      ColumnConditionModel<? extends Attribute<?>, ?> conditionModel = conditionModelFactory.createConditionModel(columnProperty.getAttribute());
      if (conditionModel != null) {
        conditionModels.put(conditionModel.getColumnIdentifier(), conditionModel);
      }
    }
    for (ForeignKeyProperty foreignKeyProperty :
            connectionProvider.getEntities().getDefinition(entityType).getForeignKeyProperties()) {
      ColumnConditionModel<ForeignKey, Entity> conditionModel = conditionModelFactory.createConditionModel(foreignKeyProperty.getAttribute());
      if (conditionModel != null) {
        conditionModels.put(conditionModel.getColumnIdentifier(), conditionModel);
      }
    }
  }

  private static Condition getForeignKeyCondition(ForeignKeyConditionModel conditionModel) {
    ForeignKey foreignKey = conditionModel.getColumnIdentifier();
    Collection<Entity> values = conditionModel.getEqualValueSet().get();
    ForeignKeyConditionBuilder builder = Conditions.where(foreignKey);
    switch (conditionModel.getOperator()) {
      case EQUAL:
        return builder.equalTo(values);
      case NOT_EQUAL:
        return builder.notEqualTo(values);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + conditionModel.getOperator() + " for foreign key conditions");
    }
  }

  private static <T> AttributeCondition<T> getAttributeCondition(ColumnConditionModel<?, T> conditionModel) {
    Attribute<T> attribute = (Attribute<T>) conditionModel.getColumnIdentifier();
    Collection<T> equalToValues = conditionModel.getEqualValues();
    boolean ignoreCase = !conditionModel.getCaseSensitiveState().get();
    AttributeCondition.Builder<T> builder = Conditions.where(attribute);
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

  private static String toString(ColumnConditionModel<?, ?> conditionModel) {
    StringBuilder stringBuilder = new StringBuilder(((Attribute<?>) conditionModel.getColumnIdentifier()).getName());
    if (conditionModel.isEnabled()) {
      stringBuilder.append(conditionModel.getOperator());
      stringBuilder.append(boundToString(conditionModel.getEqualValues()));
      stringBuilder.append(boundToString(conditionModel.getUpperBound()));
      stringBuilder.append(boundToString(conditionModel.getLowerBound()));
      stringBuilder.append(conditionModel.getCaseSensitiveState().get());
      stringBuilder.append(conditionModel.getAutomaticWildcardValue().get());
    }
    return stringBuilder.toString();
  }

  private static String boundToString(Object object) {
    StringBuilder stringBuilder = new StringBuilder();
    if (object instanceof Collection) {
      for (Object obj : (Collection<Object>) object) {
        stringBuilder.append(boundToString(obj));
      }
    }
    else if (object instanceof Entity) {
      stringBuilder.append(((Entity) object).getPrimaryKey());
    }
    else {
      stringBuilder.append(object);
    }

    return stringBuilder.toString();
  }
}
