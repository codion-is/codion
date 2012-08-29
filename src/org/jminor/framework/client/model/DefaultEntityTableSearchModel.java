/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.ColumnSearchModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A default EntityTableSearchModel implementation
 */
public class DefaultEntityTableSearchModel implements EntityTableSearchModel {

  private final State stSearchStateChanged = States.state();
  private final Event evtSimpleSearchStringChanged = Events.event();
  private final Event evtSimpleSearchPerformed = Events.event();

  private final String entityID;
  private final Map<String, ColumnSearchModel<Property>> propertyFilterModels = new LinkedHashMap<String, ColumnSearchModel<Property>>();
  private final Map<String, PropertySearchModel<? extends Property.SearchableProperty>> propertySearchModels = new HashMap<String, PropertySearchModel<? extends Property.SearchableProperty>>();
  private Criteria<Property.ColumnProperty> additionalSearchCriteria;
  private Conjunction searchConjunction = Conjunction.AND;
  private String rememberedSearchState = "";
  private String simpleSearchString = "";

  /**
   * Instantiates a new DefaultEntityTableSearchModel
   * @param entityID the ID of the underlying entity
   * @param connectionProvider a EntityConnectionProvider instance, required if the searchable properties include
   * foreign key properties
   * assumed to belong to the entity identified by <code>entityID</code>
   */
  public DefaultEntityTableSearchModel(final String entityID, final EntityConnectionProvider connectionProvider) {
    this(entityID, connectionProvider, new DefaultPropertyFilterModelProvider(), new DefaultPropertySearchModelProvider());
  }

