/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.db.Operator;
import is.codion.common.event.EventListener;
import is.codion.common.event.EventObserver;
import is.codion.common.model.Refreshable;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.StateObserver;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.Property;

import java.util.Collection;

/**
 * This interface defines filtering functionality, which refers to showing/hiding entities already available
 * in a table model and searching functionality, which refers to configuring the underlying query,
 * which then needs to be re-run.<br>
 */
public interface EntityTableConditionModel extends Refreshable {

  /**
   * @return the type of the entity this table condition model is based on
   */
  EntityType<?> getEntityType();

  /**
   * Sets the search condition values of the condition model associated with {@code attribute}.
   * @param attribute the attribute
   * @param values the search condition values
   * @param <T> the value type
   * @return true if the search state changed as a result of this method call, false otherwise
   */
  <T> boolean setConditionValues(Attribute<T> attribute, Collection<T> values);

  /**
   * Sets the condition value of the PropertyFilterModel associated with {@code attribute}.
   * @param attribute the attribute
   * @param value the condition value
   * @param <T> the value type
   */
  <T> void setFilterValue(Attribute<T> attribute, Comparable<T> value);

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
   * @param attribute the column attribute
   * @return true if the {@link ColumnConditionModel} associated with the given attribute is enabled
   */
  boolean isConditionEnabled(Attribute<?> attribute);

  /**
   * Enables the search for the given property
   * @param attribute the attribute for which to enable the search
   */
  void enable(Attribute<?> attribute);

  /**
   * Disables the search for the given property
   * @param attribute the attribute for which to disable the search
   */
  void disable(Attribute<?> attribute);

  /**
   * Remembers the current condition model state, any subsequent changes to condition
   * parameters or operators are notified via the conditionChanged observer.
   * A data model using this condition model should call this method each time the
   * model is refreshed according to the condition provided by this condition model.
   * @see #hasConditionChanged()
   * @see #getConditionObserver
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
   * @param attribute the attribute for which to check for the {@link ColumnConditionModel}
   * @return true if this EntityTableConditionModel contains a {@link ColumnConditionModel} associated
   * with {@code attribute}
   */
  boolean containsConditionModel(Attribute<?> attribute);

  /**
   * @return a Collection containing the {@link ColumnConditionModel}s available in this table condition model
   */
  Collection<ColumnConditionModel<Entity, ? extends Property<?>>> getConditionModels();

  /**
   * Returns the {@link ColumnConditionModel} associated with the given attribute.
   * @param attribute the attribute for which to retrieve the {@link ColumnConditionModel}
   * @return the {@link ColumnConditionModel} associated with {@code attribute}
   * @throws IllegalArgumentException in case no condition model is found
   * @see #containsConditionModel(Attribute)
   */
  ColumnConditionModel<Entity, ? extends Property<?>> getConditionModel(Attribute<?> attribute);

  /**
   * Clears the search state of all {@link ColumnConditionModel}, disables them and
   * resets the operator to {@link Operator#EQUALS}
   */
  void clearConditionModels();

  /**
   * @return a Collection containing the filter models available in this table condition model
   */
  Collection<ColumnConditionModel<Entity, Property<?>>> getFilterModels();

  /**
   * The filter model associated with {@code attribute}
   * @param attribute the attribute for which to retrieve the {@link ColumnConditionModel}
   * @return the {@link ColumnConditionModel} for the {@code attribute}, null if none is found
   */
  ColumnConditionModel<Entity, Property<?>> getFilterModel(Attribute<?> attribute);

  /**
   * @param attribute column attribute
   * @return true if the filter model behind column with index {@code columnIndex} is enabled
   */
  boolean isFilterEnabled(Attribute<?> attribute);

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
   * @return a StateObserver indicating if the search condition has changed since it was last remembered
   * @see #rememberCondition()
   */
  StateObserver getConditionObserver();

  /**
   * @return an EventObserver notified each time the simple search text changes
   */
  EventObserver<String> getSimpleConditionStringObserver();

  /**
   * @param listener a listener notified each time the search condition changes
   */
  void addConditionListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeConditionListener(EventListener listener);

  /**
   * @param listener a listener notified each time a simple search is performed
   */
  void addSimpleConditionListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSimpleConditionListener(EventListener listener);
}
