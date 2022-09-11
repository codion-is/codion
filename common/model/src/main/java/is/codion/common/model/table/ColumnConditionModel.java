/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.Configuration;
import is.codion.common.Operator;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.properties.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;

import java.text.Format;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Specifies a condition model based on a table column, parameters, operator, upper bound and lower bound,
 * as well as relevant events and states.
 * @param <C> the type used to identify columns
 * @param <T> the column value type
 */
public interface ColumnConditionModel<C, T> {

  /**
   * Specifies whether wildcards are automatically added to string conditions by default<br>
   * Value type: {@link AutomaticWildcard}<br>
   * Default value: {@link AutomaticWildcard#NONE}
   */
  PropertyValue<AutomaticWildcard> AUTOMATIC_WILDCARD =
          Configuration.enumValue("is.codion.common.model.table.ColumnConditionModel.automaticWildard",
                  AutomaticWildcard.class, AutomaticWildcard.NONE);

  /**
   * Specifies whether string based conditions are case-sensitive or not by default<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> CASE_SENSITIVE =
          Configuration.booleanValue("is.codion.common.model.table.ColumnConditionModel.caseSensitive", true);

  /**
   * The possible automatic wildcard types
   */
  enum AutomaticWildcard {
    /**
     * No wildcard
     */
    NONE,
    /**
     * Wildard added at front
     */
    PREFIX,
    /**
     * Wildcard added at end
     */
    POSTFIX,
    /**
     * Wildcard added at front and at end
     */
    PREFIX_AND_POSTFIX;

    private final String description;

    AutomaticWildcard() {
      this.description = ResourceBundle.getBundle(AutomaticWildcard.class.getName()).getString(this.toString());
    }

    /**
     * @return a description
     */
    public String description() {
      return description;
    }
  }

  /**
   * @return the column identifier
   */
  C columnIdentifier();

  /**
   * @return the State controlling whether this model is case-sensitive, when working with strings
   */
  State caseSensitiveState();

  /**
   * @return the Format object to use when formatting input, if any
   */
  Format format();

  /**
   * @return the date/time format pattern, if any
   */
  String dateTimePattern();

  /**
   * Note that this is only applicable to string based condition models and only used for
   * operators {@link Operator#EQUAL} and {@link Operator#NOT_EQUAL}
   * @return the Value controlling whether automatic wildcards are enabled when working with strings
   */
  Value<AutomaticWildcard> automaticWildcardValue();

  /**
   * @return the {@link State} controlling whether this model is enabled automatically when a condition value is specified
   */
  State autoEnableState();

  /**
   * @param locked true to lock this model, false to unlock
   */
  void setLocked(boolean locked);

  /**
   * @return true if this model is locked
   */
  boolean isLocked();

  /**
   * @return the column class this condition model is based on
   */
  Class<T> columnClass();

  /**
   * Sets the values used when the {@link Operator#EQUAL} is enabled.
   * @param value the value to use as condition
   */
  void setEqualValue(T value);

  /**
   * @return the equal value, possibly null
   */
  T getEqualValue();

  /**
   * @param values the values to set
   */
  void setEqualValues(Collection<T> values);

  /**
   * @return the equal values, never null
   */
  Collection<T> getEqualValues();

  /**
   * @param upper the new upper bound
   */
  void setUpperBound(T upper);

  /**
   * @return the upper bound
   */
  T getUpperBound();

  /**
   * @param value the lower bound
   */
  void setLowerBound(T value);

  /**
   * @return the lower bound
   */
  T getLowerBound();

  /**
   * @return the search operator
   */
  Operator getOperator();

  /**
   * @param operator the conditional operator
   * @throws IllegalArgumentException in case the given operator is not available in this condition model
   */
  void setOperator(Operator operator);

  /**
   * @return the operators available in this condition model
   */
  List<Operator> operators();

  /**
   * @return the Value controlling the character used as a wildcard when working with strings
   */
  Value<Character> wildcardValue();

  /**
   * @return true if this condition model is enabled
   */
  boolean isEnabled();

  /**
   * @param enabled true to enable, false to disable
   */
  void setEnabled(boolean enabled);

  /**
   * Disables and clears this condition model, that is, sets the upper and lower bounds to null
   * and the operator to the default value {@link Operator#EQUAL}
   */
  void clearCondition();

  /**
   * @return a ValueSet based on the equals values of this condition model
   */
  ValueSet<T> equalValueSet();

  /**
   * @return a Value based on the upper bound value of this condition model
   */
  Value<T> upperBoundValue();

  /**
   * @return a Value based on the lower bound value of this condition model
   */
  Value<T> lowerBoundValue();

  /**
   * @return an observer for this model's locked state
   */
  StateObserver lockedObserver();

  /**
   * @return a State controlling the enabled state
   */
  State enabledState();

  /**
   * @return a Value based on the operator
   */
  Value<Operator> operatorValue();

  /**
   * @param listener a listener to be notified each time the enabled state changes
   */
  void addEnabledListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeEnabledListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the operator changes
   */
  void addOperatorListener(EventDataListener<Operator> listener);

  /**
   * @param listener the listener to remove
   */
  void removeOperatorListener(EventDataListener<Operator> listener);

  /**
   * @param listener a listener to be notified each time the lower bound changes
   */
  void addEqualsValueListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeEqualsValueListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the lower bound changes
   */
  void addLowerBoundListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeLowerBoundListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the upper bound changes
   */
  void addUpperBoundListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeUpperBoundListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the model is cleared
   */
  void addClearedListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeClearedListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the condition state changes
   */
  void addConditionChangedListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeConditionChangedListener(EventListener listener);
}
