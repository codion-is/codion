/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.model;

import dev.codion.common.Conjunction;
import dev.codion.common.Util;
import dev.codion.common.db.Operator;
import dev.codion.common.event.Event;
import dev.codion.common.event.EventListener;
import dev.codion.common.event.EventObserver;
import dev.codion.common.event.Events;
import dev.codion.common.model.Refreshable;
import dev.codion.common.model.table.ColumnConditionModel;
import dev.codion.common.state.State;
import dev.codion.common.state.StateObserver;
import dev.codion.common.state.States;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.condition.Condition;
import dev.codion.framework.db.condition.Conditions;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.property.ColumnProperty;
import dev.codion.framework.domain.property.ForeignKeyProperty;
import dev.codion.framework.domain.property.Property;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static dev.codion.framework.db.condition.Conditions.propertyCondition;

/**
 * A default EntityTableConditionModel implementation
 */
public final class DefaultEntityTableConditionModel implements EntityTableConditionModel {

  private final State conditionChangedState = States.state();
  private final Event<String> simpleConditionStringChangedEvent = Events.event();
  private final Event simpleSearchPerformedEvent = Events.event();

  private final String entityId;
  private final EntityConnectionProvider connectionProvider;
  private final Map<String, ColumnConditionModel<Entity, Property>> propertyFilterModels = new LinkedHashMap<>();
  private final Map<String, ColumnConditionModel<Entity, ? extends Property>> propertyConditionModels = new HashMap<>();
  private Condition.Provider additionalConditionProvider;
  private Conjunction conjunction = Conjunction.AND;
  private String rememberedCondition = "";
  private String simpleConditionString = "";

  /**
   * Instantiates a new DefaultEntityTableConditionModel
   * @param entityId the id of the underlying entity
   * @param connectionProvider a EntityConnectionProvider instance, required if the searchable properties include
   * foreign key properties
   * @param filterModelProvider provides the column filter models for this table condition model
   * @param conditionModelProvider provides the column condition models for this table condition model
   */
  public DefaultEntityTableConditionModel(final String entityId, final EntityConnectionProvider connectionProvider,
                                          final PropertyFilterModelProvider filterModelProvider,
                                          final PropertyConditionModelProvider conditionModelProvider) {
    requireNonNull(entityId, "entityId");
    requireNonNull(connectionProvider, "connectionProvider");
    this.entityId = entityId;
    this.connectionProvider = connectionProvider;
    initializeFilterModels(entityId, filterModelProvider);
    initializeColumnPropertyConditionModels(entityId, conditionModelProvider);
    initializeForeignKeyPropertyConditionModels(entityId, connectionProvider, conditionModelProvider);
    rememberCondition();
    bindEvents();
  }

