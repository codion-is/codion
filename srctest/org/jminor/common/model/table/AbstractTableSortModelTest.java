package org.jminor.common.model.table;

import org.junit.Test;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AbstractTableSortModelTest {

  @Test
  public void test() {
    final TableColumn firstColumn = new TableColumn(0);
    firstColumn.setIdentifier(0);
    final TableColumn secondColumn = new TableColumn(1);
    secondColumn.setIdentifier(1);
    final TableColumn thirdColumn = new TableColumn(2);
    thirdColumn.setIdentifier(2);
    final TestTableSortModel model = new TestTableSortModel(Arrays.asList(firstColumn, secondColumn, thirdColumn));

    final Row firstRow = new Row(1, 2, null);
    final Row secondRow = new Row(1, 2, 5);
    final Row thirdRow = new Row(1, 3, 6);
    final List<Row> items = Arrays.asList(firstRow, secondRow, thirdRow);

    model.sort(items);
    assertEquals(0, items.indexOf(firstRow));
    assertEquals(1, items.indexOf(secondRow));
    assertEquals(2, items.indexOf(thirdRow));

    model.setSortingDirective(0, SortingDirective.ASCENDING, false);
    model.sort(items);
    assertEquals(0, items.indexOf(firstRow));
    assertEquals(1, items.indexOf(secondRow));
    assertEquals(2, items.indexOf(thirdRow));

    model.setSortingDirective(2, SortingDirective.ASCENDING, false);
    model.sort(items);
    assertEquals(0, items.indexOf(firstRow));
    assertEquals(1, items.indexOf(secondRow));
    assertEquals(2, items.indexOf(thirdRow));

    model.setSortingDirective(0, SortingDirective.ASCENDING, false);
    model.setSortingDirective(1, SortingDirective.DESCENDING, true);
    model.sort(items);
    assertEquals(0, items.indexOf(thirdRow));
    assertEquals(1, items.indexOf(firstRow));
    assertEquals(2, items.indexOf(secondRow));

    model.setSortingDirective(2, SortingDirective.DESCENDING, true);
    model.sort(items);
    assertEquals(0, items.indexOf(thirdRow));
    assertEquals(1, items.indexOf(secondRow));
    assertEquals(2, items.indexOf(firstRow));

    model.setSortingDirective(2, SortingDirective.ASCENDING, true);
    model.sort(items);
    assertEquals(0, items.indexOf(thirdRow));
    assertEquals(1, items.indexOf(firstRow));
    assertEquals(2, items.indexOf(secondRow));

    model.setSortingDirective(2, SortingDirective.ASCENDING, false);
    model.sort(items);
    assertEquals(0, items.indexOf(firstRow));
    assertEquals(1, items.indexOf(secondRow));
    assertEquals(2, items.indexOf(thirdRow));
  }

  @Test(expected = IllegalArgumentException.class)
  public void nonComparableColumnClass() {
    final TableColumn column = new TableColumn(0);
    column.setIdentifier(0);
    final AbstractTableSortModel<ArrayList, Integer> model = new AbstractTableSortModel<ArrayList, Integer>(Arrays.asList(column)) {
      @Override
      protected Class getColumnClass(final Integer columnIdentifier) {
        return ArrayList.class;
      }

      @Override
      protected Comparable getComparable(final ArrayList rowObject, final Integer columnIdentifier) {
        return rowObject.toString();
      }
    };
    final List<ArrayList> collections = Arrays.asList(new ArrayList(), new ArrayList());
    model.setSortingDirective(0, SortingDirective.DESCENDING, false);
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

    private TestTableSortModel(final List<TableColumn> columns) {
      super(columns);
    }

    @Override
    protected Class getColumnClass(final Integer columnIdentifier) {
      if (columnIdentifier.equals(1)) {
        return String.class;
      }

      return Integer.class;
    }

    @Override
    protected Comparable getComparable(final Row rowObject, final Integer columnIdentifier) {
      switch (columnIdentifier) {
        case 0:
          return rowObject.firstValue;
        case 1:
          return rowObject.secondValue.toString();
        case 2:
          return rowObject.thirdValue;
        default:
          return null;
      }
    }
  }
}
