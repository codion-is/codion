/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db;

import java.util.ResourceBundle;

/**
 * Enumerating all the available operator types.
 */
public enum Operator {

  EQUALS("=", "equals", Values.MANY, NullCompatible.YES),
  NOT_EQUALS("\u2260", "not_equals", Values.MANY, NullCompatible.YES),
  /** Less than or equals*/
  LESS_THAN("\u2264", "less_than", Values.ONE, NullCompatible.NO),
  /** Greater than or equals*/
  GREATER_THAN("\u2265", "greater_than", Values.ONE, NullCompatible.NO),
  WITHIN_RANGE("\u2265 \u2264", "within_range", Values.TWO, NullCompatible.NO),
  OUTSIDE_RANGE("\u2264 \u2265", "outside_range", Values.TWO, NullCompatible.NO);

  private final ResourceBundle messages = ResourceBundle.getBundle(Operator.class.getName());

  private final String caption;
  private final String description;
  private final Values values;
  private final boolean nullCompatible;

  Operator(final String caption, final String descriptionKey, final Values values, final NullCompatible nullCompatible) {
    this.caption = caption;
    this.description = messages.getString(descriptionKey);
    this.values = values;
    this.nullCompatible = nullCompatible == NullCompatible.YES;
  }

  @Override
  public String toString() {
    return caption;
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

  public boolean isNullCompatible() {
    return nullCompatible;
  }

  /**
   * The number of values expected for a operator
   */
  public enum Values {
    ONE, TWO, MANY
  }

  /**
   * Specifies whether an operator can handle null values.
   */
  public enum NullCompatible {
    YES, NO
  }
}
