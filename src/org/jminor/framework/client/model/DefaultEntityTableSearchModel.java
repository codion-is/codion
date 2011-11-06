/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.ColumnSearchModel;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A default EntityTableSearchModel implementation
 */
public class DefaultEntityTableSearchModel implements EntityTableSearchModel, EntityDataProvider {

  private final State stSearchStateChanged = States.state();
  private final Event evtSimpleSearchStringChanged = Events.event();
  private final Event evtSimpleSearchPerformed = Events.event();

  private final String entityID;
  private final EntityConnectionProvider connectionProvider;
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
    this.connectionProvider = connectionProvider;
    for (final Property property : Entities.getProperties(entityID).values()) {
      if (!property.isHidden()) {
        final ColumnSearchModel<Property> filterModel = filterModelProvider.initializePropertyFilterModel(property);
        this.propertyFilterModels.put(filterModel.getColumnIdentifier().getPropertyID(), filterModel);
      }
      if (property instanceof Property.SearchableProperty && !property.hasParentProperty() && !isAggregateColumnProperty(property)) {
        final PropertySearchModel<? extends Property.SearchableProperty> searchModel =
                searchModelProvider.initializePropertySearchModel((Property.SearchableProperty) property, connectionProvider);
        if (searchModel != null) {
          this.propertySearchModels.put(searchModel.getColumnIdentifier().getPropertyID(), searchModel);
        }
      }
    }
    this.rememberedSearchState = getSearchModelState();
    bindEvents();
  }

  /** {@inheritDoc} */
  public final String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  public final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /** {@inheritDoc} */
  public final List<Property.SearchableProperty> getSearchableProperties() {
    final List<Property.SearchableProperty> searchProperties = new ArrayList<Property.SearchableProperty>();
    for (final PropertySearchModel<? extends Property.SearchableProperty> searchModel : propertySearchModels.values()) {
      searchProperties.add(searchModel.getColumnIdentifier());
    }

    return searchProperties;
  }

  /** {@inheritDoc} */
  public final void rememberCurrentSearchState() {
    rememberedSearchState = getSearchModelState();
    stSearchStateChanged.setActive(false);
  }

  /** {@inheritDoc} */
  public final boolean hasSearchStateChanged() {
    return stSearchStateChanged.isActive();
  }

  /** {@inheritDoc} */
  public final ColumnSearchModel<Property> getPropertyFilterModel(final String propertyID) {
    if (propertyFilterModels.containsKey(propertyID)) {
      return propertyFilterModels.get(propertyID);
    }

    return null;
  }

  /** {@inheritDoc} */
  public final Collection<ColumnSearchModel<Property>> getPropertyFilterModels() {
    return Collections.unmodifiableCollection(propertyFilterModels.values());
  }

  /** {@inheritDoc} */
  public final List<ColumnSearchModel<Property>> getPropertyFilterModelsOrdered() {
    return new ArrayList<ColumnSearchModel<Property>>(propertyFilterModels.values());
  }

  /** {@inheritDoc} */
  public final boolean include(final Entity item) {
    for (final ColumnSearchModel<Property> columnFilter : propertyFilterModels.values()) {
      if (columnFilter.isEnabled() && !columnFilter.include(item)) {
        return false;
      }
    }

    return true;
  }

  /** {@inheritDoc} */
  public final void refresh() {
    for (final PropertySearchModel model : propertySearchModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).refresh();
      }
    }
  }

  /** {@inheritDoc} */
  public final void clear() {
    for (final PropertySearchModel model : propertySearchModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).clear();
      }
    }
  }

  /** {@inheritDoc} */
  public final void clearPropertySearchModels() {
    for (final PropertySearchModel searchModel : propertySearchModels.values()) {
      searchModel.clearSearch();
    }
  }

  /** {@inheritDoc} */
  public final Collection<PropertySearchModel<? extends Property.SearchableProperty>> getPropertySearchModels() {
    return Collections.unmodifiableCollection(propertySearchModels.values());
  }

  /** {@inheritDoc} */
  public final boolean containsPropertySearchModel(final String propertyID) {
    return propertySearchModels.containsKey(propertyID);
  }

  /** {@inheritDoc} */
  public final PropertySearchModel<? extends Property.SearchableProperty> getPropertySearchModel(final String propertyID) {
    if (propertySearchModels.containsKey(propertyID)) {
      return propertySearchModels.get(propertyID);
    }

    throw new IllegalArgumentException("ColumnSearchModel not found for property with ID: " + propertyID);
  }

  /** {@inheritDoc} */
  public final boolean isSearchEnabled(final String propertyID) {
    return containsPropertySearchModel(propertyID) && getPropertySearchModel(propertyID).isEnabled();
  }

  /** {@inheritDoc} */
  public final boolean isFilterEnabled(final String propertyID) {
    return getPropertyFilterModel(propertyID).isEnabled();
  }

  /** {@inheritDoc} */
  public final boolean setSearchValues(final String propertyID, final Collection<?> values) {
    final String searchState = getSearchModelState();
    if (containsPropertySearchModel(propertyID)) {
      final PropertySearchModel searchModel = getPropertySearchModel(propertyID);
      searchModel.setEnabled(values != null && !values.isEmpty());
      searchModel.setUpperBound((Object) null);//because the upperBound could be a reference to the active entity which changes accordingly
      searchModel.setUpperBound(values != null && values.isEmpty() ? null : values);//this then fails to register a changed upper bound
    }
    return !searchState.equals(getSearchModelState());
  }

  /** {@inheritDoc} */
  public final void setFilterValue(final String propertyID, final Comparable value) {
    final ColumnSearchModel<Property> filterModel = getPropertyFilterModel(propertyID);
    if (filterModel != null) {
      filterModel.setLikeValue(value);
    }
  }

  /** {@inheritDoc} */
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
  public final Criteria<Property.ColumnProperty> getAdditionalSearchCriteria() {
    return additionalSearchCriteria;
  }

  /** {@inheritDoc} */
  public final EntityTableSearchModel setAdditionalSearchCriteria(final Criteria<Property.ColumnProperty> criteria) {
    this.additionalSearchCriteria = criteria;
    return this;
  }

  /** {@inheritDoc} */
  public final String getSimpleSearchString() {
    return simpleSearchString;
  }

  /** {@inheritDoc} */
  public final void setSimpleSearchString(final String simpleSearchString) {
    this.simpleSearchString = simpleSearchString == null ? "" : simpleSearchString;
    evtSimpleSearchStringChanged.fire();
    updateSearchModels();
  }

  /** {@inheritDoc} */
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
  public final Conjunction getSearchConjunction() {
    return searchConjunction;
  }

  /** {@inheritDoc} */
  public final void setSearchConjunction(final Conjunction conjunction) {
    this.searchConjunction = conjunction;
  }

  /** {@inheritDoc} */
  public final void setSearchEnabled(final String propertyID, final boolean enabled) {
    if (containsPropertySearchModel(propertyID)) {
      getPropertySearchModel(propertyID).setEnabled(enabled);
    }
  }

  public final EventObserver getSimpleSearchStringObserver() {
    return evtSimpleSearchStringChanged.getObserver();
  }

  /** {@inheritDoc} */
  public final StateObserver getSearchStateObserver() {
    return stSearchStateChanged.getObserver();
  }

  /** {@inheritDoc} */
  public final void addSimpleSearchListener(final ActionListener listener) {
    evtSimpleSearchPerformed.addListener(listener);
  }

  /** {@inheritDoc} */
  public final void removeSimpleSearchListener(final ActionListener listener) {
    evtSimpleSearchPerformed.removeListener(listener);
  }

  private void bindEvents() {
    for (final PropertySearchModel searchModel : propertySearchModels.values()) {
      searchModel.addSearchStateListener(new ActionListener() {
        /** {@inheritDoc} */
        public void actionPerformed(final ActionEvent e) {
          stSearchStateChanged.setActive(!rememberedSearchState.equals(getSearchModelState()));
          stSearchStateChanged.notifyObservers();
        }
      });
    }
  }

  private void updateSearchModels() {
    clearPropertySearchModels();
    if (!simpleSearchString.isEmpty()) {
      final String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
      final String searchTextWithWildcards = wildcard + simpleSearchString + wildcard;
      final Collection<Property.ColumnProperty> searchProperties = Entities.getSearchProperties(entityID);
      for (final Property searchProperty : searchProperties) {
        final PropertySearchModel propertySearchModel = getPropertySearchModel(searchProperty.getPropertyID());
        propertySearchModel.setCaseSensitive(false);
        propertySearchModel.setUpperBound(searchTextWithWildcards);
        propertySearchModel.setSearchType(SearchType.LIKE);
        propertySearchModel.setEnabled(true);
      }
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

  /**
   * @param property the property
   * @return true if the property is a column property and that column is the result of an aggregate function
   */
  private static boolean isAggregateColumnProperty(final Property property) {
    return property instanceof Property.ColumnProperty && ((Property.ColumnProperty) property).isAggregateColumn();
  }
}
