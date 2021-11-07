/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.Operator;
import is.codion.common.Util;
import is.codion.common.event.EventListener;
import is.codion.common.model.table.ColumnConditionModel;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Collections.unmodifiableCollection;
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
   * @param entityType the id of the underlying entity
   * @param connectionProvider a EntityConnectionProvider instance
   * @param filterModelFactory provides the column filter models for this table condition model, null if not required
   * @param conditionModelFactory provides the column condition models for this table condition model
   */
  public DefaultEntityTableConditionModel(final EntityType entityType, final EntityConnectionProvider connectionProvider,
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
  public <C extends Attribute<T>, T> ColumnFilterModel<Entity, C, T> getFilterModel(final Attribute<T> attribute) {
    return (ColumnFilterModel<Entity, C, T>) getFilter(attribute).orElseThrow(() ->
            new IllegalArgumentException("No filter model available for attribute: " + attribute));
  }

  @Override
  public <C extends Attribute<T>, T> Optional<ColumnFilterModel<Entity, C, T>> getFilter(final Attribute<T> attribute) {
    return Optional.ofNullable((ColumnFilterModel<Entity, C, T>) filterModels.get(attribute));
  }

  @Override
  public void clearFilters() {
    filterModels.values().forEach(ColumnConditionModel::clearCondition);
  }

  @Override
  public Collection<ColumnFilterModel<Entity, Attribute<?>, ?>> getFilterModels() {
    return unmodifiableCollection(filterModels.values());
  }

  @Override
  public void refresh() {
    for (final ColumnConditionModel<?, ?> model : conditionModels.values()) {
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
  public Collection<ColumnConditionModel<? extends Attribute<?>, ?>> getConditionModels() {
    return unmodifiableCollection(conditionModels.values());
  }

  @Override
  public <T> ColumnConditionModel<? extends Attribute<T>, T> getConditionModel(final Attribute<T> attribute) {
    return getCondition(attribute).orElseThrow(() ->
            new IllegalArgumentException("No condition model available for attribute: " + attribute));
  }

  @Override
  public <T> Optional<ColumnConditionModel<? extends Attribute<T>, T>> getCondition(final Attribute<T> attribute) {
    return Optional.ofNullable((ColumnConditionModel<? extends Attribute<T>, T>) conditionModels.get(attribute));
  }

  @Override
  public boolean isConditionEnabled() {
    return conditionModels.values().stream().anyMatch(ColumnConditionModel::isEnabled);
  }

  @Override
  public boolean isConditionEnabled(final Attribute<?> attribute) {
    return getCondition(attribute).map(ColumnConditionModel::isEnabled).orElse(false);
  }

  @Override
  public boolean isFilterEnabled() {
    return filterModels.values().stream().anyMatch(ColumnConditionModel::isEnabled);
  }

  @Override
  public boolean isFilterEnabled(final Attribute<?> attribute) {
    return getFilter(attribute).map(ColumnConditionModel::isEnabled).orElse(false);
  }

  @Override
  public <T> boolean setEqualConditionValues(final Attribute<T> attribute, final Collection<T> values) {
    final String conditionsString = getConditionsString();
    getCondition(attribute).ifPresent(conditionModel -> {
      conditionModel.setOperator(Operator.EQUAL);
      conditionModel.setEnabled(!Util.nullOrEmpty(values));
      conditionModel.setEqualValues(null);//because the equalValue could be a reference to the active entity which changes accordingly
      conditionModel.setEqualValues(values != null && values.isEmpty() ? null : values);//this then fails to register a changed equalValue
    });
    return !conditionsString.equals(getConditionsString());
  }

  @Override
  public <T> void setEqualFilterValue(final Attribute<T> attribute, final Comparable<T> value) {
    getFilter(attribute).ifPresent(filterModel -> {
      filterModel.setOperator(Operator.EQUAL);
      filterModel.setEqualValue((T) value);
      filterModel.setEnabled(value != null);
    });
  }

  @Override
  public Condition getCondition() {
    final Condition.Combination conditionCombination = Conditions.combination(conjunction);
    for (final ColumnConditionModel<? extends Attribute<?>, ?> conditionModel : conditionModels.values()) {
      if (conditionModel.isEnabled()) {
        if (conditionModel instanceof ForeignKeyConditionModel) {
          conditionCombination.add(getForeignKeyCondition((ForeignKeyConditionModel) conditionModel));
        }
        else {
          conditionCombination.add(getCondition(conditionModel));
        }
      }
    }
    if (additionalConditionSupplier != null) {
      conditionCombination.add(additionalConditionSupplier.get());
    }

    return conditionCombination.getConditions().isEmpty() ? Conditions.condition(entityType) : conditionCombination;
  }

  @Override
  public Supplier<Condition> getAdditionalConditionSupplier() {
    return additionalConditionSupplier;
  }

  @Override
  public void setAdditionalConditionSupplier(final Supplier<Condition> conditionSupplier) {
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
  public void setConjunction(final Conjunction conjunction) {
    this.conjunction = conjunction;
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
    for (final ColumnConditionModel<?, ?> conditionModel : conditionModels.values()) {
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

  private void setConditionString(final String searchString) {
    final Collection<Attribute<String>> searchAttributes =
            connectionProvider.getEntities().getDefinition(entityType).getSearchAttributes();
    for (final Attribute<String> searchAttribute : searchAttributes) {
      getCondition(searchAttribute).ifPresent(conditionModel -> {
        conditionModel.setCaseSensitive(false);
        conditionModel.setAutomaticWildcard(ColumnConditionModel.AutomaticWildcard.PREFIX_AND_POSTFIX);
        conditionModel.setEqualValue(searchString);
        conditionModel.setOperator(Operator.EQUAL);
        conditionModel.setEnabled(true);
      });
    }
  }

  /**
   * @return a String representing the current state of the condition models
   */
  private String getConditionsString() {
    return conditionModels.values().stream().map(DefaultEntityTableConditionModel::toString).collect(joining());
  }

  private void initializeFilterModels(final EntityType entityType, final FilterModelFactory filterModelProvider) {
    if (filterModelProvider != null) {
      for (final Property<?> property : connectionProvider.getEntities().getDefinition(entityType).getProperties()) {
        if (!property.isHidden()) {
          filterModelProvider.createFilterModel(property)
                  .ifPresent(filterModel -> filterModels.put(filterModel.getColumnIdentifier(), filterModel));
        }
      }
    }
  }

  private void initializePropertyConditionModels(final EntityType entityType, final ConditionModelFactory conditionModelFactory) {
    final EntityDefinition definition = connectionProvider.getEntities().getDefinition(entityType);
    for (final ColumnProperty<?> columnProperty : definition.getColumnProperties()) {
      if (!columnProperty.isAggregateColumn()) {
        conditionModelFactory.createColumnConditionModel(columnProperty)
                .ifPresent(conditionModel -> conditionModels.put(conditionModel.getColumnIdentifier(), conditionModel));
      }
    }
  }

  private void initializeForeignKeyConditionModels(final EntityType entityType, final EntityConnectionProvider connectionProvider,
                                                   final ConditionModelFactory conditionModelProvider) {
    for (final ForeignKeyProperty foreignKeyProperty :
            connectionProvider.getEntities().getDefinition(entityType).getForeignKeyProperties()) {
      conditionModelProvider.createForeignKeyConditionModel(foreignKeyProperty.getAttribute())
              .ifPresent(conditionModel -> conditionModels.put(conditionModel.getColumnIdentifier(), conditionModel));
    }
  }

  private static Condition getForeignKeyCondition(final ForeignKeyConditionModel conditionModel) {
    final ForeignKey foreignKey = conditionModel.getColumnIdentifier();
    final Collection<Entity> values = conditionModel.getEqualValueSet().get();
    final ForeignKeyConditionBuilder builder = Conditions.where(foreignKey);
    switch (conditionModel.getOperator()) {
      case EQUAL:
        return builder.equalTo(values);
      case NOT_EQUAL:
        return builder.notEqualTo(values);
      default:
        throw new IllegalArgumentException("Unsupported operator: " + conditionModel.getOperator() + " for foreign key conditions");
    }
  }

  private static <T> AttributeCondition<T> getCondition(final ColumnConditionModel<?, T> conditionModel) {
    final Collection<T> equalToValues = conditionModel.getEqualValues();
    final AttributeCondition.Builder<T> builder = Conditions.where((Attribute<T>) conditionModel.getColumnIdentifier());
    switch (conditionModel.getOperator()) {
      case EQUAL:
        final AttributeCondition<T> equalCondition = builder.equalTo(equalToValues);
        if (equalCondition.getAttribute().isString()) {
          equalCondition.caseSensitive(conditionModel.isCaseSensitive());
        }
        return equalCondition;
      case NOT_EQUAL:
        final AttributeCondition<T> notEqualCondition = builder.notEqualTo(equalToValues);
        if (notEqualCondition.getAttribute().isString()) {
          notEqualCondition.caseSensitive(conditionModel.isCaseSensitive());
        }
        return notEqualCondition;
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

  private static String toString(final ColumnConditionModel<?, ?> conditionModel) {
    final StringBuilder stringBuilder = new StringBuilder(((Attribute<?>) conditionModel.getColumnIdentifier()).getName());
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
