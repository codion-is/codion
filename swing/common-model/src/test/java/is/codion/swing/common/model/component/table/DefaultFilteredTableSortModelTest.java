/*
 * Copyright (c) 2014 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnValueProvider;

import org.junit.jupiter.api.Test;

import javax.swing.SortOrder;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultFilteredTableSortModelTest {

  @Test
  void test() {
    TableColumn firstColumn = new TableColumn(0);
    firstColumn.setIdentifier(0);
    TableColumn secondColumn = new TableColumn(1);
    secondColumn.setIdentifier(1);
    TableColumn thirdColumn = new TableColumn(2);
    thirdColumn.setIdentifier(2);
    DefaultFilteredTableSortModel<Row, Integer> model = new DefaultFilteredTableSortModel<>(new ColumnValueProvider<Row, Integer>() {
      @Override
      public Object value(Row row, Integer columnIdentifier) {
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

      @Override
      public Class<?> columnClass(Integer columnIdentifier) {
        return String.class;
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
  }

  @Test
  void nonComparableColumnClass() {
    TableColumn firstColumn = new TableColumn(0);
    firstColumn.setIdentifier(0);
    DefaultFilteredTableSortModel<ArrayList, Integer> model = new DefaultFilteredTableSortModel<>(
            new ColumnValueProvider<ArrayList, Integer>() {
              @Override
              public Object value(ArrayList row, Integer columnIdentifier) {
                return row.toString();
              }

              @Override
              public Class<?> columnClass(Integer columnIdentifier) {
                return ArrayList.class;
              }
            });
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
