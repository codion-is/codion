/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.Util;
import is.codion.common.db.Operator;
import is.codion.common.event.EventListener;
import is.codion.common.model.Refreshable;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.state.States;
import is.codion.common.value.Value;
import is.codion.common.value.Values;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A default EntityTableConditionModel implementation
 */
public final class DefaultEntityTableConditionModel implements EntityTableConditionModel {

  private final State conditionChangedState = States.state();
  private final EntityType<?> entityType;
  private final EntityConnectionProvider connectionProvider;
  private final Map<Attribute<?>, ColumnConditionModel<Entity, Property<?>, ?>> filterModels = new LinkedHashMap<>();
  private final Map<Attribute<?>, ColumnConditionModel<Entity, ? extends Property<?>, ?>> conditionModels = new HashMap<>();
  private final Value<String> simpleConditionStringValue = Values.value();
  private Condition.Provider additionalConditionProvider;
  private Conjunction conjunction = Conjunction.AND;
  private String rememberedCondition = "";

  /**
   * Instantiates a new DefaultEntityTableConditionModel
   * @param entityType the id of the underlying entity
   * @param connectionProvider a EntityConnectionProvider instance
   * @param filterModelFactory provides the column filter models for this table condition model, null if not required
   * @param conditionModelFactory provides the column condition models for this table condition model
   */
  public DefaultEntityTableConditionModel(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider,
                                          final FilterModelFactory filterModelFactory,
                                          final ConditionModelFactory conditionModelFactory) {
    this.entityType = requireNonNull(entityType, "entityType");
    this.connectionProvider = requireNonNull(connectionProvider, "connectionProvider");
    initializePropertyConditionModels(entityType, requireNonNull(conditionModelFactory, "conditionModelFactory"));
    initializeFilterModels(entityType, filterModelFactory);
    initializeForeignKeyConditionModels(entityType, connectionProvider, conditionModelFactory);
    rememberCondition();
    bindEvents();
  }

