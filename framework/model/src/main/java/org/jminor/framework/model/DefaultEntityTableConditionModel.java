/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Conjunction;
import org.jminor.common.Event;
import org.jminor.common.EventListener;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.States;
import org.jminor.common.Util;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.Conditions;
import org.jminor.common.model.FilterCondition;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A default EntityTableConditionModel implementation
 */
public class DefaultEntityTableConditionModel implements EntityTableConditionModel {

  private final State conditionStateChangedState = States.state();
  private final Event conditionStateChangedEvent = Events.event();
  private final Event<String> simpleConditionStringChangedEvent = Events.event();
  private final Event simpleSearchPerformedEvent = Events.event();

  private final String entityID;
  private final Map<String, ColumnConditionModel<Property>> propertyFilterModels = new LinkedHashMap<>();
  private final Map<String, PropertyConditionModel<? extends Property>> propertyConditionModels = new HashMap<>();
  private Condition.Provider<Property.ColumnProperty> additionalConditionProvider;
  private FilterCondition<Entity> additionalFilterCondition;
  private Conjunction conjunction = Conjunction.AND;
  private String rememberedConditionState = "";
  private String simpleConditionString = "";

  /**
   * Instantiates a new DefaultEntityTableConditionModel
   * @param entityID the ID of the underlying entity
   * @param connectionProvider a EntityConnectionProvider instance, required if the searchable properties include
   * foreign key properties
   * @param filterModelProvider provides the column filter models for this table condition model
   * @param conditionModelProvider provides the column condition models for this table condition model
   */
  public DefaultEntityTableConditionModel(final String entityID, final EntityConnectionProvider connectionProvider,
                                          final PropertyFilterModelProvider filterModelProvider,
                                          final PropertyConditionModelProvider conditionModelProvider) {
    Objects.requireNonNull(entityID, entityID);
    this.entityID = entityID;
    initializeFilterModels(entityID, filterModelProvider);
    initializeColumnPropertyConditionModels(entityID, conditionModelProvider);
    initializeForeignKeyPropertyConditionModels(entityID, connectionProvider, conditionModelProvider);
    rememberCurrentConditionState();
    bindEvents();
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  @Override
  public final void rememberCurrentConditionState() {
    rememberedConditionState = getConditionModelState();
    conditionStateChangedState.setActive(false);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean hasConditionStateChanged() {
    return conditionStateChangedState.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnConditionModel<Property> getPropertyFilterModel(final String propertyID) {
    if (propertyFilterModels.containsKey(propertyID)) {
      return propertyFilterModels.get(propertyID);
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<ColumnConditionModel<Property>> getPropertyFilterModels() {
    return Collections.unmodifiableCollection(propertyFilterModels.values());
  }

  /** {@inheritDoc} */
  @Override
  public final boolean include(final Entity item) {
    for (final ColumnConditionModel<Property> columnFilter : propertyFilterModels.values()) {
      if (!columnFilter.include(item)) {
        return false;
      }
    }
    if (additionalFilterCondition != null) {
      return additionalFilterCondition.include(item);
    }

    return true;
  }

  /** {@inheritDoc} */
  @Override
  public final void refresh() {
    for (final PropertyConditionModel model : propertyConditionModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).refresh();
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    for (final PropertyConditionModel model : propertyConditionModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).clear();
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void clearPropertyConditionModels() {
    for (final PropertyConditionModel conditionModel : propertyConditionModels.values()) {
      conditionModel.clearCondition();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<PropertyConditionModel<? extends Property>> getPropertyConditionModels() {
    return Collections.unmodifiableCollection(propertyConditionModels.values());
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsPropertyConditionModel(final String propertyID) {
    return propertyConditionModels.containsKey(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public final PropertyConditionModel<? extends Property> getPropertyConditionModel(final String propertyID) {
    return propertyConditionModels.get(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isEnabled() {
    for (final PropertyConditionModel conditionModel : propertyConditionModels.values()) {
      if (conditionModel.isEnabled()) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isEnabled(final String propertyID) {
    return containsPropertyConditionModel(propertyID) && getPropertyConditionModel(propertyID).isEnabled();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isFilterEnabled(final String propertyID) {
    final ColumnConditionModel<Property> propertyFilterModel = getPropertyFilterModel(propertyID);

    return propertyFilterModel != null && propertyFilterModel.isEnabled();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean setConditionValues(final String propertyID, final Collection values) {
    final String conditionModelState = getConditionModelState();
    if (containsPropertyConditionModel(propertyID)) {
      final PropertyConditionModel conditionModel = getPropertyConditionModel(propertyID);
      conditionModel.setEnabled(!Util.nullOrEmpty(values));
      conditionModel.setUpperBound((Object) null);//because the upperBound could be a reference to the active entity which changes accordingly
      conditionModel.setUpperBound(values != null && values.isEmpty() ? null : values);//this then fails to register a changed upper bound
    }
    return !conditionModelState.equals(getConditionModelState());
  }

  /** {@inheritDoc} */
  @Override
  public final void setFilterValue(final String propertyID, final Comparable value) {
    final ColumnConditionModel<Property> filterModel = getPropertyFilterModel(propertyID);
    if (filterModel != null) {
      filterModel.setLikeValue(value);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Condition<Property.ColumnProperty> getTableCondition() {
    final Condition.Set<Property.ColumnProperty> conditionSet = Conditions.conditionSet(conjunction);
    for (final PropertyConditionModel<? extends Property> conditionModel : propertyConditionModels.values()) {
      if (conditionModel.isEnabled()) {
        conditionSet.add(conditionModel.getCondition());
      }
    }
    if (additionalConditionProvider != null) {
      conditionSet.add(additionalConditionProvider.getCondition());
    }

    return conditionSet.getConditionCount() > 0 ? conditionSet : null;
  }

  /** {@inheritDoc} */
  @Override
  public final Condition.Provider<Property.ColumnProperty> getAdditionalTableConditionProvider() {
    return additionalConditionProvider;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableConditionModel setAdditionalTableConditionProvider(final Condition.Provider<Property.ColumnProperty> conditionProvider) {
    this.additionalConditionProvider = conditionProvider;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public FilterCondition<Entity> getAdditionalTableFilterCondition() {
    return this.additionalFilterCondition;
  }

  /** {@inheritDoc} */
  @Override
  public EntityTableConditionModel setAdditionalTableFilterCondition(final FilterCondition<Entity> filterCondition) {
    this.additionalFilterCondition = filterCondition;
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final String getSimpleConditionString() {
    return simpleConditionString;
  }

  /** {@inheritDoc} */
  @Override
  public final void setSimpleConditionString(final String simpleConditionString) {
    this.simpleConditionString = simpleConditionString == null ? "" : simpleConditionString;
    clearPropertyConditionModels();
    if (this.simpleConditionString.length() != 0) {
      setConditionString(this.simpleConditionString);
    }
    simpleConditionStringChangedEvent.fire(this.simpleConditionString);
  }

  /** {@inheritDoc} */
  @Override
  public final void performSimpleSearch() {
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
  public final Conjunction getConjunction() {
    return conjunction;
  }

  /** {@inheritDoc} */
  @Override
  public final void setConjunction(final Conjunction conjunction) {
    this.conjunction = conjunction;
  }

  /** {@inheritDoc} */
  @Override
  public final void setEnabled(final String propertyID, final boolean enabled) {
    if (containsPropertyConditionModel(propertyID)) {
      getPropertyConditionModel(propertyID).setEnabled(enabled);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<String> getSimpleConditionStringObserver() {
    return simpleConditionStringChangedEvent.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getConditionStateObserver() {
    return conditionStateChangedState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final void addConditionStateListener(final EventListener listener) {
    conditionStateChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeConditionStateListener(final EventListener listener) {
    conditionStateChangedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSimpleConditionListener(final EventListener listener) {
    simpleSearchPerformedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSimpleConditionListener(final EventListener listener) {
    simpleSearchPerformedEvent.removeListener(listener);
  }

  private void bindEvents() {
    for (final PropertyConditionModel conditionModel : propertyConditionModels.values()) {
      conditionModel.addConditionStateListener(() -> {
        conditionStateChangedState.setActive(!rememberedConditionState.equals(getConditionModelState()));
        conditionStateChangedEvent.fire();
      });
    }
  }

  private void setConditionString(final String searchString) {
    final String wildcard = Property.WILDCARD_CHARACTER.get();
    final String searchTextWithWildcards = wildcard + searchString + wildcard;
    final Collection<Property.ColumnProperty> searchProperties = Entities.getSearchProperties(entityID);
    for (final Property.ColumnProperty searchProperty : searchProperties) {
      final PropertyConditionModel conditionModel = getPropertyConditionModel(searchProperty.getPropertyID());
      conditionModel.setCaseSensitive(false);
      conditionModel.setUpperBound(searchTextWithWildcards);
      conditionModel.setConditionType(Condition.Type.LIKE);
      conditionModel.setEnabled(true);
    }
  }

  /**
   * @return a String representing the current state of the condition models
   */
  private String getConditionModelState() {
    final StringBuilder stringBuilder = new StringBuilder();
    for (final PropertyConditionModel model : getPropertyConditionModels()) {
      stringBuilder.append(model.toString());
    }

    return stringBuilder.toString();
  }

  private void initializeFilterModels(final String entityID, final PropertyFilterModelProvider filterModelProvider) {
    if (filterModelProvider != null) {
      for (final Property property : Entities.getProperties(entityID).values()) {
        if (!property.isHidden()) {
          final ColumnConditionModel<Property> filterModel = filterModelProvider.initializePropertyFilterModel(property);
          this.propertyFilterModels.put(filterModel.getColumnIdentifier().getPropertyID(), filterModel);
        }
      }
    }
  }

  private void initializeColumnPropertyConditionModels(final String entityID, final PropertyConditionModelProvider conditionModelProvider) {
    for (final Property.ColumnProperty columnProperty : Entities.getColumnProperties(entityID)) {
      if (!columnProperty.isForeignKeyProperty() && !columnProperty.isAggregateColumn()) {
        final PropertyConditionModel<Property.ColumnProperty> conditionModel =
                conditionModelProvider.initializePropertyConditionModel(columnProperty);
        if (conditionModel != null) {
          this.propertyConditionModels.put(conditionModel.getColumnIdentifier().getPropertyID(), conditionModel);
        }
      }
    }
  }

  private void initializeForeignKeyPropertyConditionModels(final String entityID, final EntityConnectionProvider connectionProvider,
                                                           final PropertyConditionModelProvider conditionModelProvider) {
    for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(entityID)) {
      final PropertyConditionModel<Property.ForeignKeyProperty> conditionModel =
              conditionModelProvider.initializeForeignKeyConditionModel(foreignKeyProperty, connectionProvider);
      if (conditionModel != null) {
        this.propertyConditionModels.put(conditionModel.getColumnIdentifier().getPropertyID(), conditionModel);
      }
    }
  }
}
