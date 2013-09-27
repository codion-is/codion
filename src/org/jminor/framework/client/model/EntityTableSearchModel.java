/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.table.ColumnSearchModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;

/**
 * This interface defines filtering functionality, which refers to showing/hiding entities already available
 * in a table model and searching functionality, which refers to configuring the underlying query,
 * which then needs to be re-run.<br>
 */
public interface EntityTableSearchModel extends FilterCriteria<Entity>, Refreshable {

  /**
   * @return the ID of the entity this table searcher is based on
   */
  String getEntityID();

  /**
   * Sets the search criteria values of the search model associated with the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property
   * @param values the search criteria values
   * @return true if the search state changed as a result of this method call, false otherwise
   */
  boolean setSearchValues(final String propertyID, final Collection<?> values);

  /**
   * Sets the criteria value of the PropertyFilterModel associated with the property identified by <code>propertyID</code>.
   * @param propertyID the id of the property
   * @param value the criteria value
   */
  void setFilterValue(final String propertyID, final Comparable value);

  /**
   * @return the current criteria based on the state of the search models
   */
  Criteria<Property.ColumnProperty> getSearchCriteria();

  /**
   * @return any additional search criteria, not based on any individual property criteria
   */
  Criteria<Property.ColumnProperty> getAdditionalSearchCriteria();

  /**
   * Sets the additional search criteria, one not based on any individual property criteria
   * @param criteria the criteria
   * @return this EntityTableSearchModel instance
   */
  EntityTableSearchModel setAdditionalSearchCriteria(final Criteria<Property.ColumnProperty> criteria);

  /**
   * @param propertyID the column propertyID
   * @return true if the PropertySearchModel behind column with index <code>columnIndex</code> is enabled
   */
  boolean isSearchEnabled(final String propertyID);

  /**
   * Enables/disables the search for the given property
   * @param propertyID the ID of the property for which to enable/disable the search
   * @param enabled if true the search is enabled, otherwise it is disabled
   */
  void setSearchEnabled(final String propertyID, final boolean enabled);

  /**
   * Remembers the current search model state, any subsequent changes to search
   * parameters or operators are notified via the searchStateChanged observer.
   * A data model using this search model should call this method each time the
   * model is refreshed according to the search criteria provided by this search model.
   * @see #getSearchStateObserver
   */
  void rememberCurrentSearchState();

  /**
   * @return true if the search model state (or configuration) has changed
   * since the last time the search model state was remembered
   * @see #rememberCurrentSearchState()
   */
  boolean hasSearchStateChanged();

  /**
   * @return the conjunction to be used when multiple column search criteria are active,
   * the default is <code>Conjunction.AND</code>
   * @see Conjunction
   */
  Conjunction getSearchConjunction();

  /**
   * @param conjunction the conjunction to be used when more than one column search criteria is active
   * @see Conjunction
   */
  void setSearchConjunction(final Conjunction conjunction);

  /**
   * @param propertyID the id of the property for which to check for the PropertySearchModel
   * @return true if this EntityTableSearchModel contains a PropertySearchModel associated
   * with the property identified by <code>propertyID</code>
   */
  boolean containsPropertySearchModel(final String propertyID);

  /**
   * @return a Collection containing the PropertySearchModels available in this table search model
   */
  Collection<PropertySearchModel<? extends Property.SearchableProperty>> getPropertySearchModels();

  /**
   * @param propertyID the id of the property for which to retrieve the PropertySearchModel
   * @return the PropertySearchModel associated with the property identified by <code>propertyID</code>
   * @throws IllegalArgumentException in case no search model is available
   * @see #containsPropertySearchModel(String)
   */
  PropertySearchModel<? extends Property.SearchableProperty> getPropertySearchModel(final String propertyID);

  /**
   * Clears the search state of all PropertySearchModels, disables them and
   * resets the search type to {@link org.jminor.common.model.SearchType#LIKE}
   */
  void clearPropertySearchModels();

  /**
   * @return a Collection containing the ColumnSearchModels available in this table search model
   */
  Collection<ColumnSearchModel<Property>> getPropertyFilterModels();

  /**
   * The PropertyFilterModel associated with the property identified by <code>propertyID</code>
   * @param propertyID the id of the property for which to retrieve the PropertyFilterModel
   * @return the PropertyFilterModel for the property with id <code>propertyID</code>, null if none is found
   */
  ColumnSearchModel<Property> getPropertyFilterModel(final String propertyID);

  /**
   * @param propertyID column propertyID
   * @return true if the PropertyFilterModel behind column with index <code>columnIndex</code> is enabled
   */
  boolean isFilterEnabled(final String propertyID);

  /**
   * @return the text used when performing a simple search
   * @see #performSimpleSearch()
   */
  String getSimpleSearchString();

  /**
   * @param simpleSearchText the text to use next time a simple search is performed
   * @see #performSimpleSearch()
   */
  void setSimpleSearchString(final String simpleSearchText);

  /**
   * Uses the simpleSearchText as a basis for a wildcard search on all String based search models,
   * or the search models representing the search properties for the underlying entity
   * @see org.jminor.framework.domain.Entities#getSearchProperties(String)
   */
  void performSimpleSearch();

  /**
   * @return a StateObserver indicating if the search state has changed since it was last remembered
   * @see #rememberCurrentSearchState()
   */
  StateObserver getSearchStateObserver();

  /**
   * @return an EventObserver notified each time the simple search text changes
   */
  EventObserver<String> getSimpleSearchStringObserver();

  /**
   * @param listener a listener notified each time a simple search is performed
   */
  void addSimpleSearchListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSimpleSearchListener(final EventListener listener);
}
