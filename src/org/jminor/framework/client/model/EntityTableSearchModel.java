/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.Event;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.State;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
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
public class EntityTableSearchModel implements FilterCriteria<Entity> {

  private final Event evtFilterStateChanged = new Event();
  private final State stSearchStateChanged = new State();

  private final String entityID;
  private final TableColumnModel columnModel;
  private final Map<String, PropertyFilterModel> propertyFilterModels;
  private final Map<String, PropertySearchModel> propertySearchModels;
  /** When active the search should be simplified */
  private final boolean simpleSearch;
  private CriteriaSet.Conjunction searchConjunction = CriteriaSet.Conjunction.AND;
  private String searchStateOnRefresh;

  /**
   * Instantiates a new EntityTableSearchModel
   * @param entityID the ID of the underlying entity
   * @param columnModel the underlying TableColumnModel
   * assumed to belong to the entity identified by <code>entityID</code>
   * @param dbProvider a EntityDbProvider instance, required if <code>searchableProperties</code> include
   * foreign key properties
   * @param simpleSearch if true then search panels based on this search model should implement a simplified search
   */
  public EntityTableSearchModel(final String entityID, final TableColumnModel columnModel,
                                final EntityDbProvider dbProvider, final boolean simpleSearch) {
    if (entityID == null)
      throw new IllegalArgumentException("entityID must be specified");
    if (columnModel == null)
      throw new IllegalArgumentException("tableColumnModel must be specified");
    this.entityID = entityID;
    this.columnModel = columnModel;
    this.propertyFilterModels = initializePropertyFilterModels(columnModel);
    this.propertySearchModels = initializePropertySearchModels(columnModel, dbProvider);
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

  public TableColumnModel getColumnModel() {
    return columnModel;
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
   * The PropertyFilterModel associated with the property identified by <code>propertyID</code>
   * @param propertyID the id of the property for which to retrieve the PropertyFilterModel
   * @return the PropertyFilterModel for the property with id <code>propertyID</code>
   */
  public PropertyFilterModel getPropertyFilterModel(final String propertyID) {
    if (propertyFilterModels.containsKey(propertyID))
      return propertyFilterModels.get(propertyID);

    return null;
  }

  /**
   * @return the property filters configured in this table search model
   */
  public List<PropertyFilterModel> getPropertyFilterModels() {
    return new ArrayList<PropertyFilterModel>(propertyFilterModels.values());
  }

  /**
   * @param entity the entity
   * @return true if the entity should be included or filtered (hidden)
   */
  public boolean include(final Entity entity) {
    for (final AbstractSearchModel columnFilter : propertyFilterModels.values())
      if (columnFilter.isSearchEnabled() && !columnFilter.include(entity))
        return false;

    return true;
  }

  /**
   * Refreshes all combo box models associated with PropertySearchModels
   */
  public void refreshSearchComboBoxModels() {
    for (final PropertySearchModel model : propertySearchModels.values()) {
      if (model.getEntityComboBoxModel() != null)
        model.getEntityComboBoxModel().refresh();
    }
  }

  /**
   * Clears the contents from all combo box models associated with PropertySearchModels
   */
  public void clearSearchComboBoxModels() {
    for (final PropertySearchModel model : propertySearchModels.values()) {
      if (model.getEntityComboBoxModel() != null)
        model.getEntityComboBoxModel().clear();
    }
  }

  /**
   * Clears the state of all PropertySearchModels
   */
  public void clearPropertySearchModels() {
    for (final AbstractSearchModel searchModel : propertySearchModels.values())
      searchModel.clear();
  }

  /**
   * @return a list containing the PropertySearchModels configured in this table search model
   */
  public List<PropertySearchModel> getPropertySearchModels() {
    return new ArrayList<PropertySearchModel>(propertySearchModels.values());
  }

  /**
   * @param propertyID the id of the property for which to check for the PropertySearchModel
   * @return true if this EntityTableSearchModel contains a PropertySearchModel associated
   * with the property identified by <code>propertyID</code>
   */
  public boolean containsPropertySearchModel(final String propertyID) {
    return propertySearchModels.containsKey(propertyID);
  }

  /**
   * @param propertyID the id of the property for which to retrieve the PropertySearchModel
   * @return the PropertySearchModel associated with the property identified by <code>propertyID</code>
   */
  public PropertySearchModel getPropertySearchModel(final String propertyID) {
    if (propertySearchModels.containsKey(propertyID))
      return propertySearchModels.get(propertyID);

    throw new RuntimeException("PropertySearchModel not found for property with ID: " + propertyID);
  }

  /**
   * @param propertyID the column propertyID
   * @return true if the PropertySearchModel behind column with index <code>columnIndex</code> is enabled
   */
  public boolean isSearchEnabled(final String propertyID) {
    return containsPropertySearchModel(propertyID) && getPropertySearchModel(propertyID).isSearchEnabled();
  }

  /**
   * @param propertyID column propertyID
   * @return true if the PropertyFilterModel behind column with index <code>columnIndex</code> is enabled
   */
  public boolean isFilterEnabled(final String propertyID) {
    return getPropertyFilterModel(propertyID).isSearchEnabled();
  }

  /**
   * Sets the search criteria values of the search model associated with the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property
   * @param values the search criteria values
   * @return true if the search state changed as a result of this method call, false otherwise
   */
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
    if (filterModel != null)
      filterModel.setLikeValue(value);
  }

  /**
   * @return the current criteria based on the state of the search models
   */
  public Criteria getSearchCriteria() {
    final CriteriaSet criteriaSet = new CriteriaSet(getSearchConjunction());
    for (final AbstractSearchModel criteria : propertySearchModels.values())
      if (criteria.isSearchEnabled())
        criteriaSet.addCriteria(((PropertySearchModel) criteria).getPropertyCriteria());

    return criteriaSet.getCriteriaCount() > 0 ? criteriaSet : null;
  }

  /**
   * @return the conjunction to be used when more than one column search criteria is active,
   * the default is <code>CriteriaSet.Conjunction.AND</code>
   * @see org.jminor.common.db.criteria.CriteriaSet.Conjunction
   */
  public CriteriaSet.Conjunction getSearchConjunction() {
    return searchConjunction;
  }

  /**
   * @param searchConjunction the conjunction to be used when more than one column search criteria is active
   * @see org.jminor.common.db.criteria.CriteriaSet.Conjunction
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
    if (containsPropertySearchModel(propertyID))
      getPropertySearchModel(propertyID).setSearchEnabled(value);
  }

  /**
   * @return a State activated each time the search state differs from the state at last reset
   * @see #setSearchModelState()
   */
  public State stateSearchStateChanged() {
    return stSearchStateChanged;
  }

  /**
   * @return an Event fired when the state of a filter model changes
   */
  public Event eventFilterStateChanged() {
    return evtFilterStateChanged;
  }

  /**
   * @param tableColumnModel the TableColumnModel to base the search models on
   * @param dbProvider the EntityDbProvider to use for foreign key based fields, such as combo boxes
   * @return a map of PropertySearchModels mapped to their respective propertyIDs
   */
  protected Map<String, PropertySearchModel> initializePropertySearchModels(final TableColumnModel tableColumnModel,
                                                                            final EntityDbProvider dbProvider) {
    final Map<String, PropertySearchModel> searchModels = new HashMap<String, PropertySearchModel>();
    final Enumeration<TableColumn> columnEnumeration = tableColumnModel.getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final Property property = (Property) columnEnumeration.nextElement().getIdentifier();
      final PropertySearchModel searchModel = initializePropertySearchModel(property, dbProvider);
      if (searchModel != null)
        searchModels.put(property.getPropertyID(), searchModel);
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
    if (!property.isSearchable())
      return null;

    if (property instanceof Property.ForeignKeyProperty) {
      if (EntityRepository.isLargeDataset(((Property.ForeignKeyProperty) property).getReferencedEntityID())) {
        final EntityLookupModel lookupModel = new EntityLookupModel(((Property.ForeignKeyProperty) property).getReferencedEntityID(),
                dbProvider, getSearchProperties(((Property.ForeignKeyProperty) property).getReferencedEntityID()));
        lookupModel.setMultipleSelectionAllowed(true);
        return new PropertySearchModel(property, lookupModel);
      }
      else {
        return new PropertySearchModel(property, new EntityComboBoxModel(((Property.ForeignKeyProperty)
                property).getReferencedEntityID(), dbProvider, false, "", true));
      }
    }
    else {
      return new PropertySearchModel(property);
    }
  }

  /**
   * @param tableColumnModel the TableColumnModel to base the filter models on
   * @return a map of PropertyFilterModels mapped to their respective propertyIDs
   */
  protected Map<String, PropertyFilterModel> initializePropertyFilterModels(final TableColumnModel tableColumnModel) {
    final Map<String, PropertyFilterModel> filters = new HashMap<String, PropertyFilterModel>(tableColumnModel.getColumnCount());
    final Enumeration<TableColumn> columnEnumeration = tableColumnModel.getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final Property property = (Property) columnEnumeration.nextElement().getIdentifier();
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
        public void actionPerformed(final ActionEvent event) {
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
    for (final PropertySearchModel model : getPropertySearchModels())
      stringBuilder.append(model.toString());

    return stringBuilder.toString();
  }

  private List<Property> getSearchProperties(final String entityID) {
    final String[] searchPropertyIDs = EntityRepository.getEntitySearchPropertyIDs(entityID);

    return searchPropertyIDs == null ? getStringProperties(entityID) : EntityRepository.getProperties(entityID, searchPropertyIDs);
  }

  private List<Property> getStringProperties(final String entityID) {
    final Collection<Property> databaseProperties = EntityRepository.getDatabaseProperties(entityID);
    final List<Property> stringProperties = new ArrayList<Property>();
    for (final Property property : databaseProperties)
      if (property.isType(String.class))
        stringProperties.add(property);

    return stringProperties;
  }
}
