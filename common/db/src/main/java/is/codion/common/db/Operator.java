/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db;

import java.util.ResourceBundle;

/**
 * Enumerating all the available operator types.
 */
public enum Operator {

  EQUAL("=", Values.MANY),
  NOT_EQUAL("\u2260", Values.MANY),
  LESS_THAN("<", Values.ONE),
  LESS_THAN_OR_EQUAL("\u2264", Values.ONE),
  GREATER_THAN(">", Values.ONE),
  GREATER_THAN_OR_EQUAL("\u2265", Values.ONE),
  WITHIN_RANGE("> <", Values.TWO),
  WITHIN_RANGE_INCLUSIVE("\u2265 \u2264", Values.TWO),
  OUTSIDE_RANGE("< >", Values.TWO),
  OUTSIDE_RANGE_INCLUSIVE("\u2264 \u2265", Values.TWO);

  private final ResourceBundle messages = ResourceBundle.getBundle(Operator.class.getName());

  private final String caption;
  private final String description;
  private final Values values;

  Operator(final String caption, final Values values) {
    this.caption = caption;
    this.description = messages.getString(name().toLowerCase());
    this.values = values;
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

  /**
   * The number of values expected for a operator
   */
  public enum Values {
    ONE, TWO, MANY
  }
}
