/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

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
  BETWEEN_EXCLUSIVE("<\u2219<"),
  BETWEEN("\u2264\u2219\u2264"),
  NOT_BETWEEN_EXCLUSIVE("\u2265\u2219\u2265"),
  NOT_BETWEEN(">\u2219>");

  private final ResourceBundle messages = ResourceBundle.getBundle(Operator.class.getName());

  private final String caption;
  private final String description;

  Operator(String caption) {
    this.caption = caption;
    this.description = messages.getString(name().toLowerCase());
  }

  @Override
  public String toString() {
    return caption;
  }

  public String caption() {
    return caption;
  }

  public String description() {
    return description;
  }
}