  @Override
  public EntityType<?> getEntityType() {
    return entityType;
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
  public <C extends Property<T>, T> ColumnConditionModel<Entity, C, T> getFilterModel(final Attribute<T> attribute) {
    if (filterModels.containsKey(attribute)) {
      return (ColumnConditionModel<Entity, C, T>) filterModels.get(attribute);
    }

    throw new IllegalArgumentException("No property filter model found for attribute " + attribute);
  }

  @Override
  public Collection<ColumnConditionModel<Entity, Property<?>, ?>> getFilterModels() {
    return unmodifiableCollection(filterModels.values());
  }

  @Override
  public void refresh() {
    for (final ColumnConditionModel<?, ?, ?> model : conditionModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).refresh();
      }
    }
  }

  @Override
  public void clear() {
    for (final ColumnConditionModel<?, ?, ?> model : conditionModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).clear();
      }
    }
  }

  @Override
  public void clearConditionModels() {
    for (final ColumnConditionModel<?, ?, ?> conditionModel : conditionModels.values()) {
      conditionModel.clearCondition();
    }
  }

  @Override
  public Collection<ColumnConditionModel<Entity, ? extends Property<?>, ?>> getConditionModels() {
    return unmodifiableCollection(conditionModels.values());
  }

  @Override
  public boolean containsConditionModel(final Attribute<?> attribute) {
    return conditionModels.containsKey(attribute);
  }

  @Override
  public <T> ColumnConditionModel<Entity, ? extends Property<T>, T> getConditionModel(final Attribute<T> attribute) {
    if (conditionModels.containsKey(attribute)) {
      return (ColumnConditionModel<Entity, ? extends Property<T>, T>) conditionModels.get(attribute);
    }

    throw new IllegalArgumentException("Condition model not found for property: " + attribute);
  }

  @Override
  public boolean isEnabled() {
    return conditionModels.values().stream().anyMatch(ColumnConditionModel::isEnabled);
  }

  @Override
  public boolean isConditionEnabled(final Attribute<?> attribute) {
    return containsConditionModel(attribute) && getConditionModel(attribute).isEnabled();
  }

  @Override
  public boolean isFilterEnabled(final Attribute<?> attribute) {
    final ColumnConditionModel<Entity, ?, ?> propertyFilterModel = getFilterModel(attribute);

    return propertyFilterModel != null && propertyFilterModel.isEnabled();
  }

  @Override
  public <T> boolean setEqualConditionValues(final Attribute<T> attribute, final Collection<T> values) {
    final String conditionsString = getConditionsString();
    if (containsConditionModel(attribute)) {
      final ColumnConditionModel<?, ?, T> conditionModel = getConditionModel(attribute);
      conditionModel.setOperator(Operator.EQUAL);
      conditionModel.setEnabled(!Util.nullOrEmpty(values));
      conditionModel.setEqualValues(null);//because the upperBound could be a reference to the active entity which changes accordingly
      conditionModel.setEqualValues(values != null && values.isEmpty() ? null : values);//this then fails to register a changed upper bound
    }
    return !conditionsString.equals(getConditionsString());
  }

  @Override
  public <T> void setEqualFilterValue(final Attribute<T> attribute, final Comparable<T> value) {
    final ColumnConditionModel<Entity, Property<T>, T> filterModel = getFilterModel(attribute);
    if (filterModel != null) {
      filterModel.setEqualValue((T) value);
    }
  }

  @Override
  public Condition getCondition() {
    final Condition.Combination conditionCombination = Conditions.combination(conjunction);
    for (final ColumnConditionModel<Entity, ? extends Property<?>, ?> conditionModel : conditionModels.values()) {
      if (conditionModel.isEnabled()) {
        if (conditionModel instanceof ForeignKeyConditionModel) {
          conditionCombination.add(getForeignKeyCondition((ForeignKeyConditionModel) conditionModel));
        }
        else {
          conditionCombination.add(getCondition(conditionModel));
        }
      }
    }
    if (additionalConditionProvider != null) {
      conditionCombination.add(additionalConditionProvider.getCondition());
    }

    return conditionCombination.getConditions().isEmpty() ? Conditions.condition(entityType) : conditionCombination;
  }

  @Override
  public Condition.Provider getAdditionalConditionProvider() {
    return additionalConditionProvider;
  }

  @Override
  public void setAdditionalConditionProvider(final Condition.Provider conditionProvider) {
    this.additionalConditionProvider = conditionProvider;
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
  public void setConjunction(final Conjunction conjunction) {
    this.conjunction = conjunction;
  }

  @Override
  public void enable(final Attribute<?> attribute) {
    if (containsConditionModel(attribute)) {
      final ColumnConditionModel<?, ?, ?> conditionModel = getConditionModel(attribute);
      if (!conditionModel.isLocked()) {
        conditionModel.setEnabled(true);
      }
    }
  }

  @Override
  public void disable(final Attribute<?> attribute) {
    if (containsConditionModel(attribute)) {
      final ColumnConditionModel<?, ?, ?> conditionModel = getConditionModel(attribute);
      if (!conditionModel.isLocked()) {
        conditionModel.setEnabled(false);
      }
    }
  }

  @Override
  public StateObserver getConditionObserver() {
    return conditionChangedState.getObserver();
  }

  @Override
  public void addConditionListener(final EventListener listener) {
    conditionChangedState.addListener(listener);
  }

  @Override
  public void removeConditionListener(final EventListener listener) {
    conditionChangedState.removeListener(listener);
  }

  private void bindEvents() {
    for (final ColumnConditionModel<?, ?, ?> conditionModel : conditionModels.values()) {
      conditionModel.addConditionChangedListener(() ->
              conditionChangedState.set(!rememberedCondition.equals(getConditionsString())));
    }
    simpleConditionStringValue.addDataListener(conditionString -> {
      clearConditionModels();
      if (!Util.nullOrEmpty(conditionString)) {
        setConditionString(conditionString);
      }
    });
  }

  private void setConditionString(final String searchString) {
    final Collection<Attribute<String>> searchAttributes =
            connectionProvider.getEntities().getDefinition(entityType).getSearchAttributes();
    for (final Attribute<String> searchAttribute : searchAttributes) {
      final ColumnConditionModel<?, ?, String> conditionModel = getConditionModel(searchAttribute);
      conditionModel.setCaseSensitive(false);
      conditionModel.setAutomaticWildcard(ColumnConditionModel.AutomaticWildcard.PREFIX_AND_POSTFIX);
      conditionModel.setEqualValue(searchString);
      conditionModel.setOperator(Operator.EQUAL);
      conditionModel.setEnabled(true);
    }
  }

  /**
   * @return a String representing the current state of the condition models
   */
  private String getConditionsString() {
    return conditionModels.values().stream().map(DefaultEntityTableConditionModel::toString).collect(joining());
  }

  private void initializeFilterModels(final EntityType<?> entityType, final FilterModelFactory filterModelProvider) {
    if (filterModelProvider != null) {
      for (final Property<?> property : connectionProvider.getEntities().getDefinition(entityType).getProperties()) {
        if (!property.isHidden()) {
          final ColumnConditionModel<Entity, Property<?>, ?> filterModel = filterModelProvider.createFilterModel(property);
          filterModels.put(filterModel.getColumnIdentifier().getAttribute(), filterModel);
        }
      }
    }
  }

  private void initializePropertyConditionModels(final EntityType<?> entityType, final ConditionModelFactory conditionModelFactory) {
    final EntityDefinition definition = connectionProvider.getEntities().getDefinition(entityType);
    for (final ColumnProperty<?> columnProperty : definition.getColumnProperties()) {
      if (!columnProperty.isAggregateColumn()) {
        final ColumnConditionModel<Entity, ColumnProperty<?>, ?> conditionModel =
                conditionModelFactory.createColumnConditionModel(columnProperty);
        if (conditionModel != null) {
          conditionModels.put(conditionModel.getColumnIdentifier().getAttribute(), conditionModel);
        }
      }
    }
  }

  private void initializeForeignKeyConditionModels(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider,
                                                   final ConditionModelFactory conditionModelProvider) {
    for (final ForeignKeyProperty foreignKeyProperty :
            connectionProvider.getEntities().getDefinition(entityType).getForeignKeyProperties()) {
      final ColumnConditionModel<Entity, ForeignKeyProperty, Entity> conditionModel =
              conditionModelProvider.createForeignKeyConditionModel(foreignKeyProperty, connectionProvider);
      if (conditionModel != null) {
        conditionModels.put(conditionModel.getColumnIdentifier().getAttribute(), conditionModel);
      }
    }
  }

  private static Condition getForeignKeyCondition(final ForeignKeyConditionModel conditionModel) {
    final ForeignKey foreignKey = conditionModel.getColumnIdentifier().getAttribute();
    final Collection<Entity> values = conditionModel.getEqualValueSet().get();
    final ForeignKeyConditionBuilder builder = Conditions.condition(foreignKey);
    switch (conditionModel.getOperator()) {
      case EQUAL:
        return values.isEmpty() ? builder.isNull() : builder.equalTo(values);
      case NOT_EQUAL:
        return values.isEmpty() ? builder.isNotNull() : builder.notEqualTo(values);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + conditionModel.getOperator() + " for foreign key conditions");
    }
  }

  private static <T> AttributeCondition<T> getCondition(final ColumnConditionModel<Entity, ?, T> conditionModel) {
    final Collection<T> equalToValues = conditionModel.getEqualValues();
    final AttributeCondition.Builder<T> builder = Conditions.condition(((Property<T>) conditionModel.getColumnIdentifier()).getAttribute());
    switch (conditionModel.getOperator()) {
      case EQUAL:
        final AttributeCondition<T> equalCondition = equalToValues.isEmpty() ? builder.isNull() : builder.equalTo(equalToValues);
        if (equalCondition.getAttribute().isString()) {
          equalCondition.setCaseSensitive(conditionModel.isCaseSensitive());
        }
        return equalCondition;
      case NOT_EQUAL:
        final AttributeCondition<T> notEqualCondition = equalToValues.isEmpty() ? builder.isNotNull() : builder.notEqualTo(equalToValues);
        if (notEqualCondition.getAttribute().isString()) {
          notEqualCondition.setCaseSensitive(conditionModel.isCaseSensitive());
        }
        return notEqualCondition;
      case LESS_THAN:
        return builder.lessThan(conditionModel.getUpperBound());
      case LESS_THAN_OR_EQUAL:
        return builder.lessThanOrEqualTo(conditionModel.getUpperBound());
      case GREATER_THAN:
        return builder.greaterThan(conditionModel.getUpperBound());
      case GREATER_THAN_OR_EQUAL:
        return builder.greaterThanOrEqualTo(conditionModel.getUpperBound());
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

  private static String toString(final ColumnConditionModel<?, ?, ?> conditionModel) {
    final StringBuilder stringBuilder = new StringBuilder(((Property<?>) conditionModel.getColumnIdentifier()).getAttribute().getName());
    if (conditionModel.isEnabled()) {
      stringBuilder.append(conditionModel.getOperator());
      stringBuilder.append(boundToString(conditionModel.getEqualValues()));
      stringBuilder.append(boundToString(conditionModel.getUpperBound()));
      stringBuilder.append(boundToString(conditionModel.getLowerBound()));
    }
    return stringBuilder.toString();
  }

  private static String boundToString(final Object object) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (object instanceof Collection) {
      for (final Object obj : (Collection<Object>) object) {
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
