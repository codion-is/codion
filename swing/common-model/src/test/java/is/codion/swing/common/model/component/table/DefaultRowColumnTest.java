/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2018 - 2024, Björn Darri Sigurðsson.
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
