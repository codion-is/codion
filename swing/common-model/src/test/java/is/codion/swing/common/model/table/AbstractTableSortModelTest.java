/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.table;

import org.junit.jupiter.api.Test;

import javax.swing.SortOrder;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractTableSortModelTest {

  @Test
  void test() {
    final TableColumn firstColumn = new TableColumn(0);
    firstColumn.setIdentifier(0);
    final TableColumn secondColumn = new TableColumn(1);
    secondColumn.setIdentifier(1);
    final TableColumn thirdColumn = new TableColumn(2);
    thirdColumn.setIdentifier(2);
    final TestTableSortModel model = new TestTableSortModel();

    final Row firstRow = new Row(1, 2, null);
    final Row secondRow = new Row(1, 2, 5);
    final Row thirdRow = new Row(1, 3, 6);
    final List<Row> items = asList(firstRow, secondRow, thirdRow);

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
  }

  @Test
  void nonComparableColumnClass() {
    final AbstractTableSortModel<ArrayList, Integer> model = new AbstractTableSortModel<ArrayList, Integer>() {
      @Override
      public Class getColumnClass(final Integer columnIdentifier) {
        return ArrayList.class;
      }

      @Override
      protected Object getColumnValue(final ArrayList row, final Integer columnIdentifier) {
        return row.toString();
      }
    };
    final List<ArrayList> collections = asList(new ArrayList(), new ArrayList());
    model.setSortOrder(0, SortOrder.DESCENDING);
    model.sort(collections);
  }

  private static final class Row {
    private final Integer firstValue;
    private final Column secondValue;
    private final Integer thirdValue;

    private Row(final Integer firstValue, final Integer secondValue, final Integer thirdValue) {
      this.firstValue = firstValue;
      this.secondValue = new Column(secondValue);
      this.thirdValue = thirdValue;
    }
  }

  private static final class Column {
    private final Integer value;

    private Column(final Integer value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value.toString();
    }
  }

  private static final class TestTableSortModel extends AbstractTableSortModel<Row, Integer> {

    @Override
    public Class<? extends Object> getColumnClass(final Integer columnIdentifier) {
      if (columnIdentifier.equals(1)) {
        return String.class;
      }

      return Integer.class;
    }

    @Override
    protected Object getColumnValue(final Row row, final Integer columnIdentifier) {
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
    }
  }
}
