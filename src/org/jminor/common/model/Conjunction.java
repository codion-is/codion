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