  @Override
  public String getEntityId() {
    return entityId;
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
  public ColumnConditionModel<Entity, Property> getPropertyFilterModel(final String propertyId) {
    if (propertyFilterModels.containsKey(propertyId)) {
      return propertyFilterModels.get(propertyId);
    }

    throw new IllegalArgumentException("No property filter model found for property " + propertyId);
  }

  @Override
  public Collection<ColumnConditionModel<Entity, Property>> getPropertyFilterModels() {
    return unmodifiableCollection(propertyFilterModels.values());
  }

  @Override
  public void refresh() {
    for (final ColumnConditionModel model : propertyConditionModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).refresh();
      }
    }
  }

  @Override
  public void clear() {
    for (final ColumnConditionModel model : propertyConditionModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).clear();
      }
    }
  }

  @Override
  public void clearPropertyConditionModels() {
    for (final ColumnConditionModel conditionModel : propertyConditionModels.values()) {
      conditionModel.clearCondition();
    }
  }

  @Override
  public Collection<ColumnConditionModel<Entity, ? extends Property>> getPropertyConditionModels() {
    return unmodifiableCollection(propertyConditionModels.values());
  }

  @Override
  public boolean containsPropertyConditionModel(final String propertyId) {
    return propertyConditionModels.containsKey(propertyId);
  }

  @Override
  public ColumnConditionModel<Entity, ? extends Property> getPropertyConditionModel(final String propertyId) {
    if (propertyConditionModels.containsKey(propertyId)) {
      return propertyConditionModels.get(propertyId);
    }

    throw new IllegalArgumentException("Condition model not found for property: " + propertyId);
  }

  @Override
  public boolean isEnabled() {
    return propertyConditionModels.values().stream().anyMatch(ColumnConditionModel::isEnabled);
  }

  @Override
  public boolean isEnabled(final String propertyId) {
    return containsPropertyConditionModel(propertyId) && getPropertyConditionModel(propertyId).isEnabled();
  }

  @Override
  public boolean isFilterEnabled(final String propertyId) {
    final ColumnConditionModel<Entity, Property> propertyFilterModel = getPropertyFilterModel(propertyId);

    return propertyFilterModel != null && propertyFilterModel.isEnabled();
  }

  @Override
  public boolean setConditionValues(final String propertyId, final Collection values) {
    final String conditionsString = getConditionsString();
    if (containsPropertyConditionModel(propertyId)) {
      final ColumnConditionModel conditionModel = getPropertyConditionModel(propertyId);
      conditionModel.setEnabled(!Util.nullOrEmpty(values));
      conditionModel.setUpperBound(null);//because the upperBound could be a reference to the active entity which changes accordingly
      conditionModel.setUpperBound(values != null && values.isEmpty() ? null : values);//this then fails to register a changed upper bound
    }
    return !conditionsString.equals(getConditionsString());
  }

  @Override
  public void setFilterValue(final String propertyId, final Comparable value) {
    final ColumnConditionModel<Entity, Property> filterModel = getPropertyFilterModel(propertyId);
    if (filterModel != null) {
      filterModel.setLikeValue(value);
    }
  }

  @Override
  public Condition getCondition() {
    final Condition.Combination conditionCombination = Conditions.combination(conjunction);
    for (final ColumnConditionModel<Entity, ? extends Property> conditionModel : propertyConditionModels.values()) {
      if (conditionModel.isEnabled()) {
        conditionCombination.add(getCondition(conditionModel));
      }
    }
    if (additionalConditionProvider != null) {
      conditionCombination.add(additionalConditionProvider.getCondition());
    }

    return conditionCombination.getConditions().isEmpty() ? null : conditionCombination;
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
  public String getSimpleConditionString() {
    return simpleConditionString;
  }

  @Override
  public void setSimpleConditionString(final String simpleConditionString) {
    this.simpleConditionString = simpleConditionString == null ? "" : simpleConditionString;
    clearPropertyConditionModels();
    if (this.simpleConditionString.length() != 0) {
      setConditionString(this.simpleConditionString);
    }
    simpleConditionStringChangedEvent.onEvent(this.simpleConditionString);
  }

  @Override
  public void performSimpleSearch() {
    final Conjunction previousConjunction = getConjunction();
    try {
      setConjunction(Conjunction.OR);
      simpleSearchPerformedEvent.onEvent();
    }
    finally {
      setConjunction(previousConjunction);
    }
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
  public void enable(final String propertyId) {
    if (containsPropertyConditionModel(propertyId)) {
      getPropertyConditionModel(propertyId).setEnabled(true);
    }
  }

  @Override
  public void disable(final String propertyId) {
    if (containsPropertyConditionModel(propertyId)) {
      getPropertyConditionModel(propertyId).setEnabled(false);
    }
  }

  @Override
  public EventObserver<String> getSimpleConditionStringObserver() {
    return simpleConditionStringChangedEvent.getObserver();
  }

  @Override
  public StateObserver getConditionChangedObserver() {
    return conditionChangedState.getObserver();
  }

  @Override
  public void addConditionChangedListener(final EventListener listener) {
    conditionChangedState.addListener(listener);
  }

  @Override
  public void removeConditionChangedListener(final EventListener listener) {
    conditionChangedState.removeListener(listener);
  }

  @Override
  public void addSimpleConditionListener(final EventListener listener) {
    simpleSearchPerformedEvent.addListener(listener);
  }

  @Override
  public void removeSimpleConditionListener(final EventListener listener) {
    simpleSearchPerformedEvent.removeListener(listener);
  }

  private void bindEvents() {
    for (final ColumnConditionModel conditionModel : propertyConditionModels.values()) {
      conditionModel.addConditionChangedListener(() ->
              conditionChangedState.set(!rememberedCondition.equals(getConditionsString())));
    }
  }

  private void setConditionString(final String searchString) {
    final Collection<ColumnProperty> searchProperties =
            connectionProvider.getEntities().getDefinition(entityId).getSearchProperties();
    for (final ColumnProperty searchProperty : searchProperties) {
      final ColumnConditionModel conditionModel = getPropertyConditionModel(searchProperty.getPropertyId());
      conditionModel.setCaseSensitive(false);
      conditionModel.setAutomaticWildcard(ColumnConditionModel.AutomaticWildcard.PREFIX_AND_POSTFIX);
      conditionModel.setUpperBound(searchString);
      conditionModel.setOperator(Operator.LIKE);
      conditionModel.setEnabled(true);
    }
  }

  /**
   * @return a String representing the current state of the condition models
   */
  private String getConditionsString() {
    return propertyConditionModels.values().stream().map(DefaultEntityTableConditionModel::toString).collect(joining());
  }

  private void initializeFilterModels(final String entityId, final PropertyFilterModelProvider filterModelProvider) {
    if (filterModelProvider != null) {
      for (final Property property : connectionProvider.getEntities().getDefinition(entityId).getProperties()) {
        if (!property.isHidden()) {
          final ColumnConditionModel<Entity, Property> filterModel = filterModelProvider.initializePropertyFilterModel(property);
          this.propertyFilterModels.put(filterModel.getColumnIdentifier().getPropertyId(), filterModel);
        }
      }
    }
  }

  private void initializeColumnPropertyConditionModels(final String entityId, final PropertyConditionModelProvider conditionModelProvider) {
    for (final ColumnProperty columnProperty :
            connectionProvider.getEntities().getDefinition(entityId).getColumnProperties()) {
      if (!columnProperty.isForeignKeyProperty() && !columnProperty.isAggregateColumn()) {
        final ColumnConditionModel<Entity, ColumnProperty> conditionModel =
                conditionModelProvider.initializePropertyConditionModel(columnProperty);
        if (conditionModel != null) {
          this.propertyConditionModels.put(conditionModel.getColumnIdentifier().getPropertyId(), conditionModel);
        }
      }
    }
  }

  private void initializeForeignKeyPropertyConditionModels(final String entityId, final EntityConnectionProvider connectionProvider,
                                                           final PropertyConditionModelProvider conditionModelProvider) {
    for (final ForeignKeyProperty foreignKeyProperty :
            connectionProvider.getEntities().getDefinition(entityId).getForeignKeyProperties()) {
      final ColumnConditionModel<Entity, ForeignKeyProperty> conditionModel =
              conditionModelProvider.initializeForeignKeyConditionModel(foreignKeyProperty, connectionProvider);
      if (conditionModel != null) {
        this.propertyConditionModels.put(conditionModel.getColumnIdentifier().getPropertyId(), conditionModel);
      }
    }
  }

  private static Condition getCondition(final ColumnConditionModel<Entity, ? extends Property> conditionModel) {
    final Object conditionValue = conditionModel.getOperator().getValues().equals(Operator.Values.TWO) ?
            asList(conditionModel.getLowerBound(), conditionModel.getUpperBound()) : conditionModel.getUpperBound();

    return propertyCondition(conditionModel.getColumnIdentifier().getPropertyId(), conditionModel.getOperator(), conditionValue)
            .setCaseSensitive(conditionModel.isCaseSensitive());
  }

  private static String toString(final ColumnConditionModel<Entity, ? extends Property> conditionModel) {
    final StringBuilder stringBuilder = new StringBuilder(conditionModel.getColumnIdentifier().getPropertyId());
    if (conditionModel.isEnabled()) {
      stringBuilder.append(conditionModel.getOperator());
      stringBuilder.append(boundToString(conditionModel.getUpperBound()));
      stringBuilder.append(boundToString(conditionModel.getLowerBound()));
    }

    return stringBuilder.toString();
  }

  private static String boundToString(final Object object) {
    final StringBuilder stringBuilder = new StringBuilder();
    if (object instanceof Collection) {
      for (final Object obj : (Collection) object) {
        stringBuilder.append(boundToString(obj));
      }
    }
    else if (object instanceof Entity) {
      stringBuilder.append(((Entity) object).getKey());
    }
    else {
      stringBuilder.append(object);
    }

    return stringBuilder.toString();
  }
}
