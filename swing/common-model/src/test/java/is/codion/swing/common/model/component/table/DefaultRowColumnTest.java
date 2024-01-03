/*
 * Copyright (c) 2018 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.swing.common.model.component.table.FilteredTableSearchModel.RowColumn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class DefaultRowColumnTest {

  @Test
  void test() {
    RowColumn rowColumn1 = new DefaultFilteredTableSearchModel.DefaultRowColumn(1, 2);
    assertEquals(1, rowColumn1.row());
    assertEquals(2, rowColumn1.column());
    RowColumn rowColumn2 = new DefaultFilteredTableSearchModel.DefaultRowColumn(3, 4);
    assertEquals(3, rowColumn2.row());
    assertEquals(4, rowColumn2.column());
    assertTrue(rowColumn2.equals(3, 4));
    assertFalse(rowColumn2.equals(1, 2));

    assertEquals(3, rowColumn1.hashCode());

    assertNotEquals(rowColumn1, rowColumn2);
    assertEquals(rowColumn1, rowColumn1);
    assertEquals(rowColumn1, new DefaultFilteredTableSearchModel.DefaultRowColumn(1, 2));
    assertNotEquals(rowColumn1, new DefaultFilteredTableSearchModel.DefaultRowColumn(2, 1));

    assertEquals("row: 1, column: 2", rowColumn1.toString());
  }
}
