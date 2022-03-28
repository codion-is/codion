/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

final class DefaultRowColumn implements FilteredTableModel.RowColumn {

  private final int row;
  private final int column;

  DefaultRowColumn(int row, int column) {
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
  public boolean equals(Object obj) {
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
