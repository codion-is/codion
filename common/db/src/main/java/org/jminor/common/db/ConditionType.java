/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Enumerating all the available condition types.
 */
public enum ConditionType {

  LIKE("  = ", "like", Values.MANY),
  NOT_LIKE("  \u2260 ", "not_like", Values.MANY),
  /** Less than or equals*/
  LESS_THAN("  \u2264 ", "less_than", Values.ONE),
  /** Greater than or equals*/
  GREATER_THAN("  \u2265 ", "greater_than", Values.ONE),
  WITHIN_RANGE("\u2265 \u2264", "within_range", Values.TWO),
  OUTSIDE_RANGE("\u2264 \u2265", "outside_range", Values.TWO);

  private final ResourceBundle messages = ResourceBundle.getBundle(ConditionType.class.getName(), Locale.getDefault());

  private final String caption;
  private final String description;
  private final Values values;

  ConditionType(final String caption, final String descriptionKey, final Values values) {
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
