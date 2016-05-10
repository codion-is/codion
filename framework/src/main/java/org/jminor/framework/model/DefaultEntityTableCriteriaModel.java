/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.db.criteria.CriteriaUtil;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.ColumnCriteriaModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A default EntityTableCriteriaModel implementation
 */
public class DefaultEntityTableCriteriaModel implements EntityTableCriteriaModel {

  private final State criteriaStateChangedState = States.state();
  private final Event criteriaStateChangedEvent = Events.event();
  private final Event<String> simpleCriteriaStringChangedEvent = Events.event();
  private final Event simpleSearchPerformedEvent = Events.event();

  private final String entityID;
  private final Map<String, ColumnCriteriaModel<Property>> propertyFilterModels = new LinkedHashMap<>();
  private final Map<String, PropertyCriteriaModel<? extends Property.SearchableProperty>> propertyCriteriaModels = new HashMap<>();
  private Criteria<Property.ColumnProperty> additionalCriteria;
  private Conjunction conjunction = Conjunction.AND;
  private String rememberedCriteriaState = "";
  private String simpleCriteriaString = "";

  /**
   * Instantiates a new DefaultEntityTableCriteriaModel
   * @param entityID the ID of the underlying entity
   * @param connectionProvider a EntityConnectionProvider instance, required if the searchable properties include
   * foreign key properties
   * @param filterModelProvider provides the column filter models for this table criteria model
   * @param criteriaModelProvider provides the column criteria models for this table criteria model
   */
  public DefaultEntityTableCriteriaModel(final String entityID, final EntityConnectionProvider connectionProvider,
                                         final PropertyFilterModelProvider filterModelProvider,
                                         final PropertyCriteriaModelProvider criteriaModelProvider) {
    Util.rejectNullValue(entityID, entityID);
    this.entityID = entityID;
    initializeFilterModels(entityID, filterModelProvider);
    initializeColumnPropertyCriteriaModels(entityID, criteriaModelProvider);
    initializeForeignKeyPropertyCriteriaModels(entityID, connectionProvider, criteriaModelProvider);
    rememberCurrentCriteriaState();
    bindEvents();
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  @Override
  public final void rememberCurrentCriteriaState() {
    rememberedCriteriaState = getCriteriaModelState();
    criteriaStateChangedState.setActive(false);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean hasCriteriaStateChanged() {
    return criteriaStateChangedState.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnCriteriaModel<Property> getPropertyFilterModel(final String propertyID) {
    if (propertyFilterModels.containsKey(propertyID)) {
      return propertyFilterModels.get(propertyID);
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<ColumnCriteriaModel<Property>> getPropertyFilterModels() {
    return Collections.unmodifiableCollection(propertyFilterModels.values());
  }

  /** {@inheritDoc} */
  @Override
  public final boolean include(final Entity item) {
    for (final ColumnCriteriaModel<Property> columnFilter : propertyFilterModels.values()) {
      if (!columnFilter.include(item)) {
        return false;
      }
    }

    return true;
  }

  /** {@inheritDoc} */
  @Override
  public final void refresh() {
    for (final PropertyCriteriaModel model : propertyCriteriaModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).refresh();
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    for (final PropertyCriteriaModel model : propertyCriteriaModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).clear();
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void clearPropertyCriteriaModels() {
    for (final PropertyCriteriaModel criteriaModel : propertyCriteriaModels.values()) {
      criteriaModel.clearCriteria();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<PropertyCriteriaModel<? extends Property.SearchableProperty>> getPropertyCriteriaModels() {
    return Collections.unmodifiableCollection(propertyCriteriaModels.values());
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsPropertyCriteriaModel(final String propertyID) {
    return propertyCriteriaModels.containsKey(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public final PropertyCriteriaModel<? extends Property.SearchableProperty> getPropertyCriteriaModel(final String propertyID) {
    if (propertyCriteriaModels.containsKey(propertyID)) {
      return propertyCriteriaModels.get(propertyID);
    }

    throw new IllegalArgumentException("ColumnCriteriaModel not found for property with ID: " + propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isEnabled() {
    for (final PropertyCriteriaModel criteriaModel : propertyCriteriaModels.values()) {
      if (criteriaModel.isEnabled()) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isEnabled(final String propertyID) {
    return containsPropertyCriteriaModel(propertyID) && getPropertyCriteriaModel(propertyID).isEnabled();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isFilterEnabled(final String propertyID) {
    return getPropertyFilterModel(propertyID).isEnabled();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean setCriteriaValues(final String propertyID, final Collection<?> values) {
    final String criteriaState = getCriteriaModelState();
    if (containsPropertyCriteriaModel(propertyID)) {
      final PropertyCriteriaModel criteriaModel = getPropertyCriteriaModel(propertyID);
      criteriaModel.setEnabled(!Util.nullOrEmpty(values));
      criteriaModel.setUpperBound((Object) null);//because the upperBound could be a reference to the active entity which changes accordingly
      criteriaModel.setUpperBound(values != null && values.isEmpty() ? null : values);//this then fails to register a changed upper bound
    }
    return !criteriaState.equals(getCriteriaModelState());
  }

  /** {@inheritDoc} */
  @Override
  public final void setFilterValue(final String propertyID, final Comparable value) {
    final ColumnCriteriaModel<Property> filterModel = getPropertyFilterModel(propertyID);
    if (filterModel != null) {
      filterModel.setLikeValue(value);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Criteria<Property.ColumnProperty> getTableCriteria() {
    final CriteriaSet<Property.ColumnProperty> criteriaSet = CriteriaUtil.criteriaSet(conjunction);
    for (final PropertyCriteriaModel<? extends Property.SearchableProperty> criteriaModel : propertyCriteriaModels.values()) {
      if (criteriaModel.isEnabled()) {
        criteriaSet.add(criteriaModel.getCriteria());
      }
    }
    if (additionalCriteria != null) {
      criteriaSet.add(additionalCriteria);
    }

    return criteriaSet.getCriteriaCount() > 0 ? criteriaSet : null;
  }

  /** {@inheritDoc} */
  @Override
  public final Criteria<Property.ColumnProperty> getAdditionalTableCriteria() {
    return additionalCriteria;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableCriteriaModel setAdditionalTableCriteria(final Criteria<Property.ColumnProperty> criteria) {
    this.additionalCriteria = criteria;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final String getSimpleCriteriaString() {
    return simpleCriteriaString;
  }

  /** {@inheritDoc} */
  @Override
  public final void setSimpleCriteriaString(final String simpleCriteriaString) {
    this.simpleCriteriaString = simpleCriteriaString == null ? "" : simpleCriteriaString;
    clearPropertyCriteriaModels();
    if (this.simpleCriteriaString.length() != 0) {
      setCriteriaString(this.simpleCriteriaString);
    }
    simpleCriteriaStringChangedEvent.fire(this.simpleCriteriaString);
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
    if (containsPropertyCriteriaModel(propertyID)) {
      getPropertyCriteriaModel(propertyID).setEnabled(enabled);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<String> getSimpleCriteriaStringObserver() {
    return simpleCriteriaStringChangedEvent.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getCriteriaStateObserver() {
    return criteriaStateChangedState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final void addCriteriaStateListener(final EventListener listener) {
    criteriaStateChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeCriteriaStateListener(final EventListener listener) {
    criteriaStateChangedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSimpleCriteriaListener(final EventListener listener) {
    simpleSearchPerformedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSimpleCriteriaListener(final EventListener listener) {
    simpleSearchPerformedEvent.removeListener(listener);
  }

  private void bindEvents() {
    for (final PropertyCriteriaModel criteriaModel : propertyCriteriaModels.values()) {
      criteriaModel.addCriteriaStateListener(new EventListener() {
        @Override
        public void eventOccurred() {
          criteriaStateChangedState.setActive(!rememberedCriteriaState.equals(getCriteriaModelState()));
          criteriaStateChangedEvent.fire();
        }
      });
    }
  }

  private void setCriteriaString(final String searchString) {
    final String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
    final String searchTextWithWildcards = wildcard + searchString + wildcard;
    final Collection<Property.ColumnProperty> searchProperties = Entities.getSearchProperties(entityID);
    for (final Property searchProperty : searchProperties) {
      final PropertyCriteriaModel propertyCriteriaModel = getPropertyCriteriaModel(searchProperty.getPropertyID());
      propertyCriteriaModel.setCaseSensitive(false);
      propertyCriteriaModel.setUpperBound(searchTextWithWildcards);
      propertyCriteriaModel.setSearchType(SearchType.LIKE);
      propertyCriteriaModel.setEnabled(true);
    }
  }

  /**
   * @return a String representing the current state of the criteria models
   */
  private String getCriteriaModelState() {
    final StringBuilder stringBuilder = new StringBuilder();
    for (final PropertyCriteriaModel model : getPropertyCriteriaModels()) {
      stringBuilder.append(model.toString());
    }

    return stringBuilder.toString();
  }

  private void initializeFilterModels(final String entityID, final PropertyFilterModelProvider filterModelProvider) {
    if (filterModelProvider != null) {
      for (final Property property : Entities.getProperties(entityID).values()) {
        if (!property.isHidden()) {
          final ColumnCriteriaModel<Property> filterModel = filterModelProvider.initializePropertyFilterModel(property);
          this.propertyFilterModels.put(filterModel.getColumnIdentifier().getPropertyID(), filterModel);
        }
      }
    }
  }

  private void initializeColumnPropertyCriteriaModels(final String entityID, final PropertyCriteriaModelProvider criteriaModelProvider) {
    for (final Property.ColumnProperty columnProperty : Entities.getColumnProperties(entityID)) {
      if (!columnProperty.isForeignKeyProperty() && !columnProperty.isAggregateColumn()) {
        final PropertyCriteriaModel<? extends Property.SearchableProperty> criteriaModel =
                criteriaModelProvider.initializePropertyCriteriaModel(columnProperty, null);
        if (criteriaModel != null) {
          this.propertyCriteriaModels.put(criteriaModel.getColumnIdentifier().getPropertyID(), criteriaModel);
        }
      }
    }
  }

  private void initializeForeignKeyPropertyCriteriaModels(final String entityID, final EntityConnectionProvider connectionProvider,
                                                          final PropertyCriteriaModelProvider criteriaModelProvider) {
    for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(entityID)) {
      final PropertyCriteriaModel<? extends Property.SearchableProperty> criteriaModel =
              criteriaModelProvider.initializePropertyCriteriaModel(foreignKeyProperty, connectionProvider);
      if (criteriaModel != null) {
        this.propertyCriteriaModels.put(criteriaModel.getColumnIdentifier().getPropertyID(), criteriaModel);
      }
    }
  }
}
