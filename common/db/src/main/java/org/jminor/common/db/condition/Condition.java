/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.condition;

import org.jminor.common.db.Column;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A generic interface for objects serving as where conditions in database queries
 * @param <T> the type used to describe the columns involved in the condition
 */
public interface Condition<T extends Column> extends Serializable {
  /**
   * Returns a condition clause based on this Condition, note that this
   * clause contains the ? substitute character instead of the actual values.
   * Note that this method can return an empty string.
   * @return a where clause based on this Condition
   * @see #getValues()
   */
  String getWhereClause();

  /**
   * @return a list of the values this condition is based on, in the order they appear
   * in the condition clause. An empty list is returned in case no values are specified.
   */
  List getValues();

  /**
   * @return a list of T describing the columns this condition is based on, in the same
   * order as their respective values appear in the condition clause.
   * An empty list is returned in case no values are specified.
   */
  List<T> getColumns();

  /**
   * An interface encapsulating a set of Condition objects, that should be either AND'ed or OR'ed together in a query context
   * @param <T> the type used to describe the condition values
   */
  interface Set<T extends Column> extends Condition<T> {

    /**
     * Adds a new Condition object to this set, adding a null condition has no effect
     * @param condition the Condition to add
     */
    void add(final Condition<T> condition);

    /**
     * @return the number of conditions in this set
     */
    int getConditionCount();
  }

  /**
   * For providing dynamic Conditions
   * @param <T> the type used to describe the condition values
   */
  interface Provider<T extends Column> {

    /**
     * @return the Condition
     */
    Condition<T> getCondition();
  }

  /**
   * Enumerating all the possible condition types.
   */
  enum Type {

    LIKE("  = ", "like", Values.MANY),
    NOT_LIKE("  \u2260 ", "not_like", Values.MANY),
    /** Less than or equals*/
    LESS_THAN("  \u2264 ", "less_than", Values.ONE),
    /** Greater than or equals*/
    GREATER_THAN("  \u2265 ", "greater_than", Values.ONE),
    WITHIN_RANGE("\u2265 \u2264", "within_range", Values.TWO),
    OUTSIDE_RANGE("\u2264 \u2265", "outside_range", Values.TWO);

    private final ResourceBundle messages = ResourceBundle.getBundle(Condition.class.getName(), Locale.getDefault());

    private final String caption;
    private final String description;
    private final Values values;

    Type(final String caption, final String descriptionKey, final Values values) {
      this.caption = caption;
      this.description = messages.getString(descriptionKey);
      this.values = values;
    }

    public String getCaption() {
      return caption;
    }

    public String getDescription() {
      return description;
    }

    public Values getValues() {
      return values;
    }

    /**
     * The number of values expected for a Condition.Type
     */
    public enum Values {
      ONE, TWO, MANY
    }
  }
}
