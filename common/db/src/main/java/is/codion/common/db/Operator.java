/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db;

import java.util.ResourceBundle;

/**
 * Enumerating all the available operator types.
 */
public enum Operator {

  EQUAL("="),
  NOT_EQUAL("\u2260"),
  LESS_THAN("<"),
  LESS_THAN_OR_EQUAL("\u2264"),
  GREATER_THAN(">"),
  GREATER_THAN_OR_EQUAL("\u2265"),
  BETWEEN_EXCLUSIVE("> <"),
  BETWEEN("\u2265 \u2264"),
  NOT_BETWEEN_EXCLUSIVE("< >"),
  NOT_BETWEEN("\u2264 \u2265");

  private final ResourceBundle messages = ResourceBundle.getBundle(Operator.class.getName());

  private final String caption;
  private final String description;

  Operator(final String caption) {
    this.caption = caption;
    this.description = messages.getString(name().toLowerCase());
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
}
