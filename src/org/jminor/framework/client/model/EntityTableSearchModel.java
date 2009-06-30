/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.CriteriaSet;
import org.jminor.common.db.ICriteria;
import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Björn Darri
 * Date: 24.7.2008
 * Time: 21:29:55
 *
 * This class encapsulates both filtering and searching facilities
 */
public class EntityTableSearchModel {

  /**
   * Fired when the state of the filter models changes
   */
  public final Event evtFilterStateChanged = new Event();

  /**
   * When active the search should be simplified
   */
  public final State stSimpleSearch = new State("EntityTableSearchModel.stSimpleSearch");

  /**
   * Activated each time the search state differs from the state at last reset
   * @see #setSearchModelState()
   */
  public final State stSearchStateChanged = new State("EntityTableSearchModel.stSearchStateChanged");

  private final String entityID;
  private final List<Property> visibleProperties;
  private final List<PropertyFilterModel> propertyFilterModels;
  private final List<PropertySearchModel> propertySearchModels;
  private final Map<Property, EntityComboBoxModel> propertySearchComboBoxModels = new HashMap<Property, EntityComboBoxModel>();
  private CriteriaSet.Conjunction searchConjunction = CriteriaSet.Conjunction.AND;
  private String searchStateOnRefresh;

  public EntityTableSearchModel(final String entityID, final List<Property> tableColumnProperties,
                                final List<Property> searchableProperties, final IEntityDbProvider dbProvider,
                                final List<Property> visibleProperties) {
    this.entityID = entityID;
    this.visibleProperties = visibleProperties;
    this.propertyFilterModels = initPropertyFilterModels(tableColumnProperties);
    this.propertySearchModels = initPropertySearchModels(searchableProperties, dbProvider);
    this.searchStateOnRefresh = getSearchModelState();
  }

  public String getEntityID() {
    return entityID;
  }

  /**
   * Sets the current search model state
   * @see #stSearchStateChanged
   */
  public void setSearchModelState() {
    searchStateOnRefresh = getSearchModelState();
    stSearchStateChanged.setActive(false);
  }

  /**
   * Returns the property filter at index <code>idx</code>
   * @param idx the property index
   * @return the property filter
   */
  public PropertyFilterModel getPropertyFilterModel(final int idx) {
    return propertyFilterModels.get(idx);
  }

  /**
   * The PropertyFilterModel associated with the property identified by <code>propertyID</code>
   * @param propertyID the id of the property for which to retrieve the PropertyFilterModel
   * @return the PropertyFilterModel for the property with id <code>propertyID</code>
   */
  public PropertyFilterModel getPropertyFilterModel(final String propertyID) {
    for (final AbstractSearchModel filter : propertyFilterModels)
      if (filter.getPropertyName().equals(propertyID))
        return (PropertyFilterModel) filter;

    return null;
  }

  /**
   * @return the property filters this table model uses
   */
  public List<PropertyFilterModel> getPropertyFilterModels() {
    return propertyFilterModels;
  }

  /**
   * @param entity the entity
   * @return true if the entity should be included in this table model or filtered (hidden)
   */
  public boolean include(final Entity entity) {
    for (final AbstractSearchModel columnFilter : propertyFilterModels)
      if (columnFilter.isSearchEnabled() && !columnFilter.include(entity))
        return false;

    return true;
  }

  /**
   * Refreshes all combo box models associated with PropertySearchModels
   */
  public void refreshSearchComboBoxModels() {
    try {
      for (final EntityComboBoxModel model : propertySearchComboBoxModels.values())
        model.refresh();
    }
    catch (UserException e) {
      throw e.getRuntimeException();
    }
  }

  /**
   * Clears the contents from all combo box models associated with PropertySearchModels
   */
  public void clearSearchComboBoxModels() {
    for (final EntityComboBoxModel model : propertySearchComboBoxModels.values())
      model.clear();
  }

  /**
   * Clears the state of all PropertySearchModels
   */
  public void clearPropertySearchModels() {
    for (final AbstractSearchModel searchModel : propertySearchModels)
      searchModel.clear();
  }

  /**
   * @return a list containing the PropertySearchModels found in this table model
   */
  public List<PropertySearchModel> getPropertySearchModels() {
    return propertySearchModels;
  }

  /**
   * @param propertyID the id of the property for which to retrieve the PropertySearchModel
   * @return the PropertySearchModel associated with the property identified by <code>propertyID</code>
   */
  public PropertySearchModel getPropertySearchModel(final String propertyID) {
    for (final PropertySearchModel searchModel : propertySearchModels)
      if (searchModel.getProperty().propertyID.equals(propertyID))
        return searchModel;

    return null;
  }

  /**
   * @param columnIdx the column index
   * @return true if the PropertySearchModel behind column with index <code>columnIdx</code> is enabled
   */
  public boolean isSearchEnabled(final int columnIdx) {
    final PropertySearchModel model = getPropertySearchModel(visibleProperties.get(columnIdx).propertyID);

    return model != null && model.isSearchEnabled();
  }

  /**
   * @param columnIndex the column index
   * @return true if the PropertyFilterModel behind column with index <code>columnIdx</code> is enabled
   */
  public boolean isFilterEnabled(final int columnIndex) {
    return getPropertyFilterModel(columnIndex).isSearchEnabled();
  }

