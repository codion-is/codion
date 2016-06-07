/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Conjunction;
import org.jminor.common.EventListener;
import org.jminor.common.EventObserver;
import org.jminor.common.StateObserver;
import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.table.ColumnCriteriaModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;

/**
 * This interface defines filtering functionality, which refers to showing/hiding entities already available
 * in a table model and searching functionality, which refers to configuring the underlying query,
 * which then needs to be re-run.<br>
 */
public interface EntityTableCriteriaModel extends FilterCriteria<Entity>, Refreshable {

  /**
   * @return the ID of the entity this table searcher is based on
   */
  String getEntityID();

  /**
   * Sets the search criteria values of the criteria model associated with the property identified by {@code propertyID}
   * @param propertyID the ID of the property
   * @param values the search criteria values
   * @return true if the search state changed as a result of this method call, false otherwise
   */
  boolean setCriteriaValues(final String propertyID, final Collection<?> values);

  /**
   * Sets the criteria value of the PropertyFilterModel associated with the property identified by {@code propertyID}.
   * @param propertyID the id of the property
   * @param value the criteria value
   */
  void setFilterValue(final String propertyID, final Comparable value);

  /**
   * @return the current criteria based on the state of the underlying criteria models
   */
  Criteria<Property.ColumnProperty> getTableCriteria();

  /**
   * @return any additional search criteria, not based on any individual property criteria
   */
  Criteria<Property.ColumnProperty> getAdditionalTableCriteria();

  /**
   * Sets the additional criteria, one not based on any individual property criteria
   * @param criteria the criteria
   * @return this EntityTableCriteriaModel instance
   */
  EntityTableCriteriaModel setAdditionalTableCriteria(final Criteria<Property.ColumnProperty> criteria);

  /**
   * @return true if any of the underlying PropertyCriteriaModels is enabled
   */
  boolean isEnabled();

  /**
   * @param propertyID the column propertyID
   * @return true if the PropertyCriteriaModel behind column with index {@code columnIndex} is enabled
   */
  boolean isEnabled(final String propertyID);

  /**
   * Enables/disables the search for the given property
   * @param propertyID the ID of the property for which to enable/disable the search
   * @param enabled if true the search is enabled, otherwise it is disabled
   */
  void setEnabled(final String propertyID, final boolean enabled);

  /**
   * Remembers the current criteria model state, any subsequent changes to criteria
   * parameters or operators are notified via the criteriaStateChanged observer.
   * A data model using this criteria model should call this method each time the
   * model is refreshed according to the criteria provided by this criteria model.
   * @see #getCriteriaStateObserver
   */
  void rememberCurrentCriteriaState();

  /**
   * @return true if the criteria model state (or configuration) has changed
   * since the last time the criteria model state was remembered
   * @see #rememberCurrentCriteriaState()
   */
  boolean hasCriteriaStateChanged();

  /**
   * @return the conjunction to be used when multiple column criteria are active,
   * the default is {@code Conjunction.AND}
   * @see Conjunction
   */
  Conjunction getConjunction();

  /**
   * @param conjunction the conjunction to be used when more than one column search criteria is active
   * @see Conjunction
   */
  void setConjunction(final Conjunction conjunction);

  /**
   * @param propertyID the id of the property for which to check for the PropertyCriteriaModel
   * @return true if this EntityTableCriteriaModel contains a PropertyCriteriaModel associated
   * with the property identified by {@code propertyID}
   */
  boolean containsPropertyCriteriaModel(final String propertyID);

  /**
   * @return a Collection containing the PropertyCriteriaModels available in this table criteria model
   */
  Collection<PropertyCriteriaModel<? extends Property.SearchableProperty>> getPropertyCriteriaModels();

  /**
   * @param propertyID the id of the property for which to retrieve the PropertyCriteriaModel
   * @return the PropertyCriteriaModel associated with the property identified by {@code propertyID}
   * @throws IllegalArgumentException in case no criteria model is available
   * @see #containsPropertyCriteriaModel(String)
   */
  PropertyCriteriaModel<? extends Property.SearchableProperty> getPropertyCriteriaModel(final String propertyID);

  /**
   * Clears the search state of all PropertyCriteriaModels, disables them and
   * resets the search type to {@link org.jminor.common.model.SearchType#LIKE}
   */
  void clearPropertyCriteriaModels();

  /**
   * @return a Collection containing the filter models available in this table criteria model
   */
  Collection<ColumnCriteriaModel<Property>> getPropertyFilterModels();

  /**
   * The PropertyFilterModel associated with the property identified by {@code propertyID}
   * @param propertyID the id of the property for which to retrieve the PropertyFilterModel
   * @return the PropertyFilterModel for the property with id {@code propertyID}, null if none is found
   */
  ColumnCriteriaModel<Property> getPropertyFilterModel(final String propertyID);

  /**
   * @param propertyID column propertyID
   * @return true if the PropertyFilterModel behind column with index {@code columnIndex} is enabled
   */
  boolean isFilterEnabled(final String propertyID);

  /**
   * @return the text used when performing a simple search
   * @see #performSimpleSearch()
   */
  String getSimpleCriteriaString();

  /**
   * @param simpleSearchText the text to use next time a simple search is performed
   * @see #performSimpleSearch()
   */
  void setSimpleCriteriaString(final String simpleSearchText);

  /**
   * Uses the simpleSearchText as a basis for a wildcard search on all String based criteria models,
   * or the criteria models representing the search properties for the underlying entity
   * @see org.jminor.framework.domain.Entities#getSearchProperties(String)
   */
  void performSimpleSearch();

  /**
   * @return a StateObserver indicating if the search state has changed since it was last remembered
   * @see #rememberCurrentCriteriaState()
   */
  StateObserver getCriteriaStateObserver();

  /**
   * @return an EventObserver notified each time the simple search text changes
   */
  EventObserver<String> getSimpleCriteriaStringObserver();

  /**
   * @param listener a listener notified each time the search state changes
   */
  void addCriteriaStateListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeCriteriaStateListener(final EventListener listener);

  /**
   * @param listener a listener notified each time a simple search is performed
   */
  void addSimpleCriteriaListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSimpleCriteriaListener(final EventListener listener);
}
