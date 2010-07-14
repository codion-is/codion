/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.AbstractSearchModel;
import org.jminor.common.model.Event;
import org.jminor.common.model.State;
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
 * This class encapsulates filtering functionality, which refers to showing/hiding entities already available
 * in a table model and searching functionality, which refers to configuring the underlying query,
 * which then needs to be re-run.<br>
 * User: Bjorn Darri<br>
 * Date: 24.7.2008<br>
 * Time: 21:29:55<br>
 */
public class DefaultEntityTableSearchModel implements EntityTableSearchModel {

  private final Event evtFilterStateChanged = new Event();
  private final State stSearchStateChanged = new State();

  private final String entityID;
  private final List<Property> properties;
  private final Map<String, PropertyFilterModel> propertyFilterModels;
  private final Map<String, PropertySearchModel> propertySearchModels;
  /** When active the search should be simplified */
  private final boolean simpleSearch;
  private CriteriaSet.Conjunction searchConjunction = CriteriaSet.Conjunction.AND;
  private String searchStateOnRefresh;

  /**
   * Instantiates a new EntityTableSearchModel
   * @param entityID the ID of the underlying entity
   * @param dbProvider a EntityDbProvider instance, required if <code>searchableProperties</code> include
   * foreign key properties
   * @param properties the underlying properties
   * assumed to belong to the entity identified by <code>entityID</code>
   * @param simpleSearch if true then search panels based on this search model should implement a simplified search
   */
  public DefaultEntityTableSearchModel(final String entityID, final EntityDbProvider dbProvider,
                                final List<Property> properties, final boolean simpleSearch) {
    Util.rejectNullValue(entityID);
    Util.rejectNullValue(properties);
    this.entityID = entityID;
    this.properties = properties;
    this.propertyFilterModels = initializePropertyFilterModels();
    this.propertySearchModels = initializePropertySearchModels(dbProvider);
    this.searchStateOnRefresh = getSearchModelState();
    this.simpleSearch = simpleSearch;
    bindEvents();
  }

  public String getEntityID() {
    return entityID;
  }

  public boolean isSimpleSearch() {
    return simpleSearch;
  }

  public List<Property> getProperties() {
    return Collections.unmodifiableList(properties);
  }

  public void setSearchModelState() {
    searchStateOnRefresh = getSearchModelState();
    stSearchStateChanged.setActive(false);
  }

  public PropertyFilterModel getPropertyFilterModel(final String propertyID) {
    if (propertyFilterModels.containsKey(propertyID)) {
      return propertyFilterModels.get(propertyID);
    }

    return null;
  }

  public Collection<PropertyFilterModel> getPropertyFilterModels() {
    return Collections.unmodifiableCollection(propertyFilterModels.values());
  }

  /**
   * @param item the entity
   * @return true if the entity should be included or filtered
   */
  public boolean include(final Entity item) {
    for (final AbstractSearchModel columnFilter : propertyFilterModels.values()) {
      if (columnFilter.isSearchEnabled() && !columnFilter.include(item)) {
        return false;
      }
    }

    return true;
  }

  public void refreshSearchComboBoxModels() {
    for (final PropertySearchModel model : propertySearchModels.values()) {
      if (model.getEntityComboBoxModel() != null) {
        model.getEntityComboBoxModel().refresh();
      }
    }
  }

  public void clearSearchComboBoxModels() {
    for (final PropertySearchModel model : propertySearchModels.values()) {
      if (model.getEntityComboBoxModel() != null) {
        model.getEntityComboBoxModel().clear();
      }
    }
  }

  public void clearPropertySearchModels() {
    for (final AbstractSearchModel searchModel : propertySearchModels.values()) {
      searchModel.clear();
    }
  }

  public Collection<PropertySearchModel> getPropertySearchModels() {
    return Collections.unmodifiableCollection(propertySearchModels.values());
  }

  public boolean containsPropertySearchModel(final String propertyID) {
    return propertySearchModels.containsKey(propertyID);
  }

  public PropertySearchModel getPropertySearchModel(final String propertyID) {
    if (propertySearchModels.containsKey(propertyID)) {
      return propertySearchModels.get(propertyID);
    }

    throw new RuntimeException("PropertySearchModel not found for property with ID: " + propertyID);
  }

  public boolean isSearchEnabled(final String propertyID) {
    return containsPropertySearchModel(propertyID) && getPropertySearchModel(propertyID).isSearchEnabled();
  }

  public boolean isFilterEnabled(final String propertyID) {
    return getPropertyFilterModel(propertyID).isSearchEnabled();
  }

  public boolean setSearchValues(final String propertyID, final Collection<?> values) {
    final String searchState = getSearchModelState();
    if (containsPropertySearchModel(propertyID)) {
      final PropertySearchModel searchModel = getPropertySearchModel(propertyID);
      searchModel.initialize();
      searchModel.setSearchEnabled(values != null && values.size() > 0);
      searchModel.setUpperBound((Object) null);//because the upperBound could be a reference to the active entity which changes accordingly
      searchModel.setUpperBound(values != null && values.size() == 0 ? null : values);//this then fails to register a changed upper bound
    }
    return !searchState.equals(getSearchModelState());
  }

