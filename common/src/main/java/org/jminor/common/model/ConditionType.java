/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jminor.common.i18n.Messages;

/**
 * Enumerating all the possible ways of searching.
 */
public enum ConditionType {

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

  ConditionType(final String caption, final String description, final Values values) {
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