  /**
   * Finds the PropertySearchModel associated with the EntityProperty representing
   * the entity identified by <code>referencedEntityID</code>, sets <code>referenceEntities</code>
   * as the search criteria value and enables the PropertySearchModel
   * @param referencedEntityID the ID of the entity
   * @param referenceEntities the entities to use as search criteria value
   * @return true if the search state changed as a result of this method call, false otherwise
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  public boolean setExactSearchValue(final String referencedEntityID, final List<Entity> referenceEntities) throws UserException {
    final String searchState = getSearchModelState();
    for (final Property property : visibleProperties) {
      if (property instanceof Property.EntityProperty && ((Property.EntityProperty)property).referenceEntityID.equals(referencedEntityID)) {
        final PropertySearchModel searchModel = getPropertySearchModel(property.propertyID);
        if (searchModel != null) {
          searchModel.initialize();
          searchModel.setSearchEnabled(referenceEntities != null && referenceEntities.size() > 0);
          searchModel.setUpperBound((Object) null);//because the upperBound is a reference to the active entity and changes accordingly
          searchModel.setUpperBound(referenceEntities != null && referenceEntities.size() == 0 ? null : referenceEntities);//this then failes to register a changed upper bound
        }
      }
    }
    return !searchState.equals(getSearchModelState());
  }

  /**
   * Sets the criteria value of the PropertyFilterModel behind the column at <code>columnIndex</code>
   * @param value the criteria value
   * @param columnIndex the index of the column
   */
  public void setExactFilterValue(final Comparable value, final int columnIndex) {
    if (columnIndex >= 0)
      getPropertyFilterModel(columnIndex).setLikeValue(value);
  }

  /**
   * @return the current criteria
   */
  public ICriteria getSearchCriteria() {
    final CriteriaSet ret = new CriteriaSet(getSearchCriteriaConjunction());
    for (final AbstractSearchModel criteria : propertySearchModels)
      if (criteria.isSearchEnabled())
        ret.addCriteria(((PropertySearchModel) criteria).getPropertyCriteria());

    return ret.getCriteriaCount() > 0 ? ret : null;
  }

  /**
   * @return the conjuction to be used when more than one search criteria is specified
   * @see org.jminor.common.db.CriteriaSet.Conjunction
   */
  public CriteriaSet.Conjunction getSearchCriteriaConjunction() {
    return searchConjunction;
  }

  /**
   * @param searchConjunction the conjuction to be used when more than one search criteria is specified
   * @see org.jminor.common.db.CriteriaSet.Conjunction
   */
  public void setSearchConjunction(final CriteriaSet.Conjunction searchConjunction) {
    this.searchConjunction = searchConjunction;
  }

  /**
   * Enables/disables the search for the given property
   * @param propertyID the ID of the property for which to enable/disable the search
   * @param value if true the search is enabled, otherwise it is disabled
   */
  public void setSearchEnabled(final String propertyID, final boolean value) {
    getPropertySearchModel(propertyID).setSearchEnabled(value);
  }

  /**
   * @param properties the properties for which to initialize PropertySearchModels
   * @param dbProvider the IEntityDbProvider to use for entity based fields, such as combo boxes
   * @return a list of PropertySearchModels initialized according to the properties in <code>properties</code>
   */
  private List<PropertySearchModel> initPropertySearchModels(final List<Property> properties, final IEntityDbProvider dbProvider) {
    final List<PropertySearchModel> ret = new ArrayList<PropertySearchModel>();
    for (final Property property : properties) {
      PropertySearchModel searchModel;
      if (property instanceof Property.EntityProperty) {
        if (EntityRepository.get().isLargeDataset(((Property.EntityProperty) property).referenceEntityID)) {
          final EntityLookupModel lookupModel = new EntityLookupModel(((Property.EntityProperty) property).referenceEntityID,
                  dbProvider, getSearchProperties(((Property.EntityProperty) property).referenceEntityID));
          lookupModel.setMultipleSelectionAllowed(true);
          searchModel = new PropertySearchModel(property, lookupModel);
        }
        else {
          propertySearchComboBoxModels.put(property, new EntityComboBoxModel(((Property.EntityProperty) property).referenceEntityID,
                  dbProvider, false, "", true));
          searchModel = new PropertySearchModel(property, propertySearchComboBoxModels.get(property));
        }
      }
      else {
        searchModel = new PropertySearchModel(property);
      }
      searchModel.evtSearchStateChanged.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          stSearchStateChanged.setActive(!searchStateOnRefresh.equals(getSearchModelState()));
        }
      });
      ret.add(searchModel);
    }

    return ret;
  }

  /**
   * @param tableColumnProperties the properties for which to initialize PropertyFilterModels
   * @return a list of PropertyFilterModels initialized according to the model
   */
  private List<PropertyFilterModel> initPropertyFilterModels(final List<Property> tableColumnProperties) {
    final List<PropertyFilterModel> filters = new ArrayList<PropertyFilterModel>(tableColumnProperties.size());
    int i = 0;
    for (final Property property : tableColumnProperties) {
      final PropertyFilterModel filterModel = new PropertyFilterModel(property, i++);
      filterModel.evtSearchStateChanged.addListener(evtFilterStateChanged);
      filters.add(filterModel);
    }

    return filters;
  }

  /**
   * @return a String representing the current state of the search models
   */
  private String getSearchModelState() {
    final StringBuffer ret = new StringBuffer();
    for (final AbstractSearchModel model : getPropertySearchModels())
      ret.append(model.toString());

    return ret.toString();
  }

  private List<Property> getSearchProperties(final String entityID) {
    final String[] searchPropertyIDs = EntityRepository.get().getEntitySearchPropertyIDs(entityID);

    return searchPropertyIDs == null ? getStringProperties(entityID) : EntityRepository.get().getProperties(entityID, searchPropertyIDs);
  }

  private List<Property> getStringProperties(final String entityID) {
    final Collection<Property> properties = EntityRepository.get().getDatabaseProperties(entityID);
    final List<Property> ret = new ArrayList<Property>();
    for (final Property property : properties)
      if (property.propertyType == Type.STRING)
        ret.add(property);

    return ret;
  }
}
