/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.SearchModel;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
  private final List<Property> properties;
  private Map<String, SearchModel<Property>> propertyFilterModels;
  private Map<String, PropertySearchModel<? extends Property.SearchableProperty>> propertySearchModels;
  /** When active the search should be simplified */
  private final boolean simpleSearch;
  private Criteria<Property.ColumnProperty> additionalSearchCriteria;
  private Conjunction searchConjunction = Conjunction.AND;
  private String searchStateOnRefresh;

  /**
   * Instantiates a new DefaultEntityTableSearchModel
   * @param entityID the ID of the underlying entity
   * @param dbProvider a EntityDbProvider instance, required if <code>searchableProperties</code> include
   * foreign key properties
   * assumed to belong to the entity identified by <code>entityID</code>
   * @param simpleSearch if true then search panels based on this search model should implement a simplified search
   */
  public DefaultEntityTableSearchModel(final String entityID, final EntityDbProvider dbProvider, final boolean simpleSearch) {
    this(entityID, dbProvider, new ArrayList<Property>(EntityRepository.getVisibleProperties(entityID)), simpleSearch);
  }

  /**
   * Instantiates a new DefaultEntityTableSearchModel
   * @param entityID the ID of the underlying entity
   * @param dbProvider a EntityDbProvider instance, required if <code>searchableProperties</code> include
   * foreign key properties
   * @param properties the underlying properties
   * assumed to belong to the entity identified by <code>entityID</code>
   * @param simpleSearch if true then search panels based on this search model should implement a simplified search
   */
  public DefaultEntityTableSearchModel(final String entityID, final EntityDbProvider dbProvider,
                                       final List<Property> properties, final boolean simpleSearch) {
    Util.rejectNullValue(entityID, "entityID");
    Util.rejectNullValue(properties, "properties");
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.properties = properties;
    this.searchStateOnRefresh = getSearchModelState();
    this.simpleSearch = simpleSearch;
    bindEvents();
  }

  public final String getEntityID() {
    return entityID;
  }

  public EntityDbProvider getDbProvider() {
    return dbProvider;
  }

  public final boolean isSimpleSearch() {
    return simpleSearch;
  }

  public final List<Property.SearchableProperty> getSearchableProperties() {
    final List<Property.SearchableProperty> searchProperties = new ArrayList<Property.SearchableProperty>();
    for (final Property property : properties) {
      if (property instanceof Property.SearchableProperty) {
        searchProperties.add((Property.SearchableProperty) property);
      }
    }

    return searchProperties;
  }

  public final void setSearchModelState() {
    searchStateOnRefresh = getSearchModelState();
    stSearchStateChanged.setActive(false);
  }

  public final SearchModel<Property> getPropertyFilterModel(final String propertyID) {
    initialize();
    if (propertyFilterModels.containsKey(propertyID)) {
      return propertyFilterModels.get(propertyID);
    }

    return null;
  }

  public final Collection<SearchModel<Property>> getPropertyFilterModels() {
    initialize();
    return Collections.unmodifiableCollection(propertyFilterModels.values());
  }

  public List<SearchModel<Property>> getPropertyFilterModelsOrdered() {
    final List<SearchModel<Property>> models = new ArrayList<SearchModel<Property>>(properties.size());
    for (final Property property : properties) {
      models.add(getPropertyFilterModel(property.getPropertyID()));
    }

    return models;
  }

  public final boolean include(final Entity item) {
    initialize();
    for (final SearchModel<Property> columnFilter : propertyFilterModels.values()) {
      if (columnFilter.isSearchEnabled() && !columnFilter.include(item)) {
        return false;
      }
    }

    return true;
  }

  public final void refresh() {
    initialize();
    for (final PropertySearchModel model : propertySearchModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).refresh();
      }
    }
  }

  public final void clear() {
    initialize();
    for (final PropertySearchModel model : propertySearchModels.values()) {
      if (model instanceof Refreshable) {
        ((Refreshable) model).clear();
      }
    }
  }

  public final void clearPropertySearchModels() {
    initialize();
    for (final PropertySearchModel searchModel : propertySearchModels.values()) {
      searchModel.clearSearch();
    }
  }

  public final Collection<PropertySearchModel<? extends Property.SearchableProperty>> getPropertySearchModels() {
    initialize();
    return Collections.unmodifiableCollection(propertySearchModels.values());
  }

  public final boolean containsPropertySearchModel(final String propertyID) {
    initialize();
    return propertySearchModels.containsKey(propertyID);
  }

  public final PropertySearchModel<? extends Property.SearchableProperty> getPropertySearchModel(final String propertyID) {
    initialize();
    if (propertySearchModels.containsKey(propertyID)) {
      return propertySearchModels.get(propertyID);
    }

    throw new RuntimeException("SearchModel not found for property with ID: " + propertyID);
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
    final SearchModel<Property> filterModel = getPropertyFilterModel(propertyID);
    if (filterModel != null) {
      filterModel.setLikeValue(value);
    }
  }

  public final Criteria<Property.ColumnProperty> getSearchCriteria() {
    initialize();
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

  /**
   * Initializes a PropertySearchModel for the given property
   * @param property the Property for which to create a PropertySearchModel
   * @param dbProvider the EntityDbProvider instance to use in case the property is a ForeignKeyProperty
   * @return a PropertySearchModel for the given property, null if this property is not searchable or if searching
   * should not be allowed for this property
   */
  protected PropertySearchModel<? extends Property.SearchableProperty> initializePropertySearchModel(
          final Property.SearchableProperty property, final EntityDbProvider dbProvider) {
    if (property instanceof Property.ForeignKeyProperty) {
      final Property.ForeignKeyProperty fkProperty = (Property.ForeignKeyProperty) property;
      if (EntityRepository.isLargeDataset(fkProperty.getReferencedEntityID())) {
        final EntityLookupModel lookupModel = new DefaultEntityLookupModel(fkProperty.getReferencedEntityID(),
                dbProvider, getSearchProperties(fkProperty.getReferencedEntityID()));
        lookupModel.setMultipleSelectionAllowed(true);
        return new DefaultForeignKeySearchModel(fkProperty, lookupModel);
      }
      else {
        final EntityComboBoxModel comboBoxModel = new DefaultEntityComboBoxModel(fkProperty.getReferencedEntityID(), dbProvider);
        comboBoxModel.setNullValueString("");
        return new DefaultForeignKeySearchModel(fkProperty, comboBoxModel);
      }
    }
    else if (property instanceof Property.ColumnProperty) {
      return new DefaultPropertySearchModel((Property.ColumnProperty) property);
    }

    throw new RuntimeException("Not a searchable property (Property.ColumnProperty or PropertyForeignKeyProperty): " + property);
  }

  /**
   * Initializes a PropertyFilterModel for the given property
   * @param property the Property for which to initialize a PropertyFilterModel
   * @return a PropertyFilterModel for the given property
   */
  protected SearchModel<Property> initializePropertyFilterModel(final Property property) {
    return new DefaultPropertyFilterModel(property);
  }

  private void initialize() {
    if (propertyFilterModels == null) {
      propertyFilterModels = initializePropertyFilterModels();
    }
    if (propertySearchModels == null) {
      propertySearchModels = initializePropertySearchModels(dbProvider);
    }
  }

  /**
   * @param dbProvider the EntityDbProvider to use for foreign key based fields, such as combo boxes
   * @return a map of PropertySearchModels mapped to their respective propertyIDs
   */
  private Map<String, PropertySearchModel<? extends Property.SearchableProperty>> initializePropertySearchModels(final EntityDbProvider dbProvider) {
    final Map<String, PropertySearchModel<? extends Property.SearchableProperty>> searchModels =
            new HashMap<String, PropertySearchModel<? extends Property.SearchableProperty>>();
    for (final Property.SearchableProperty property : getSearchableProperties()) {
      final PropertySearchModel<? extends Property.SearchableProperty> searchModel = initializePropertySearchModel(property, dbProvider);
      if (searchModel != null) {
        searchModels.put(property.getPropertyID(), searchModel);
      }
    }

    return searchModels;
  }

  /**
   * @return a map of PropertyFilterModels mapped to their respective propertyIDs
   */
  private Map<String, SearchModel<Property>> initializePropertyFilterModels() {
    final Map<String, SearchModel<Property>> filters = new HashMap<String, SearchModel<Property>>(properties.size());
    for (final Property property : properties) {
      final SearchModel<Property> filterModel = initializePropertyFilterModel(property);
      filterModel.addSearchStateListener(evtFilterStateChanged);
      filters.put(property.getPropertyID(), filterModel);
    }

    return filters;
  }

  private void bindEvents() {
    for (final PropertySearchModel searchModel : propertySearchModels.values()) {
      searchModel.addSearchStateListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          stSearchStateChanged.setActive(!searchStateOnRefresh.equals(getSearchModelState()));
          stSearchStateChanged.notifyObserver();
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

  private List<Property.ColumnProperty> getSearchProperties(final String entityID) {
    final Collection<String> searchPropertyIDs = EntityRepository.getEntitySearchPropertyIDs(entityID);

    return searchPropertyIDs == null ? getStringProperties(entityID) : EntityRepository.getSearchProperties(entityID, searchPropertyIDs);
  }

  private List<Property.ColumnProperty> getStringProperties(final String entityID) {
    final Collection<Property.ColumnProperty> databaseProperties = EntityRepository.getColumnProperties(entityID);
    final List<Property.ColumnProperty> stringProperties = new ArrayList<Property.ColumnProperty>();
    for (final Property.ColumnProperty property : databaseProperties) {
      if (property.isString()) {
        stringProperties.add(property);
      }
    }

    return stringProperties;
  }
}
