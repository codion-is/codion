package org.jminor.common.model;

/**
 * User: Björn Darri
 * Date: 29.7.2010
 * Time: 22:18:34
 */
public enum Conjunction {
  AND, OR;

  @Override
  public String toString() {
    switch (this) {
      case AND:
        return " and ";
      case OR:
        return " or ";
      default:
        throw new IllegalArgumentException("Unknown org.jminor.common.model.Conjunction enum");
    }
  }
  }
