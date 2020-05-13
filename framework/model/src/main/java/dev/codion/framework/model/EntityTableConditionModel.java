/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.model;

import dev.codion.common.Conjunction;
import dev.codion.common.db.Operator;
import dev.codion.common.event.EventListener;
import dev.codion.common.event.EventObserver;
import dev.codion.common.model.Refreshable;
import dev.codion.common.model.table.ColumnConditionModel;
import dev.codion.common.state.StateObserver;
import dev.codion.framework.db.condition.Condition;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.entity.EntityDefinition;
import dev.codion.framework.domain.property.Property;

import java.util.Collection;

/**
 * This interface defines filtering functionality, which refers to showing/hiding entities already available
 * in a table model and searching functionality, which refers to configuring the underlying query,
 * which then needs to be re-run.<br>
 */
public interface EntityTableConditionModel extends Refreshable {

  /**
   * @return the id of the entity this table condition model is based on
   */
  String getEntityId();

  /**
   * Sets the search condition values of the condition model associated with the property identified by {@code propertyId}
   * @param propertyId the id of the property
   * @param values the search condition values
   * @return true if the search state changed as a result of this method call, false otherwise
   */
  boolean setConditionValues(String propertyId, Collection values);

  /**
   * Sets the condition value of the PropertyFilterModel associated with the property identified by {@code propertyId}.
   * @param propertyId the id of the property
   * @param value the condition value
   */
  void setFilterValue(String propertyId, Comparable value);

  /**
   * @return the current condition based on the state of the underlying condition models
   */
  Condition getCondition();

  /**
   * @return any additional search condition, not based on any individual property condition
   */
  Condition.Provider getAdditionalConditionProvider();

  /**
   * Sets the additional condition provider, one not based on any individual property condition
   * @param conditionProvider the condition provider
   */
  void setAdditionalConditionProvider(Condition.Provider conditionProvider);

  /**
   * @return true if any of the underlying PropertyConditionModels is enabled
   */
  boolean isEnabled();

  /**
   * @param propertyId the column propertyId
   * @return true if the {@link ColumnConditionModel} behind column with index {@code columnIndex} is enabled
   */
  boolean isEnabled(String propertyId);

  /**
   * Enables the search for the given property
   * @param propertyId the id of the property for which to enable the search
   */
  void enable(String propertyId);

  /**
   * Disables the search for the given property
   * @param propertyId the id of the property for which to disable the search
   */
  void disable(String propertyId);

  /**
   * Remembers the current condition model state, any subsequent changes to condition
   * parameters or operators are notified via the conditionChanged observer.
   * A data model using this condition model should call this method each time the
   * model is refreshed according to the condition provided by this condition model.
   * @see #hasConditionChanged()
   * @see #getConditionChangedObserver
   */
  void rememberCondition();

  /**
   * @return true if the condition model state (or configuration) has changed
   * since the last time the condition model state was remembered
   * @see #rememberCondition()
   */
  boolean hasConditionChanged();

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
  void setConjunction(Conjunction conjunction);

  /**
   * @param propertyId the id of the property for which to check for the {@link ColumnConditionModel}
   * @return true if this EntityTableConditionModel contains a {@link ColumnConditionModel} associated
   * with the property identified by {@code propertyId}
   */
  boolean containsPropertyConditionModel(String propertyId);

  /**
   * @return a Collection containing the PropertyConditionModels available in this table condition model
   */
  Collection<ColumnConditionModel<Entity, ? extends Property>> getPropertyConditionModels();

  /**
   * Returns the {@link ColumnConditionModel} associated with the given property.
   * @param propertyId the id of the property for which to retrieve the {@link ColumnConditionModel}
   * @return the {@link ColumnConditionModel} associated with the property identified by {@code propertyId}
   * @throws IllegalArgumentException in case no condition model is found
   * @see #containsPropertyConditionModel(String)
   */
  ColumnConditionModel<Entity, ? extends Property> getPropertyConditionModel(String propertyId);

  /**
   * Clears the search state of all PropertyConditionModels, disables them and
   * resets the operator to {@link Operator#LIKE}
   */
  void clearPropertyConditionModels();

  /**
   * @return a Collection containing the filter models available in this table condition model
   */
  Collection<ColumnConditionModel<Entity, Property>> getPropertyFilterModels();

  /**
   * The PropertyFilterModel associated with the property identified by {@code propertyId}
   * @param propertyId the id of the property for which to retrieve the PropertyFilterModel
   * @return the PropertyFilterModel for the property with id {@code propertyId}, null if none is found
   */
  ColumnConditionModel<Entity, Property> getPropertyFilterModel(String propertyId);

  /**
   * @param propertyId column propertyId
   * @return true if the PropertyFilterModel behind column with index {@code columnIndex} is enabled
   */
  boolean isFilterEnabled(String propertyId);

  /**
   * @return the text used when performing a simple search
   * @see #performSimpleSearch()
   */
  String getSimpleConditionString();

  /**
   * Note that calling this method may (and probably will) change the automatic prefix and case sensetivity settings of
   * the underlying {@link ColumnConditionModel}s
   * @param simpleSearchText the text to use next time a simple search is performed
   * @see #performSimpleSearch()
   * @see ColumnConditionModel#setCaseSensitive(boolean)
   * @see ColumnConditionModel#setAutomaticWildcard(ColumnConditionModel.AutomaticWildcard)
   */
  void setSimpleConditionString(String simpleSearchText);

  /**
   * Uses the simpleSearchText as a basis for a wildcard search on all String based condition models,
   * or the condition models representing the search properties for the underlying entity
   * @see EntityDefinition#getSearchProperties()
   */
  void performSimpleSearch();

  /**
   * @return a StateObserver indicating if the search state has changed since it was last remembered
   * @see #rememberCondition()
   */
  StateObserver getConditionChangedObserver();

  /**
   * @return an EventObserver notified each time the simple search text changes
   */
  EventObserver<String> getSimpleConditionStringObserver();

  /**
   * @param listener a listener notified each time the search state changes
   */
  void addConditionChangedListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeConditionChangedListener(EventListener listener);

  /**
   * @param listener a listener notified each time a simple search is performed
   */
  void addSimpleConditionListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSimpleConditionListener(EventListener listener);
}
