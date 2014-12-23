/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * AND / OR
 */
public enum Conjunction {
  AND, OR;

  /**
   * @return AND: " and " OR: " or "
   */
  @Override
  public String toString() {
    if (equals(AND)) {
      return " and ";
    }
    else {
      return " or ";
    }
  }
}
