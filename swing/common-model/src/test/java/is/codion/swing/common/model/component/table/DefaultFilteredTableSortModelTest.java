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
 * Copyright (c) 2014 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import org.junit.jupiter.api.Test;

import javax.swing.SortOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultFilteredTableSortModelTest {

  @Test
  void test() {
    FilteredTableColumn<Integer> firstColumn = FilteredTableColumn.builder(0)
            .columnClass(String.class)
            .build();
    FilteredTableColumn<Integer> secondColumn = FilteredTableColumn.builder(1)
            .columnClass(String.class)
            .build();
    FilteredTableColumn<Integer> thirdColumn = FilteredTableColumn.builder(2)
            .columnClass(String.class)
            .build();
    FilteredTableColumnModel<Integer> columnModel = new DefaultFilteredTableColumnModel<>(Arrays.asList(firstColumn, secondColumn, thirdColumn));
    DefaultFilteredTableSortModel<Row, Integer> model = new DefaultFilteredTableSortModel<>(columnModel, (row, columnIdentifier) -> {
      switch (columnIdentifier) {
        case 0:
          return row.firstValue;
        case 1:
          return row.secondValue.toString();
        case 2:
          return row.thirdValue;
        default:
          return null;
      }
    });

    Row firstRow = new Row(1, 2, null);
    Row secondRow = new Row(1, 2, 5);
    Row thirdRow = new Row(1, 3, 6);
    List<Row> items = asList(firstRow, secondRow, thirdRow);

    model.sort(items);
    assertEquals(0, items.indexOf(firstRow));
    assertEquals(1, items.indexOf(secondRow));
    assertEquals(2, items.indexOf(thirdRow));

    model.setSortOrder(0, SortOrder.ASCENDING);
    model.sort(items);
    assertEquals(0, items.indexOf(firstRow));
    assertEquals(1, items.indexOf(secondRow));
    assertEquals(2, items.indexOf(thirdRow));

    model.setSortOrder(2, SortOrder.ASCENDING);
    model.sort(items);
    assertEquals(0, items.indexOf(firstRow));
    assertEquals(1, items.indexOf(secondRow));
    assertEquals(2, items.indexOf(thirdRow));

    model.setSortOrder(0, SortOrder.ASCENDING);
    model.addSortOrder(1, SortOrder.DESCENDING);
    model.sort(items);
    assertEquals(0, items.indexOf(thirdRow));
    assertEquals(1, items.indexOf(firstRow));
    assertEquals(2, items.indexOf(secondRow));

    model.addSortOrder(2, SortOrder.DESCENDING);
    model.sort(items);
    assertEquals(0, items.indexOf(thirdRow));
    assertEquals(1, items.indexOf(secondRow));
    assertEquals(2, items.indexOf(firstRow));

    model.addSortOrder(2, SortOrder.ASCENDING);
    model.sort(items);
    assertEquals(0, items.indexOf(thirdRow));
    assertEquals(1, items.indexOf(firstRow));
    assertEquals(2, items.indexOf(secondRow));

    model.setSortOrder(2, SortOrder.ASCENDING);
    model.sort(items);
    assertEquals(0, items.indexOf(firstRow));
    assertEquals(1, items.indexOf(secondRow));
    assertEquals(2, items.indexOf(thirdRow));

    model.clear();
    model.setSortOrder(2, SortOrder.ASCENDING);
    model.setSortingEnabled(2, false);
    assertFalse(model.isSortingEnabled(2));
    assertSame(SortOrder.UNSORTED, model.sortOrder(2));
    assertThrows(IllegalStateException.class, () -> model.setSortOrder(2, SortOrder.DESCENDING));
    model.setSortingEnabled(2, true);
    assertTrue(model.isSortingEnabled(2));
    model.setSortOrder(2, SortOrder.DESCENDING);
    assertSame(model.sortOrder(2), SortOrder.DESCENDING);
  }

  @Test
  void nonComparableColumnClass() {
    FilteredTableColumn<Integer> firstColumn = FilteredTableColumn.builder(0)
            .columnClass(ArrayList.class)
            .build();
    FilteredTableColumnModel<Integer> columnModel = new DefaultFilteredTableColumnModel<>(Collections.singletonList(firstColumn));
    DefaultFilteredTableSortModel<ArrayList, Integer> model = new DefaultFilteredTableSortModel<>(columnModel, (row, columnIdentifier) -> row.toString());
    List<ArrayList> collections = asList(new ArrayList(), new ArrayList());
    model.setSortOrder(0, SortOrder.DESCENDING);
    model.sort(collections);
  }

  private static final class Row {
    private final Integer firstValue;
    private final Column secondValue;
    private final Integer thirdValue;

    private Row(Integer firstValue, Integer secondValue, Integer thirdValue) {
      this.firstValue = firstValue;
      this.secondValue = new Column(secondValue);
      this.thirdValue = thirdValue;
    }
  }

  private static final class Column {
    private final Integer value;

    private Column(Integer value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value.toString();
    }
  }
}
