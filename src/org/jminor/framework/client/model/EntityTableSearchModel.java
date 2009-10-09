/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.Criteria;
import org.jminor.common.db.CriteriaSet;
import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

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
 * This class encapsulates filtering functionality, which refers to showing/hiding entities already available
 * in the a table model and searching functionality, which refers to configuring the underlying query,
 * which then needs to be re-run
 */
public class EntityTableSearchModel {

  /**
   * Fired when the state of a filter model changes
   */
  public final Event evtFilterStateChanged = new Event();

  /**
   * Activated each time the search state differs from the state at last reset
   * @see #setSearchModelState()
   */
  public final State stSearchStateChanged = new State();

  private final String entityID;
  private final List<Property> tableColumnProperties;
  private final List<PropertyFilterModel> propertyFilterModels;
  private final List<PropertySearchModel> propertySearchModels;
  private final Map<Property, EntityComboBoxModel> propertySearchComboBoxModels = new HashMap<Property, EntityComboBoxModel>();
  /** When active the search should be simplified */
  private final boolean simpleSearch;
  private CriteriaSet.Conjunction searchConjunction = CriteriaSet.Conjunction.AND;
  private String searchStateOnRefresh;

  /**
   * Instantiates a new EntityTableSearchModel
   * @param entityID the ID of the underlying entity
   * @param tableColumnProperties the properties constituting the table columns,
   * assumed to belong to the entity identified by <code>entityID</code>
   * @param searchableProperties properties that are searchable via the database, that is,
   * properties that map to database columns, assumed to belong to the entity identified by <code>entityID</code>
   * @param dbProvider a EntityDbProvider instance, required if <code>searchableProperties</code> include
   * foreign key properties
   * @param simpleSearch if true then search panels based on this search model should implement a simplified search
   */
  public EntityTableSearchModel(final String entityID, final List<Property> tableColumnProperties,
                                final List<Property> searchableProperties, final EntityDbProvider dbProvider,
                                final boolean simpleSearch) {
    if (entityID == null)
      throw new IllegalArgumentException("entityID must be specified");
    if (tableColumnProperties == null)
      throw new IllegalArgumentException("tableColumnProperties must be specified");
    if (searchableProperties == null)
      throw new IllegalArgumentException("searchableProperties must be specified");
    this.entityID = entityID;
    this.tableColumnProperties = tableColumnProperties;
    this.propertyFilterModels = initPropertyFilterModels();
    this.propertySearchModels = initPropertySearchModels(searchableProperties, dbProvider);
    this.searchStateOnRefresh = getSearchModelState();
    this.simpleSearch = simpleSearch;
  }

  public String getEntityID() {
    return entityID;
  }

