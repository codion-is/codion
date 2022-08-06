/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.Operator;
import is.codion.common.event.EventListener;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnFilterModel;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * This interface defines filtering functionality, which refers to showing/hiding entities already available
 * in a table model and searching functionality, which refers to configuring the underlying query,
 * which then needs to be re-run.<br>
 */
public interface EntityTableConditionModel {

  /**
   * @return the type of the entity this table condition model is based on
   */
  EntityType entityType();

  /**
   * @return the underlying entity definition
   */
  EntityDefinition entityDefinition();

  /**
   * Sets the search condition values of the condition model associated with {@code attribute}.
   * @param attribute the attribute
   * @param values the search condition values
   * @param <T> the value type
   * @return true if the search state changed as a result of this method call, false otherwise
   */
  <T> boolean setEqualConditionValues(Attribute<T> attribute, Collection<T> values);

  /**
   * Sets the condition value of the filter model associated with {@code attribute}.
   * @param attribute the attribute
   * @param value the condition value
   * @param <T> the value type
   */
  <T> void setEqualFilterValue(Attribute<T> attribute, Comparable<T> value);

  /**
   * @return the current condition based on the state of the underlying condition models
   */
  Condition condition();

  /**
   * @return supplies any additional search condition, not based on any individual property condition
   */
  Supplier<Condition> getAdditionalConditionSupplier();

  /**
   * Sets the additional condition supplier, one not based on any individual property condition
   * @param conditionSupplier the condition supplier
   */
  void setAdditionalConditionSupplier(Supplier<Condition> conditionSupplier);

  /**
   * @return true if any of the underlying condition models are enabled
   */
  boolean isConditionEnabled();

  /**
   * @param attribute the column attribute
   * @return true if the {@link ColumnConditionModel} associated with the given attribute is enabled
   */
  boolean isConditionEnabled(Attribute<?> attribute);

  /**
   * Remembers the current condition model state, any subsequent changes to condition
   * parameters or operators are notified via the conditionChanged observer.
   * A data model using this condition model should call this method each time the
   * model is refreshed according to the condition provided by this condition model.
   * @see #hasConditionChanged()
   * @see #conditionChangedObserver
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
   * @return a Map containing the {@link ColumnConditionModel}s available in this table condition model, mapped to their respective attributes
   */
  Map<Attribute<?>, ColumnConditionModel<? extends Attribute<?>, ?>> conditionModels();

  /**
   * Returns the {@link ColumnConditionModel} associated with the given attribute.
   * @param <C> the attribute type
   * @param <T> the column value type
   * @param attribute the attribute for which to retrieve the {@link ColumnConditionModel}
   * @return the {@link ColumnConditionModel} associated with {@code attribute}
   * @throws IllegalArgumentException in case no condition model exists for the given attribute
   */
  <C extends Attribute<T>, T> ColumnConditionModel<C, T> conditionModel(C attribute);

  /**
   * Clears the search state of all the condition models, disables them and
   * resets the operator to {@link Operator#EQUAL}
   */
  void clearConditions();

  /**
   * @return a Map containing the filter models available in this table condition model, mapped to their respective attributes
   */
  Map<Attribute<?>, ColumnFilterModel<Entity, Attribute<?>, ?>> filterModels();

  /**
   * The filter model associated with {@code attribute}
   * @param <C> the attribute type
   * @param <T> the column value type
   * @param attribute the attribute for which to retrieve the {@link ColumnFilterModel}
   * @return the {@link ColumnFilterModel} for the {@code attribute}
   * @throws IllegalArgumentException in case no filter model exists for the given attribute
   */
  <C extends Attribute<T>, T> ColumnFilterModel<Entity, C, T> filterModel(C attribute);

  /**
   * Clears the search state of all the filter models, disables them and
   * resets the operator to {@link Operator#EQUAL}
   */
  void clearFilters();

  /**
   * @return true if any of the underlying filter models are enabled
   */
  boolean isFilterEnabled();

  /**
   * @param attribute column attribute
   * @return true if the filter model behind column with index {@code columnIndex} is enabled
   */
  boolean isFilterEnabled(Attribute<?> attribute);

  /**
   * Refreshes any data bound models in this table condition model
   */
  void refresh();

  /**
   * Note that modifying this value may (and probably will) change the automatic prefix and case sensetivity settings of
   * the underlying {@link ColumnConditionModel}s
   * @see ColumnConditionModel#caseSensitiveState()
   * @see ColumnConditionModel#automaticWildcardValue()
   * @return the value used when performing a simple search.
   */
  Value<String> simpleConditionStringValue();

  /**
   * @return a StateObserver indicating if the search condition has changed since it was last remembered
   * @see #rememberCondition()
   */
  StateObserver conditionChangedObserver();

  /**
   * @param listener a listener notified each time the search condition changes
   */
  void addConditionChangedListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeConditionChangedListener(EventListener listener);
}
