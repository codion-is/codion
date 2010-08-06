/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.ColumnSearchModel;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.framework.db.provider.EntityDbProvider;
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

  private final Event evtFilterStateChanged = Events.event();
  private final State stSearchStateChanged = States.state();

  private final String entityID;
  private final EntityDbProvider dbProvider;
  private Map<String, ColumnSearchModel<Property>> propertyFilterModels = new LinkedHashMap<String, ColumnSearchModel<Property>>();
  private Map<String, PropertySearchModel<? extends Property.SearchableProperty>> propertySearchModels = new HashMap<String, PropertySearchModel<? extends Property.SearchableProperty>>();
  /** When active the search should be simplified */
  private final boolean simpleSearch;
  private Criteria<Property.ColumnProperty> additionalSearchCriteria;
  private Conjunction searchConjunction = Conjunction.AND;
  private String searchStateOnRefresh = "";

  /**
   * Instantiates a new DefaultEntityTableSearchModel
   * @param entityID the ID of the underlying entity
   * @param dbProvider a EntityDbProvider instance, required if <code>searchableProperties</code> include
   * foreign key properties
   * assumed to belong to the entity identified by <code>entityID</code>
   * @param simpleSearch if true then search panels based on this search model should implement a simplified search
   */
  public DefaultEntityTableSearchModel(final String entityID, final EntityDbProvider dbProvider, final boolean simpleSearch) {
    this(entityID, dbProvider, simpleSearch, new DefaultPropertyFilterModelProvider(), new DefaultPropertySearchModelProvider());
  }

  /**
   * Instantiates a new DefaultEntityTableSearchModel
   * @param entityID the ID of the underlying entity
   * @param dbProvider a EntityDbProvider instance, required if <code>searchableProperties</code> include
   * foreign key properties
   * assumed to belong to the entity identified by <code>entityID</code>
   * @param simpleSearch if true then search panels based on this search model should implement a simplified search
   * @param filterModelProvider provides the column filter models for this table search model
   * @param searchModelProvider provides the column search models for this table search model
   */
  public DefaultEntityTableSearchModel(final String entityID, final EntityDbProvider dbProvider, final boolean simpleSearch,
                                       final PropertyFilterModelProvider filterModelProvider,
                                       final PropertySearchModelProvider searchModelProvider) {
    Util.rejectNullValue(entityID, entityID);
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    for (final Property property : Entities.getVisibleProperties(entityID)) {
      final ColumnSearchModel<Property> filterModel = filterModelProvider.initializePropertyFilterModel(property);
      this.propertyFilterModels.put(filterModel.getColumnIdentifier().getPropertyID(), filterModel);
      if (property instanceof Property.SearchableProperty) {
        final PropertySearchModel<? extends Property.SearchableProperty> searchModel =
                searchModelProvider.initializePropertySearchModel((Property.SearchableProperty) property, dbProvider);
        if (searchModel != null) {
          this.propertySearchModels.put(searchModel.getColumnIdentifier().getPropertyID(), searchModel);
        }
      }
    }
    this.simpleSearch = simpleSearch;
    this.searchStateOnRefresh = getSearchModelState();
    bindEvents();
  }

  public final String getEntityID() {
    return entityID;
  }

  public final EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  public final boolean isSimpleSearch() {
    return simpleSearch;
  }

  public final List<Property.SearchableProperty> getSearchableProperties() {
    final List<Property.SearchableProperty> searchProperties = new ArrayList<Property.SearchableProperty>();
    for (final PropertySearchModel<? extends Property.SearchableProperty> searchModel : propertySearchModels.values()) {
      searchProperties.add(searchModel.getColumnIdentifier());
    }

    return searchProperties;
  }

  public final void setSearchModelState() {
    searchStateOnRefresh = getSearchModelState();
    stSearchStateChanged.setActive(false);
  }

  public final ColumnSearchModel<Property> getPropertyFilterModel(final String propertyID) {
    if (propertyFilterModels.containsKey(propertyID)) {
      return propertyFilterModels.get(propertyID);
    }

    return null;
  }

  public final Collection<ColumnSearchModel<Property>> getPropertyFilterModels() {
    return Collections.unmodifiableCollection(propertyFilterModels.values());
  }

  public final List<ColumnSearchModel<Property>> getPropertyFilterModelsOrdered() {
    return new ArrayList<ColumnSearchModel<Property>>(propertyFilterModels.values());
  }

  public final boolean include(final Entity item) {
    for (final ColumnSearchModel<Property> columnFilter : propertyFilterModels.values()) {
      if (columnFilter.isSearchEnabled() && !columnFilter.include(item)) {
        return false;
      }
    }

    return true;
  }

  public final void refresh() {
    for (final PropertySearchModel model : propertySearchModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).refresh();
      }
    }
  }

  public final void clear() {
    for (final PropertySearchModel model : propertySearchModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).clear();
      }
    }
  }

  public final void clearPropertySearchModels() {
    for (final PropertySearchModel searchModel : propertySearchModels.values()) {
      searchModel.clearSearch();
    }
  }

  public final Collection<PropertySearchModel<? extends Property.SearchableProperty>> getPropertySearchModels() {
    return Collections.unmodifiableCollection(propertySearchModels.values());
  }

  public final boolean containsPropertySearchModel(final String propertyID) {
    return propertySearchModels.containsKey(propertyID);
  }

  public final PropertySearchModel<? extends Property.SearchableProperty> getPropertySearchModel(final String propertyID) {
    if (propertySearchModels.containsKey(propertyID)) {
      return propertySearchModels.get(propertyID);
    }

    throw new RuntimeException("ColumnSearchModel not found for property with ID: " + propertyID);
  }

  public final boolean isSearchEnabled(final String propertyID) {
    return containsPropertySearchModel(propertyID) && getPropertySearchModel(propertyID).isSearchEnabled();
  }

  public final boolean isFilterEnabled(final String propertyID) {
    return getPropertyFilterModel(propertyID).isSearchEnabled();
  }

  public final boolean setSearchValues(final String propertyID, final Collection<?> values) {
    final String searchState = getSearchModelState();
    if (containsPropertySearchModel(propertyID)) {
      final PropertySearchModel searchModel = getPropertySearchModel(propertyID);
      searchModel.setSearchEnabled(values != null && !values.isEmpty());
      searchModel.setUpperBound((Object) null);//because the upperBound could be a reference to the active entity which changes accordingly
      searchModel.setUpperBound(values != null && values.isEmpty() ? null : values);//this then fails to register a changed upper bound
    }
    return !searchState.equals(getSearchModelState());
  }

  public final void setFilterValue(final String propertyID, final Comparable value) {
    final ColumnSearchModel<Property> filterModel = getPropertyFilterModel(propertyID);
    if (filterModel != null) {
      filterModel.setLikeValue(value);
    }
  }

  public final Criteria<Property.ColumnProperty> getSearchCriteria() {
    final CriteriaSet<Property.ColumnProperty> criteriaSet = new CriteriaSet<Property.ColumnProperty>(searchConjunction);
    for (final PropertySearchModel<? extends Property.SearchableProperty> criteria : propertySearchModels.values()) {
      if (criteria.isSearchEnabled()) {
        criteriaSet.add(criteria.getCriteria());
      }
    }
    if (additionalSearchCriteria != null) {
      criteriaSet.add(additionalSearchCriteria);
    }

    return criteriaSet.getCriteriaCount() > 0 ? criteriaSet : null;
  }

  public final Criteria<Property.ColumnProperty> getAdditionalSearchCriteria() {
    return additionalSearchCriteria;
  }

  public final EntityTableSearchModel setAdditionalSearchCriteria(final Criteria<Property.ColumnProperty> criteria) {
    this.additionalSearchCriteria = criteria;
    return this;
  }

  public final Conjunction getSearchConjunction() {
    return searchConjunction;
  }

  public final void setSearchConjunction(final Conjunction conjunction) {
    this.searchConjunction = conjunction;
  }

  public final void setSearchEnabled(final String propertyID, final boolean enabled) {
    if (containsPropertySearchModel(propertyID)) {
      getPropertySearchModel(propertyID).setSearchEnabled(enabled);
    }
  }

  public final StateObserver getSearchStateChangedState() {
    return stSearchStateChanged.getObserver();
  }

  public final void addFilterStateListener(final ActionListener listener) {
    evtFilterStateChanged.addListener(listener);
  }

  public final void removeFilterStateListener(final ActionListener listener) {
    evtFilterStateChanged.removeListener(listener);
  }

  private void bindEvents() {
    for (final PropertySearchModel searchModel : propertySearchModels.values()) {
      searchModel.addSearchStateListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          stSearchStateChanged.setActive(!searchStateOnRefresh.equals(getSearchModelState()));
          stSearchStateChanged.notifyObservers();
        }
      });
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
}
