/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

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
