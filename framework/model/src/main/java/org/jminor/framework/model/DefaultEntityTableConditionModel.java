/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Conjunction;
import org.jminor.common.Util;
import org.jminor.common.db.ConditionType;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.EventObserver;
import org.jminor.common.event.Events;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.state.State;
import org.jminor.common.state.StateObserver;
import org.jminor.common.state.States;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.jminor.framework.db.condition.Conditions.conditionSet;

/**
 * A default EntityTableConditionModel implementation
 */
public final class DefaultEntityTableConditionModel implements EntityTableConditionModel {

  private final State conditionStateChangedState = States.state();
  private final Event conditionStateChangedEvent = Events.event();
  private final Event<String> simpleConditionStringChangedEvent = Events.event();
  private final Event simpleSearchPerformedEvent = Events.event();

  private final String entityId;
  private final EntityConnectionProvider connectionProvider;
  private final Map<String, ColumnConditionModel<Property>> propertyFilterModels = new LinkedHashMap<>();
  private final Map<String, PropertyConditionModel<? extends Property>> propertyConditionModels = new HashMap<>();
  private Condition.Provider additionalConditionProvider;
  private Conjunction conjunction = Conjunction.AND;
  private String rememberedConditionState = "";
  private String simpleConditionString = "";

  /**
   * Instantiates a new DefaultEntityTableConditionModel
   * @param entityId the ID of the underlying entity
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
    rememberCurrentConditionState();
    bindEvents();
  }

  /** {@inheritDoc} */
  @Override
  public String getEntityId() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public void rememberCurrentConditionState() {
    rememberedConditionState = getConditionModelState();
    conditionStateChangedState.set(false);
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasConditionStateChanged() {
    return conditionStateChangedState.get();
  }

  /** {@inheritDoc} */
  @Override
  public ColumnConditionModel<Property> getPropertyFilterModel(final String propertyId) {
    if (propertyFilterModels.containsKey(propertyId)) {
      return propertyFilterModels.get(propertyId);
    }

    throw new IllegalArgumentException("No property filter model found for property " + propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public Collection<ColumnConditionModel<Property>> getPropertyFilterModels() {
    return Collections.unmodifiableCollection(propertyFilterModels.values());
  }

  /** {@inheritDoc} */
  @Override
  public void refresh() {
    for (final PropertyConditionModel model : propertyConditionModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).refresh();
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void clear() {
    for (final PropertyConditionModel model : propertyConditionModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).clear();
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void clearPropertyConditionModels() {
    for (final PropertyConditionModel conditionModel : propertyConditionModels.values()) {
      conditionModel.clearCondition();
    }
  }

  /** {@inheritDoc} */
  @Override
  public Collection<PropertyConditionModel<? extends Property>> getPropertyConditionModels() {
    return Collections.unmodifiableCollection(propertyConditionModels.values());
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsPropertyConditionModel(final String propertyId) {
    return propertyConditionModels.containsKey(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public PropertyConditionModel<? extends Property> getPropertyConditionModel(final String propertyId) {
    if (propertyConditionModels.containsKey(propertyId)) {
      return propertyConditionModels.get(propertyId);
    }

    throw new IllegalArgumentException("Condition model not found for property: " + propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEnabled() {
    return propertyConditionModels.values().stream().anyMatch(ColumnConditionModel::isEnabled);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEnabled(final String propertyId) {
    return containsPropertyConditionModel(propertyId) && getPropertyConditionModel(propertyId).isEnabled();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isFilterEnabled(final String propertyId) {
    final ColumnConditionModel<Property> propertyFilterModel = getPropertyFilterModel(propertyId);

    return propertyFilterModel != null && propertyFilterModel.isEnabled();
  }

  /** {@inheritDoc} */
  @Override
  public boolean setConditionValues(final String propertyId, final Collection values) {
    final String conditionModelState = getConditionModelState();
    if (containsPropertyConditionModel(propertyId)) {
      final PropertyConditionModel conditionModel = getPropertyConditionModel(propertyId);
      conditionModel.setEnabled(!Util.nullOrEmpty(values));
      conditionModel.setUpperBound((Object) null);//because the upperBound could be a reference to the active entity which changes accordingly
      conditionModel.setUpperBound(values != null && values.isEmpty() ? null : values);//this then fails to register a changed upper bound
    }
    return !conditionModelState.equals(getConditionModelState());
  }

  /** {@inheritDoc} */
  @Override
  public void setFilterValue(final String propertyId, final Comparable value) {
    final ColumnConditionModel<Property> filterModel = getPropertyFilterModel(propertyId);
    if (filterModel != null) {
      filterModel.setLikeValue(value);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Condition getCondition() {
    final Condition.Set conditionSet = conditionSet(conjunction);
    for (final PropertyConditionModel<? extends Property> conditionModel : propertyConditionModels.values()) {
      if (conditionModel.isEnabled()) {
        conditionSet.add(conditionModel.getCondition());
      }
    }
    if (additionalConditionProvider != null) {
      conditionSet.add(additionalConditionProvider.getCondition());
    }

    return conditionSet.getConditions().isEmpty() ? null : conditionSet;
  }

  /** {@inheritDoc} */
  @Override
  public Condition.Provider getAdditionalConditionProvider() {
    return additionalConditionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public EntityTableConditionModel setAdditionalConditionProvider(final Condition.Provider conditionProvider) {
    this.additionalConditionProvider = conditionProvider;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public String getSimpleConditionString() {
    return simpleConditionString;
  }

  /** {@inheritDoc} */
  @Override
  public void setSimpleConditionString(final String simpleConditionString) {
    this.simpleConditionString = simpleConditionString == null ? "" : simpleConditionString;
    clearPropertyConditionModels();
    if (this.simpleConditionString.length() != 0) {
      setConditionString(this.simpleConditionString);
    }
    simpleConditionStringChangedEvent.fire(this.simpleConditionString);
  }

  /** {@inheritDoc} */
  @Override
  public void performSimpleSearch() {
    final Conjunction previousConjunction = getConjunction();
    try {
      setConjunction(Conjunction.OR);
      simpleSearchPerformedEvent.fire();
    }
    finally {
      setConjunction(previousConjunction);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Conjunction getConjunction() {
    return conjunction;
  }

  /** {@inheritDoc} */
  @Override
  public void setConjunction(final Conjunction conjunction) {
    this.conjunction = conjunction;
  }

  /** {@inheritDoc} */
  @Override
  public void setEnabled(final String propertyId, final boolean enabled) {
    if (containsPropertyConditionModel(propertyId)) {
      getPropertyConditionModel(propertyId).setEnabled(enabled);
    }
  }

  /** {@inheritDoc} */
  @Override
  public EventObserver<String> getSimpleConditionStringObserver() {
    return simpleConditionStringChangedEvent.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public StateObserver getConditionStateObserver() {
    return conditionStateChangedState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public void addConditionStateListener(final EventListener listener) {
    conditionStateChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeConditionStateListener(final EventListener listener) {
    conditionStateChangedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void addSimpleConditionListener(final EventListener listener) {
    simpleSearchPerformedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public void removeSimpleConditionListener(final EventListener listener) {
    simpleSearchPerformedEvent.removeListener(listener);
  }

  private void bindEvents() {
    for (final PropertyConditionModel conditionModel : propertyConditionModels.values()) {
      conditionModel.addConditionStateListener(() -> {
        conditionStateChangedState.set(!rememberedConditionState.equals(getConditionModelState()));
        conditionStateChangedEvent.fire();
      });
    }
  }

  private void setConditionString(final String searchString) {
    final Collection<ColumnProperty> searchProperties =
            connectionProvider.getDomain().getDefinition(entityId).getSearchProperties();
    for (final ColumnProperty searchProperty : searchProperties) {
      final PropertyConditionModel conditionModel = getPropertyConditionModel(searchProperty.getPropertyId());
      conditionModel.setCaseSensitive(false);
      conditionModel.setAutomaticWildcard(ColumnConditionModel.AutomaticWildcard.PREFIX_AND_POSTFIX);
      conditionModel.setUpperBound(searchString);
      conditionModel.setConditionType(ConditionType.LIKE);
      conditionModel.setEnabled(true);
    }
  }

  /**
   * @return a String representing the current state of the condition models
   */
  private String getConditionModelState() {
    return getPropertyConditionModels().stream().map(PropertyConditionModel::toString).collect(Collectors.joining());
  }

  private void initializeFilterModels(final String entityId, final PropertyFilterModelProvider filterModelProvider) {
    if (filterModelProvider != null) {
      for (final Property property : connectionProvider.getDomain().getDefinition(entityId).getProperties()) {
        if (!property.isHidden()) {
          final ColumnConditionModel<Property> filterModel = filterModelProvider.initializePropertyFilterModel(property);
          this.propertyFilterModels.put(filterModel.getColumnIdentifier().getPropertyId(), filterModel);
        }
      }
    }
  }

  private void initializeColumnPropertyConditionModels(final String entityId, final PropertyConditionModelProvider conditionModelProvider) {
    for (final ColumnProperty columnProperty :
            connectionProvider.getDomain().getDefinition(entityId).getColumnProperties()) {
      if (!columnProperty.isForeignKeyProperty() && !columnProperty.isAggregateColumn()) {
        final PropertyConditionModel<ColumnProperty> conditionModel =
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
            connectionProvider.getDomain().getDefinition(entityId).getForeignKeyProperties()) {
      final PropertyConditionModel<ForeignKeyProperty> conditionModel =
              conditionModelProvider.initializeForeignKeyConditionModel(foreignKeyProperty, connectionProvider);
      if (conditionModel != null) {
        this.propertyConditionModels.put(conditionModel.getColumnIdentifier().getPropertyId(), conditionModel);
      }
    }
  }
}
