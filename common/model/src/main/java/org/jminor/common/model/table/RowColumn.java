package org.jminor.common.model.table;

/**
 * Holds a row/column coordinate
 */
public interface RowColumn {

  /**
   * @return the row
   */
  int getRow();

  /**
   * @return the column
   */
  int getColumn();

  /**
   * Factory method for {@link RowColumn} instances.
   * @param row the row index
   * @param column the column index
   * @return the RowColumn
   */
  static RowColumn rowColumn(final int row, final int column) {
    return new DefaultRowColumn(row, column);
  }
}