  /**
   * Instantiates a new DefaultEntityTableSearchModel
   * @param entityID the ID of the underlying entity
   * @param connectionProvider a EntityConnectionProvider instance, required if the searchable properties include
   * foreign key properties
   * @param filterModelProvider provides the column filter models for this table search model
   * @param searchModelProvider provides the column search models for this table search model
   */
  public DefaultEntityTableSearchModel(final String entityID, final EntityConnectionProvider connectionProvider,
                                       final PropertyFilterModelProvider filterModelProvider,
                                       final PropertySearchModelProvider searchModelProvider) {
    Util.rejectNullValue(entityID, entityID);
    this.entityID = entityID;
    initializeFilterModels(entityID, filterModelProvider);
    initializeColumnPropertySearchModels(entityID, searchModelProvider);
    initializeForeignKeyPropertySearchModels(entityID, connectionProvider, searchModelProvider);
    rememberCurrentSearchState();
    bindEvents();
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  @Override
  public final void rememberCurrentSearchState() {
    rememberedSearchState = getSearchModelState();
    stSearchStateChanged.setActive(false);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean hasSearchStateChanged() {
    return stSearchStateChanged.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnSearchModel<Property> getPropertyFilterModel(final String propertyID) {
    if (propertyFilterModels.containsKey(propertyID)) {
      return propertyFilterModels.get(propertyID);
    }

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<ColumnSearchModel<Property>> getPropertyFilterModels() {
    return Collections.unmodifiableCollection(propertyFilterModels.values());
  }

  /** {@inheritDoc} */
  @Override
  public final boolean include(final Entity item) {
    for (final ColumnSearchModel<Property> columnFilter : propertyFilterModels.values()) {
      if (columnFilter.isEnabled() && !columnFilter.include(item)) {
        return false;
      }
    }

    return true;
  }

  /** {@inheritDoc} */
  @Override
  public final void refresh() {
    for (final PropertySearchModel model : propertySearchModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).refresh();
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    for (final PropertySearchModel model : propertySearchModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).clear();
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void clearPropertySearchModels() {
    for (final PropertySearchModel searchModel : propertySearchModels.values()) {
      searchModel.clearSearch();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<PropertySearchModel<? extends Property.SearchableProperty>> getPropertySearchModels() {
    return Collections.unmodifiableCollection(propertySearchModels.values());
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsPropertySearchModel(final String propertyID) {
    return propertySearchModels.containsKey(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public final PropertySearchModel<? extends Property.SearchableProperty> getPropertySearchModel(final String propertyID) {
    if (propertySearchModels.containsKey(propertyID)) {
      return propertySearchModels.get(propertyID);
    }

    throw new IllegalArgumentException("ColumnSearchModel not found for property with ID: " + propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isSearchEnabled(final String propertyID) {
    return containsPropertySearchModel(propertyID) && getPropertySearchModel(propertyID).isEnabled();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isFilterEnabled(final String propertyID) {
    return getPropertyFilterModel(propertyID).isEnabled();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean setSearchValues(final String propertyID, final Collection<?> values) {
    final String searchState = getSearchModelState();
    if (containsPropertySearchModel(propertyID)) {
      final PropertySearchModel searchModel = getPropertySearchModel(propertyID);
      searchModel.setEnabled(!Util.nullOrEmpty(values));
      searchModel.setUpperBound((Object) null);//because the upperBound could be a reference to the active entity which changes accordingly
      searchModel.setUpperBound(values != null && values.isEmpty() ? null : values);//this then fails to register a changed upper bound
    }
    return !searchState.equals(getSearchModelState());
  }

  /** {@inheritDoc} */
  @Override
  public final void setFilterValue(final String propertyID, final Comparable value) {
    final ColumnSearchModel<Property> filterModel = getPropertyFilterModel(propertyID);
    if (filterModel != null) {
      filterModel.setLikeValue(value);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Criteria<Property.ColumnProperty> getSearchCriteria() {
    final CriteriaSet<Property.ColumnProperty> criteriaSet = new CriteriaSet<Property.ColumnProperty>(searchConjunction);
    for (final PropertySearchModel<? extends Property.SearchableProperty> criteria : propertySearchModels.values()) {
      if (criteria.isEnabled()) {
        criteriaSet.add(criteria.getCriteria());
      }
    }
    if (additionalSearchCriteria != null) {
      criteriaSet.add(additionalSearchCriteria);
    }

    return criteriaSet.getCriteriaCount() > 0 ? criteriaSet : null;
  }

  /** {@inheritDoc} */
  @Override
  public final Criteria<Property.ColumnProperty> getAdditionalSearchCriteria() {
    return additionalSearchCriteria;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityTableSearchModel setAdditionalSearchCriteria(final Criteria<Property.ColumnProperty> criteria) {
    this.additionalSearchCriteria = criteria;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public final String getSimpleSearchString() {
    return simpleSearchString;
  }

  /** {@inheritDoc} */
  @Override
  public final void setSimpleSearchString(final String simpleSearchString) {
    this.simpleSearchString = simpleSearchString == null ? "" : simpleSearchString;
    clearPropertySearchModels();
    if (!this.simpleSearchString.isEmpty()) {
      setSearchString(this.simpleSearchString);
    }
    evtSimpleSearchStringChanged.fire();
  }

  /** {@inheritDoc} */
  @Override
  public final void performSimpleSearch() {
    final Conjunction conjunction = getSearchConjunction();
    try {
      setSearchConjunction(Conjunction.OR);
      evtSimpleSearchPerformed.fire();
    }
    finally {
      setSearchConjunction(conjunction);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Conjunction getSearchConjunction() {
    return searchConjunction;
  }

  /** {@inheritDoc} */
  @Override
  public final void setSearchConjunction(final Conjunction conjunction) {
    this.searchConjunction = conjunction;
  }

  /** {@inheritDoc} */
  @Override
  public final void setSearchEnabled(final String propertyID, final boolean enabled) {
    if (containsPropertySearchModel(propertyID)) {
      getPropertySearchModel(propertyID).setEnabled(enabled);
    }
  }

  @Override
  public final EventObserver getSimpleSearchStringObserver() {
    return evtSimpleSearchStringChanged.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getSearchStateObserver() {
    return stSearchStateChanged.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final void addSimpleSearchListener(final EventListener listener) {
    evtSimpleSearchPerformed.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSimpleSearchListener(final EventListener listener) {
    evtSimpleSearchPerformed.removeListener(listener);
  }

  private void bindEvents() {
    for (final PropertySearchModel searchModel : propertySearchModels.values()) {
      searchModel.addSearchStateListener(new EventAdapter() {
        /** {@inheritDoc} */
        @Override
        public void eventOccurred() {
          stSearchStateChanged.setActive(!rememberedSearchState.equals(getSearchModelState()));
        }
      });
    }
  }

  private void setSearchString(final String searchString) {
    final String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
    final String searchTextWithWildcards = wildcard + searchString + wildcard;
    final Collection<Property.ColumnProperty> searchProperties = Entities.getSearchProperties(entityID);
    for (final Property searchProperty : searchProperties) {
      final PropertySearchModel propertySearchModel = getPropertySearchModel(searchProperty.getPropertyID());
      propertySearchModel.setCaseSensitive(false);
      propertySearchModel.setUpperBound(searchTextWithWildcards);
      propertySearchModel.setSearchType(SearchType.LIKE);
      propertySearchModel.setEnabled(true);
    }
  }

  /**
   * @return a String representing the current state of the search models
   */
  private String getSearchModelState() {
    final StringBuilder stringBuilder = new StringBuilder();
    for (final PropertySearchModel model : getPropertySearchModels()) {
      stringBuilder.append(model.toString());
    }

    return stringBuilder.toString();
  }

  private void initializeFilterModels(final String entityID, final PropertyFilterModelProvider filterModelProvider) {
    for (final Property property : Entities.getProperties(entityID).values()) {
      if (!property.isHidden()) {
        final ColumnSearchModel<Property> filterModel = filterModelProvider.initializePropertyFilterModel(property);
        this.propertyFilterModels.put(filterModel.getColumnIdentifier().getPropertyID(), filterModel);
      }
    }
  }

  private void initializeColumnPropertySearchModels(final String entityID, final PropertySearchModelProvider searchModelProvider) {
    for (final Property.ColumnProperty columnProperty : Entities.getColumnProperties(entityID)) {
      if (!columnProperty.isForeignKeyProperty() && !columnProperty.isAggregateColumn()) {
        final PropertySearchModel<? extends Property.SearchableProperty> searchModel =
                searchModelProvider.initializePropertySearchModel(columnProperty, null);
        if (searchModel != null) {
          this.propertySearchModels.put(searchModel.getColumnIdentifier().getPropertyID(), searchModel);
        }
      }
    }
  }

  private void initializeForeignKeyPropertySearchModels(final String entityID, final EntityConnectionProvider connectionProvider,
                                                        final PropertySearchModelProvider searchModelProvider) {
    for (final Property.ForeignKeyProperty foreignKeyProperty : Entities.getForeignKeyProperties(entityID)) {
      final PropertySearchModel<? extends Property.SearchableProperty> searchModel =
              searchModelProvider.initializePropertySearchModel(foreignKeyProperty, connectionProvider);
      if (searchModel != null) {
        this.propertySearchModels.put(searchModel.getColumnIdentifier().getPropertyID(), searchModel);
      }
    }
  }
}
