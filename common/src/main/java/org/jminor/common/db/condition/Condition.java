/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.condition;

import org.jminor.common.i18n.Messages;

import java.util.List;

/**
 * A generic interface for objects serving as where conditions in database queries
 * @param <T> the type used to describe the condition values
 */
public interface Condition<T> {
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
   * @return a list of T describing the values this condition is based on, in the same
   * order as their respective values appear in the condition clause.
   * An empty list is returned in case no values are specified.
   */
  List<T> getValueKeys();

  /**
   * An interface encapsulating a set of Condition objects, that should be either AND'ed or OR'ed together in a query context
   * @param <T> the type used to describe the condition values
   */
  interface Set<T> extends Condition<T> {

    /**
     * Adds a new Condition object to this set, adding a null condition has no effect
     * @param condition the Condition to add
     */
    void add(final Condition<T> condition);

    /**
     * @return the number of condition in this set
     */
    int getConditionCount();
  }

  /**
   * Enumerating all the possible ways of searching.
   */
  enum Type {

    LIKE("  = ", Messages.get(Messages.LIKE), Values.MANY),
    NOT_LIKE("  \u2260 ", Messages.get(Messages.NOT_LIKE), Values.MANY),
    /** Less than or equals*/
    LESS_THAN("  \u2264 ", Messages.get(Messages.LESS_THAN), Values.ONE),
    /** Greater than or equals*/
    GREATER_THAN("  \u2265 ", Messages.get(Messages.GREATER_THAN), Values.ONE),
    WITHIN_RANGE("\u2265 \u2264", Messages.get(Messages.WITHIN_RANGE), Values.TWO),
    OUTSIDE_RANGE("\u2264 \u2265", Messages.get(Messages.OUTSIDE_RANGE), Values.TWO);

    private final String caption;
    private final String description;
    private final Values values;

    Type(final String caption, final String description, final Values values) {
      this.caption = caption;
      this.description = description;
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
     * The number of values expected for a ConditionType
     */
    public enum Values {
      ONE, TWO, MANY
    }
  }
}
