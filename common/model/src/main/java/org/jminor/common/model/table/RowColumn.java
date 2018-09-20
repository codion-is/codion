package org.jminor.common.model.table;

/**
 * Holds a row/column combination
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
}
