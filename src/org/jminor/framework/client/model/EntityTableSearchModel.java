/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.ColumnSearchModel;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.StateObserver;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;

/**
 * This interface defines filtering functionality, which refers to showing/hiding entities already available
 * in a table model and searching functionality, which refers to configuring the underlying query,
 * which then needs to be re-run.<br>
 */
public interface EntityTableSearchModel extends FilterCriteria<Entity>, Refreshable {

  /**
   * @return a State activated each time the search state differs from the state at last reset
   * @see #setSearchModelState()
   */
  StateObserver getSearchStateChangedState();

  void addFilterStateListener(final ActionListener listener);

  void removeFilterStateListener(final ActionListener listener);

  /**
   * @return the ID of the entity this table searcher is based on
   */
  String getEntityID();

  /**
   * @return the properties that are searchable via this searcher
   */
  List<Property.SearchableProperty> getSearchableProperties();

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

  Criteria<Property.ColumnProperty> getAdditionalSearchCriteria();

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
   * Sets the current search model state
   * @see #getSearchStateChangedState
   */
  void setSearchModelState();

  boolean isSimpleSearch();

  /**
   * @return the conjunction to be used when more than one column search criteria is active,
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
   * @return a list containing the PropertySearchModels configured in this table search model
   */
  Collection<PropertySearchModel<? extends Property.SearchableProperty>> getPropertySearchModels();

  /**
   * @param propertyID the id of the property for which to retrieve the PropertySearchModel
   * @return the PropertySearchModel associated with the property identified by <code>propertyID</code>
   */
  PropertySearchModel<? extends Property.SearchableProperty> getPropertySearchModel(final String propertyID);

  /**
   * Clears the state of all PropertySearchModels
   */
  void clearPropertySearchModels();

  /**
   * @return the property filters configured in this table search model
   */
  Collection<ColumnSearchModel<Property>> getPropertyFilterModels();

  /**
   * @return the PropertyFilterModels in the same order as the columns they represent
   */
  List<ColumnSearchModel<Property>> getPropertyFilterModelsOrdered();

  /**
   * The PropertyFilterModel associated with the property identified by <code>propertyID</code>
   * @param propertyID the id of the property for which to retrieve the PropertyFilterModel
   * @return the PropertyFilterModel for the property with id <code>propertyID</code>
   */
  ColumnSearchModel<Property> getPropertyFilterModel(final String propertyID);

  /**
   * @param propertyID column propertyID
   * @return true if the PropertyFilterModel behind column with index <code>columnIndex</code> is enabled
   */
  boolean isFilterEnabled(final String propertyID);
}
