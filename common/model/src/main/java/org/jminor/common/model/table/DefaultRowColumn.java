/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

final class DefaultRowColumn implements RowColumn {

  private final int row;
  private final int column;

  DefaultRowColumn(final int row, final int column) {
    this.row = row;
    this.column = column;
  }

  @Override
  public int getRow() {
    return row;
  }

  @Override
  public int getColumn() {
    return column;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }

    return obj instanceof DefaultRowColumn &&
            ((DefaultRowColumn) obj).getRow() == getRow() &&
            ((DefaultRowColumn) obj).getColumn() == getColumn();
  }

  @Override
  public int hashCode() {
    return row + column;
  }

  @Override
  public String toString() {
    return "row: " + row + ", column: " + column;
  }
}
