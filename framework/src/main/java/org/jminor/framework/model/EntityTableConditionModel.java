/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Conjunction;
import org.jminor.common.EventListener;
import org.jminor.common.EventObserver;
import org.jminor.common.StateObserver;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.ConditionType;
import org.jminor.common.model.FilterCondition;
import org.jminor.common.model.Refreshable;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;

/**
 * This interface defines filtering functionality, which refers to showing/hiding entities already available
 * in a table model and searching functionality, which refers to configuring the underlying query,
 * which then needs to be re-run.<br>
 */
public interface EntityTableConditionModel extends FilterCondition<Entity>, Refreshable {

  /**
   * @return the ID of the entity this table searcher is based on
   */
  String getEntityID();

  /**
   * Sets the search condition values of the condition model associated with the property identified by {@code propertyID}
   * @param propertyID the ID of the property
   * @param values the search condition values
   * @return true if the search state changed as a result of this method call, false otherwise
   */
  boolean setConditionValues(final String propertyID, final Collection<?> values);

  /**
   * Sets the condition value of the PropertyFilterModel associated with the property identified by {@code propertyID}.
   * @param propertyID the id of the property
   * @param value the condition value
   */
  void setFilterValue(final String propertyID, final Comparable value);

  /**
   * @return the current condition based on the state of the underlying condition models
   */
  Condition<Property.ColumnProperty> getTableCondition();

  /**
   * @return any additional search condition, not based on any individual property condition
   */
  Condition<Property.ColumnProperty> getAdditionalTableCondition();

  /**
   * Sets the additional condition, one not based on any individual property condition
   * @param condition the condition
   * @return this EntityTableConditionModel instance
   */
  EntityTableConditionModel setAdditionalTableCondition(final Condition<Property.ColumnProperty> condition);

  /**
   * @return true if any of the underlying PropertyConditionModels is enabled
   */
  boolean isEnabled();

  /**
   * @param propertyID the column propertyID
   * @return true if the PropertyConditionModel behind column with index {@code columnIndex} is enabled
   */
  boolean isEnabled(final String propertyID);

  /**
   * Enables/disables the search for the given property
   * @param propertyID the ID of the property for which to enable/disable the search
   * @param enabled if true the search is enabled, otherwise it is disabled
   */
  void setEnabled(final String propertyID, final boolean enabled);

  /**
   * Remembers the current condition model state, any subsequent changes to condition
   * parameters or operators are notified via the conditionStateChanged observer.
   * A data model using this condition model should call this method each time the
   * model is refreshed according to the condition provided by this condition model.
   * @see #getConditionStateObserver
   */
  void rememberCurrentConditionState();

  /**
   * @return true if the condition model state (or configuration) has changed
   * since the last time the condition model state was remembered
   * @see #rememberCurrentConditionState()
   */
  boolean hasConditionStateChanged();

  /**
   * @return the conjunction to be used when multiple column condition are active,
   * the default is {@code Conjunction.AND}
   * @see Conjunction
   */
  Conjunction getConjunction();

  /**
   * @param conjunction the conjunction to be used when more than one column search condition is active
   * @see Conjunction
   */
  void setConjunction(final Conjunction conjunction);

  /**
   * @param propertyID the id of the property for which to check for the PropertyConditionModel
   * @return true if this EntityTableConditionModel contains a PropertyConditionModel associated
   * with the property identified by {@code propertyID}
   */
  boolean containsPropertyConditionModel(final String propertyID);

  /**
   * @return a Collection containing the PropertyConditionModels available in this table condition model
   */
  Collection<PropertyConditionModel<? extends Property.SearchableProperty>> getPropertyConditionModels();

  /**
   * @param propertyID the id of the property for which to retrieve the PropertyConditionModel
   * @return the PropertyConditionModel associated with the property identified by {@code propertyID}
   * @throws IllegalArgumentException in case no condition model is available
   * @see #containsPropertyConditionModel(String)
   */
  PropertyConditionModel<? extends Property.SearchableProperty> getPropertyConditionModel(final String propertyID);

  /**
   * Clears the search state of all PropertyConditionModels, disables them and
   * resets the search type to {@link ConditionType#LIKE}
   */
  void clearPropertyConditionModels();

  /**
   * @return a Collection containing the filter models available in this table condition model
   */
  Collection<ColumnConditionModel<Property>> getPropertyFilterModels();

  /**
   * The PropertyFilterModel associated with the property identified by {@code propertyID}
   * @param propertyID the id of the property for which to retrieve the PropertyFilterModel
   * @return the PropertyFilterModel for the property with id {@code propertyID}, null if none is found
   */
  ColumnConditionModel<Property> getPropertyFilterModel(final String propertyID);

  /**
   * @param propertyID column propertyID
   * @return true if the PropertyFilterModel behind column with index {@code columnIndex} is enabled
   */
  boolean isFilterEnabled(final String propertyID);

  /**
   * @return the text used when performing a simple search
   * @see #performSimpleSearch()
   */
  String getSimpleConditionString();

  /**
   * @param simpleSearchText the text to use next time a simple search is performed
   * @see #performSimpleSearch()
   */
  void setSimpleConditionString(final String simpleSearchText);

  /**
   * Uses the simpleSearchText as a basis for a wildcard search on all String based condition models,
   * or the condition models representing the search properties for the underlying entity
   * @see org.jminor.framework.domain.Entities#getSearchProperties(String)
   */
  void performSimpleSearch();

  /**
   * @return a StateObserver indicating if the search state has changed since it was last remembered
   * @see #rememberCurrentConditionState()
   */
  StateObserver getConditionStateObserver();

  /**
   * @return an EventObserver notified each time the simple search text changes
   */
  EventObserver<String> getSimpleConditionStringObserver();

  /**
   * @param listener a listener notified each time the search state changes
   */
  void addConditionStateListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeConditionStateListener(final EventListener listener);

  /**
   * @param listener a listener notified each time a simple search is performed
   */
  void addSimpleConditionListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSimpleConditionListener(final EventListener listener);
}
