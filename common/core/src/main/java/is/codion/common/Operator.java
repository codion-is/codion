/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.util.ResourceBundle;

/**
 * Enumerating all the available operator types.
 */
public enum Operator {

  EQUAL,
  NOT_EQUAL,
  LESS_THAN,
  LESS_THAN_OR_EQUAL,
  GREATER_THAN,
  GREATER_THAN_OR_EQUAL,
  BETWEEN_EXCLUSIVE,
  BETWEEN,
  NOT_BETWEEN_EXCLUSIVE,
  NOT_BETWEEN;

  private final String description;

  Operator() {
    this.description = ResourceBundle.getBundle(Operator.class.getName()).getString(name().toLowerCase());
  }

  public String description() {
    return description;
  }
}
