package org.jminor.framework.client.model;

import org.jminor.common.db.CriteriaSet;
import org.jminor.common.db.ICriteria;
import org.jminor.common.model.UserException;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * User: Björn Darri
 * Date: 24.7.2008
 * Time: 21:29:55
 */
public class EntityTableSearchModel {

  public final State stSimpleSearch = new State("EntityTableSearchModel.stSimpleSearch");
  public final State stSearchStateChanged = new State("EntityTableSearchModel.stSearchStateChanged");

  private final String entityID;
  private final IEntityDbProvider dbProvider;
  private final List<PropertySearchModel> propertySearchModels;
  private final Map<Property, EntityComboBoxModel> propertySearchComboBoxModels = new HashMap<Property, EntityComboBoxModel>();
  private CriteriaSet.Conjunction conjunction = CriteriaSet.Conjunction.AND;
  private String searchStateOnRefresh;

  public EntityTableSearchModel(final String entityID, final List<Property> searchableProperties,
                                final IEntityDbProvider dbProvider) {
    this.entityID = entityID;
    this.dbProvider = dbProvider;
    this.propertySearchModels = initPropertySearchModels(searchableProperties);
    this.searchStateOnRefresh = getSearchModelState();
  }

  public String getEntityID() {
    return entityID;
  }

  /**
   * @return a String representing the state of the search models
   */
  public String getSearchModelState() {
    final StringBuffer ret = new StringBuffer();
    for (final AbstractSearchModel model : getPropertySearchModels())
      ret.append(model.toString());

    return ret.toString();
  }

  public IEntityDbProvider getDbProvider() {
    return dbProvider;
  }

  public void resetSearchState() {
    searchStateOnRefresh = getSearchModelState();
    stSearchStateChanged.setActive(false);
  }

  /**
   * @param properties the properties for which to initialize PropertySearchModels
   * @return a list of PropertySearchModels initialized according to the properties in <code>properties</code>
   */
  private List<PropertySearchModel> initPropertySearchModels(final List<Property> properties) {
    final List<PropertySearchModel> ret = new ArrayList<PropertySearchModel>();
    for (final Property property : properties) {
      if (property instanceof Property.EntityProperty && ((Property.EntityProperty) property).isLookup())
        propertySearchComboBoxModels.put(property, new EntityComboBoxModel(dbProvider,
                ((Property.EntityProperty) property).referenceEntityID, false, "", true));

      final PropertySearchModel searchModel = new PropertySearchModel(property, propertySearchComboBoxModels.get(property));
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
   * Refreshes all combo box models associated with PropertySearchModels
   */
  public void refreshSearchComboBoxModels() {
    try {
      for (final EntityComboBoxModel model : propertySearchComboBoxModels.values())
        model.refresh();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
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
    final PropertySearchModel model =
            getPropertySearchModel(EntityRepository.get().getPropertyAtViewIndex(getEntityID(), columnIdx).propertyID);

    return model != null && model.isSearchEnabled();
  }

  /**
   * Finds the PropertySearchModel associated with the EntityProperty representing
   * the entity identified by <code>referencedEntityID</code> and sets <code>referenceEntities</code>
   * as the search criteria value, enables the PropertySearchModel and initiates a refresh
   * @param referencedEntityID the ID of the entity
   * @param referenceEntities the entities to use as search criteria value
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  public void setExactSearchValue(final String referencedEntityID, final List<Entity> referenceEntities) throws UserException {
    for (final Property.EntityProperty property : EntityRepository.get().getEntityProperties(getEntityID(), referencedEntityID)) {
      final PropertySearchModel searchModel = getPropertySearchModel(property.propertyID);
      if (searchModel != null) {
        searchModel.initialize();
        searchModel.setSearchEnabled(referenceEntities != null && referenceEntities.size() > 0);
        searchModel.setUpperBound((Object) null);//because the upperBound is a reference to the active entity and changes accordingly
        searchModel.setUpperBound(referenceEntities != null && referenceEntities.size() == 0 ? null : referenceEntities);//this then failes to register a changed upper bound
      }
    }
  }

  /**
   * @return a ICriteria object used to filter the result when this
   * table models data is queried
   */
  public ICriteria getSearchCriteria() {
    final CriteriaSet ret = new CriteriaSet(getSearchCriteriaConjunction());
    for (final AbstractSearchModel criteria : propertySearchModels)
      if (criteria.isSearchEnabled())
        ret.addCriteria(((PropertySearchModel) criteria).getPropertyCriteria());

    return ret.getCriteriaCount() > 0 ? ret : null;
  }

  /**
   * @return the conjuction to be used when more than one search criteria is specified, by default the
   * result depends on EntityTableModel.stSimpleSearch, being OR when it is active, AND otherwise
   * @see org.jminor.common.db.CriteriaSet.Conjunction
   */
  public CriteriaSet.Conjunction getSearchCriteriaConjunction() {
    return conjunction;
  }

  public void setConjunction(final CriteriaSet.Conjunction conjunction) {
    this.conjunction = conjunction;
  }

  public void setSearchEnabled(final String propertyID, final boolean value) {
    getPropertySearchModel(propertyID).setSearchEnabled(value);
  }

  public void setStringSearchValue(final String propertyID, final String searchText) {
    final PropertySearchModel searchModel = getPropertySearchModel(propertyID);
    if (searchModel != null) {
      searchModel.setCaseSensitive(false);
      searchModel.setUpperBound(searchText);
      searchModel.setSearchType(SearchType.LIKE);
      searchModel.setSearchEnabled(true);
    }
  }
}
