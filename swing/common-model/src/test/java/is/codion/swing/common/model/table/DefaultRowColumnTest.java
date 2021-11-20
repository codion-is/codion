/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.table;

import is.codion.swing.common.model.table.FilteredTableModel.RowColumn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public final class DefaultRowColumnTest {

  @Test
  void test() {
    final RowColumn rowColumn1 = RowColumn.rowColumn(1, 2);
    assertEquals(1, rowColumn1.getRow());
    assertEquals(2, rowColumn1.getColumn());
    final RowColumn rowColumn2 = RowColumn.rowColumn(3, 4);
    assertEquals(3, rowColumn2.getRow());
    assertEquals(4, rowColumn2.getColumn());

    assertEquals(3, rowColumn1.hashCode());

    assertNotEquals(rowColumn1, rowColumn2);
    assertEquals(rowColumn1, rowColumn1);
    assertEquals(rowColumn1, RowColumn.rowColumn(1, 2));
    assertNotEquals(rowColumn1, RowColumn.rowColumn(2, 1));

    assertEquals("row: 1, column: 2", rowColumn1.toString());
  }
}