  /**
   * Sets the criteria value of the PropertyFilterModel associated with the property identified by <code>propertyID</code>.
   * @param propertyID the id of the property
   * @param value the criteria value
   */
  public void setFilterValue(final String propertyID, final Comparable value) {
    final PropertyFilterModel filterModel = getPropertyFilterModel(propertyID);
    if (filterModel != null) {
      filterModel.setLikeValue(value);
    }
  }

  public Criteria<Property> getSearchCriteria() {
    final CriteriaSet<Property> criteriaSet = new CriteriaSet<Property>(searchConjunction);
    for (final AbstractSearchModel criteria : propertySearchModels.values()) {
      if (criteria.isSearchEnabled()) {
        criteriaSet.addCriteria(((PropertySearchModel) criteria).getPropertyCriteria());
      }
    }

    return criteriaSet.getCriteriaCount() > 0 ? criteriaSet : null;
  }

  public CriteriaSet.Conjunction getSearchConjunction() {
    return searchConjunction;
  }

  public void setSearchConjunction(final CriteriaSet.Conjunction conjunction) {
    this.searchConjunction = conjunction;
  }

  public void setSearchEnabled(final String propertyID, final boolean enabled) {
    if (containsPropertySearchModel(propertyID)) {
      getPropertySearchModel(propertyID).setSearchEnabled(enabled);
    }
  }

  public State stateSearchStateChanged() {
    return stSearchStateChanged.getLinkedState();
  }

  public Event eventFilterStateChanged() {
    return evtFilterStateChanged;
  }

  /**
   * @param dbProvider the EntityDbProvider to use for foreign key based fields, such as combo boxes
   * @return a map of PropertySearchModels mapped to their respective propertyIDs
   */
  protected Map<String, PropertySearchModel> initializePropertySearchModels(final EntityDbProvider dbProvider) {
    final Map<String, PropertySearchModel> searchModels = new HashMap<String, PropertySearchModel>();
    for (final Property property : properties) {
      final PropertySearchModel searchModel = initializePropertySearchModel(property, dbProvider);
      if (searchModel != null) {
        searchModels.put(property.getPropertyID(), searchModel);
      }
    }

    return searchModels;
  }

  /**
   * Initializes a PropertySearchModel for the given property
   * @param property the Property for which to create a PropertySearchModel
   * @param dbProvider the EntityDbProvider instance to use in case the property is a ForeignKeyProperty
   * @return a PropertySearchModel for the given property, null if this property is not searchable or if searching
   * should not be allowed for this property
   * @see org.jminor.framework.domain.Property#isSearchable()
   */
  protected PropertySearchModel initializePropertySearchModel(final Property property, final EntityDbProvider dbProvider) {
    if (!property.isSearchable()) {
      return null;
    }

    if (property instanceof Property.ForeignKeyProperty) {
      if (EntityRepository.isLargeDataset(((Property.ForeignKeyProperty) property).getReferencedEntityID())) {
        final EntityLookupModel lookupModel = new DefaultEntityLookupModel(((Property.ForeignKeyProperty) property).getReferencedEntityID(),
                dbProvider, getSearchProperties(((Property.ForeignKeyProperty) property).getReferencedEntityID()));
        lookupModel.setMultipleSelectionAllowed(true);
        return new PropertySearchModel(property, lookupModel);
      }
      else {
        final EntityComboBoxModel comboBoxModel = new DefaultEntityComboBoxModel(((Property.ForeignKeyProperty)
                property).getReferencedEntityID(), dbProvider);
        comboBoxModel.setNullValueString("");
        return new PropertySearchModel(property, comboBoxModel);
      }
    }
    else {
      return new PropertySearchModel(property);
    }
  }

  /**
   * @return a map of PropertyFilterModels mapped to their respective propertyIDs
   */
  protected Map<String, PropertyFilterModel> initializePropertyFilterModels() {
    final Map<String, PropertyFilterModel> filters = new HashMap<String, PropertyFilterModel>(properties.size());
    for (final Property property : properties) {
      final PropertyFilterModel filterModel = initializePropertyFilterModel(property);
      filterModel.eventSearchStateChanged().addListener(evtFilterStateChanged);
      filters.put(property.getPropertyID(), filterModel);
    }

    return filters;
  }

  /**
   * Initializes a PropertyFilterModel for the given property
   * @param property the Property for which to initialize a PropertyFilterModel
   * @return a PropertyFilterModel for the given property
   */
  protected PropertyFilterModel initializePropertyFilterModel(final Property property) {
    return new PropertyFilterModel(property);
  }

  private void bindEvents() {
    for (final PropertySearchModel searchModel : propertySearchModels.values()) {
      searchModel.eventSearchStateChanged().addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          stSearchStateChanged.setActive(!searchStateOnRefresh.equals(getSearchModelState()));
          stSearchStateChanged.eventStateChanged().fire();
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

  private List<Property> getSearchProperties(final String entityID) {
    final Collection<String> searchPropertyIDs = EntityRepository.getEntitySearchPropertyIDs(entityID);

    return searchPropertyIDs == null ? getStringProperties(entityID) : EntityRepository.getProperties(entityID, searchPropertyIDs);
  }

  private List<Property> getStringProperties(final String entityID) {
    final Collection<Property> databaseProperties = EntityRepository.getDatabaseProperties(entityID);
    final List<Property> stringProperties = new ArrayList<Property>();
    for (final Property property : databaseProperties) {
      if (property.isString()) {
        stringProperties.add(property);
      }
    }

    return stringProperties;
  }
}