  public boolean isSimpleSearch() {
    return simpleSearch;
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
   * Returns the property filter at <code>index</code>
   * @param index the property index
   * @return the property filter
   */
  public PropertyFilterModel getPropertyFilterModel(final int index) {
    return propertyFilterModels.get(index);
  }

  /**
   * The PropertyFilterModel associated with the property identified by <code>propertyID</code>
   * @param propertyID the id of the property for which to retrieve the PropertyFilterModel
   * @return the PropertyFilterModel for the property with id <code>propertyID</code>
   */
  public PropertyFilterModel getPropertyFilterModel(final String propertyID) {
    for (final AbstractSearchModel filter : propertyFilterModels)
      if (filter.getPropertyID().equals(propertyID))
        return (PropertyFilterModel) filter;

    return null;
  }

  /**
   * @return the property filters configured in this table search model
   */
  public List<PropertyFilterModel> getPropertyFilterModels() {
    return new ArrayList<PropertyFilterModel>(propertyFilterModels);
  }

  /**
   * @param entity the entity
   * @return true if the entity should be included or filtered (hidden)
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
    for (final EntityComboBoxModel model : propertySearchComboBoxModels.values())
      model.refresh();
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
   * @return a list containing the PropertySearchModels configured in this table search model
   */
  public List<PropertySearchModel> getPropertySearchModels() {
    return new ArrayList<PropertySearchModel>(propertySearchModels);
  }

  /**
   * @param propertyID the id of the property for which to retrieve the PropertySearchModel
   * @return the PropertySearchModel associated with the property identified by <code>propertyID</code>
   */
  public PropertySearchModel getPropertySearchModel(final String propertyID) {
    for (final PropertySearchModel searchModel : propertySearchModels)
      if (searchModel.getProperty().getPropertyID().equals(propertyID))
        return searchModel;

    return null;
  }

  /**
   * @param columnIndex the column index
   * @return true if the PropertySearchModel behind column with index <code>columnIndex</code> is enabled
   */
  public boolean isSearchEnabled(final int columnIndex) {
    final PropertySearchModel model = getPropertySearchModel(tableColumnProperties.get(columnIndex).getPropertyID());

    return model != null && model.isSearchEnabled();
  }

  /**
   * @param columnIndex the column index
   * @return true if the PropertyFilterModel behind column with index <code>columnIndex</code> is enabled
   */
  public boolean isFilterEnabled(final int columnIndex) {
    return getPropertyFilterModel(columnIndex).isSearchEnabled();
  }

  /**
   * Finds the PropertySearchModel associated with the ForeignKeyProperty representing
   * the entity identified by <code>referencedEntityID</code>, sets <code>referenceEntities</code>
   * as the search criteria value and enables the PropertySearchModel
   * @param referencedEntityID the ID of the entity
   * @param referenceEntities the entities to use as search criteria value
   * @return true if the search state changed as a result of this method call, false otherwise
   */
  public boolean setExactSearchValue(final String referencedEntityID, final List<Entity> referenceEntities) {
    final String searchState = getSearchModelState();
    for (final Property property : tableColumnProperties) {
      if (property instanceof Property.ForeignKeyProperty && ((Property.ForeignKeyProperty)property).getReferencedEntityID().equals(referencedEntityID)) {
        final PropertySearchModel searchModel = getPropertySearchModel(property.getPropertyID());
        if (searchModel != null) {
          searchModel.initialize();
          searchModel.setSearchEnabled(referenceEntities != null && referenceEntities.size() > 0);
          searchModel.setUpperBound((Object) null);//because the upperBound is a reference to the active entity and changes accordingly
          searchModel.setUpperBound(referenceEntities != null && referenceEntities.size() == 0 ? null : referenceEntities);//this then fails to register a changed upper bound
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
   * @return the current criteria based on the state of the search models
   */
  public Criteria getSearchCriteria() {
    final CriteriaSet ret = new CriteriaSet(getSearchCriteriaConjunction());
    for (final AbstractSearchModel criteria : propertySearchModels)
      if (criteria.isSearchEnabled())
        ret.addCriteria(((PropertySearchModel) criteria).getPropertyCriteria());

    return ret.getCriteriaCount() > 0 ? ret : null;
  }

  /**
   * @return the conjunction to be used when more than one column search criteria is active,
   * the default is <code>CriteriaSet.Conjunction.AND</code>
   * @see org.jminor.common.db.CriteriaSet.Conjunction
   */
  public CriteriaSet.Conjunction getSearchCriteriaConjunction() {
    return searchConjunction;
  }

  /**
   * @param searchConjunction the conjunction to be used when more than one column search criteria is active
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
   * @param dbProvider the EntityDbProvider to use for foreign key based fields, such as combo boxes
   * @return a list of PropertySearchModels initialized according to the properties in <code>properties</code>
   */
  private List<PropertySearchModel> initPropertySearchModels(final List<Property> properties, final EntityDbProvider dbProvider) {
    final List<PropertySearchModel> ret = new ArrayList<PropertySearchModel>();
    for (final Property property : properties) {
      PropertySearchModel searchModel;
      if (property instanceof Property.ForeignKeyProperty) {
        if (EntityRepository.isLargeDataset(((Property.ForeignKeyProperty) property).getReferencedEntityID())) {
          final EntityLookupModel lookupModel = new EntityLookupModel(((Property.ForeignKeyProperty) property).getReferencedEntityID(),
                  dbProvider, getSearchProperties(((Property.ForeignKeyProperty) property).getReferencedEntityID()));
          lookupModel.setMultipleSelectionAllowed(true);
          searchModel = new PropertySearchModel(property, lookupModel);
        }
        else {
          propertySearchComboBoxModels.put(property, new EntityComboBoxModel(((Property.ForeignKeyProperty) property).getReferencedEntityID(),
                  dbProvider, false, "", true));
          searchModel = new PropertySearchModel(property, propertySearchComboBoxModels.get(property));
        }
      }
      else {
        searchModel = new PropertySearchModel(property);
      }
      searchModel.evtSearchStateChanged.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent event) {
          stSearchStateChanged.setActive(!searchStateOnRefresh.equals(getSearchModelState()));
        }
      });
      ret.add(searchModel);
    }

    return ret;
  }

  /**
   * @return a list of PropertyFilterModels initialized according to the model
   */
  private List<PropertyFilterModel> initPropertyFilterModels() {
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
    final StringBuilder ret = new StringBuilder();
    for (final PropertySearchModel model : getPropertySearchModels())
      ret.append(model.toString());

    return ret.toString();
  }

  private List<Property> getSearchProperties(final String entityID) {
    final String[] searchPropertyIDs = EntityRepository.getEntitySearchPropertyIDs(entityID);

    return searchPropertyIDs == null ? getStringProperties(entityID) : EntityRepository.getProperties(entityID, searchPropertyIDs);
  }

  private List<Property> getStringProperties(final String entityID) {
    final Collection<Property> properties = EntityRepository.getDatabaseProperties(entityID);
    final List<Property> ret = new ArrayList<Property>();
    for (final Property property : properties)
      if (property.getPropertyType() == Type.STRING)
        ret.add(property);

    return ret;
  }
}
