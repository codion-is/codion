/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Specifies a condition model based on a table column, parameters, operator, upper bound and lower bound,
 * as well as relevant events and states.
 * For instances create a {@link Builder} via {@link #builder(Object, Class)}.
 * @param <C> the type used to identify columns
 * @param <T> the column value type
 */
public interface ColumnConditionModel<C, T> {

  /**
   * Specifies whether wildcards are automatically added to string conditions by default<br>
   * Value type: {@link AutomaticWildcard}<br>
   * Default value: {@link AutomaticWildcard#POSTFIX}
   */
  PropertyValue<AutomaticWildcard> AUTOMATIC_WILDCARD =
          Configuration.enumValue("is.codion.common.model.table.ColumnConditionModel.automaticWildard",
                  AutomaticWildcard.class, AutomaticWildcard.POSTFIX);

  /**
   * Specifies whether string based conditions are case-sensitive or not by default<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> CASE_SENSITIVE =
          Configuration.booleanValue("is.codion.common.model.table.ColumnConditionModel.caseSensitive", false);

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
   * @return the character used as a wildcard when working with strings
   */
  char wildcard();

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
   * Returns true if this model is enabled and the given value is accepted by this models condition.
   * @param columnValue the column value
   * @return true if this model is enabled and the given value is accepted by this models condition
   */
  boolean accepts(Comparable<T> columnValue);

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
   * @param listener a listener to be notified each time the equal value changes
   */
  void addEqualValueListener(EventDataListener<T> listener);

  /**
   * @param listener the listener to remove
   */
  void removeEqualValueListener(EventDataListener<T> listener);

  /**
   * @param listener a listener to be notified each time the equal values change
   */
  void addEqualValuesListener(EventDataListener<Set<T>> listener);

  /**
   * @param listener the listener to remove
   */
  void removeEqualValuesListener(EventDataListener<Set<T>> listener);

  /**
   * @param listener a listener to be notified each time the lower bound changes
   */
  void addLowerBoundListener(EventDataListener<T> listener);

  /**
   * @param listener the listener to remove
   */
  void removeLowerBoundListener(EventDataListener<T> listener);

  /**
   * @param listener a listener to be notified each time the upper bound changes
   */
  void addUpperBoundListener(EventDataListener<T> listener);

  /**
   * @param listener the listener to remove
   */
  void removeUpperBoundListener(EventDataListener<T> listener);

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
  void addChangeListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeChangeListener(EventListener listener);

  /**
   * Returns a new {@link Builder} instance.
   * @param columnIdentifier the column identifier
   * @param columnClass the column class
   * @return a new {@link Builder} instance
   * @param <C> the column identifier type
   * @param <T> the column value type
   */
  static <C, T> Builder<C, T> builder(C columnIdentifier, Class<T> columnClass) {
    return new DefaultColumnConditionModel.DefaultBuilder<>(columnIdentifier, columnClass);
  }

  /**
   * Responsible for creating {@link ColumnConditionModel} instances.
   */
  interface Factory<C> {

    /**
     * Creates a {@link ColumnConditionModel} for a given column
     * @param columnIdentifier the identifier of the column for which to create a {@link ColumnConditionModel}
     * @return a {@link ColumnConditionModel} for the given column, null if filtering should
     * not be allowed for this column
     */
    ColumnConditionModel<? extends C, ?> createConditionModel(C columnIdentifier);
  }

  /**
   * Builds a {@link ColumnConditionModel} instance.
   */
  interface Builder<C, T> {

    /**
     * @param operators the conditional operators available to this condition model
     * @return this builder instance
     */
    Builder<C, T> operators(List<Operator> operators);

    /**
     * @param wildcard the character to use as wildcard
     * @return this builder instance
     */
    Builder<C, T> wildcard(char wildcard);

    /**
     * @param format the format to use when presenting the values, numbers for example
     * @return this builder instance
     */
    Builder<C, T> format(Format format);

    /**
     * @param dateTimePattern the date/time format pattern to use in case of a date/time column
     * @return this builder instance
     */
    Builder<C, T> dateTimePattern(String dateTimePattern);

    /**
     * @param automaticWildcard the automatic wildcard type to use
     * @return this builder instance
     */
    Builder<C, T> automaticWildcard(AutomaticWildcard automaticWildcard);

    /**
     * @param caseSensitive true if the model should be case-sensitive
     * @return this builder instance
     */
    Builder<C, T> caseSensitive(boolean caseSensitive);

    /**
     * @param autoEnable true if the model should auto-enable
     * @return this builder instance
     */
    Builder<C, T> autoEnable(boolean autoEnable);

    /**
     * @return a new {@link ColumnConditionModel} instance based on this builder
     */
    ColumnConditionModel<C, T> build();
  }

  /**
   * @param operator the operator
   * @return a caption for the given operator
   */
  static String caption(Operator operator) {
    switch (requireNonNull(operator)) {
      case EQUAL: return "α =";
      case NOT_EQUAL: return "α ≠";
      case LESS_THAN: return "α <";
      case LESS_THAN_OR_EQUAL: return "α ≤";
      case GREATER_THAN: return "α >";
      case GREATER_THAN_OR_EQUAL: return "α ≥";
      case BETWEEN_EXCLUSIVE: return "< α <";
      case BETWEEN: return "≤ α ≤";
      case NOT_BETWEEN_EXCLUSIVE: return "≥ α ≥";
      case NOT_BETWEEN: return "> α >";
      default:
        throw new IllegalArgumentException("Unknown operator: " + operator);
    }
  }
}
