/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.table.DefaultFilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnValueProvider;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.table.TableColumn;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class TableColumnComponentPanelTest {

  private final TableColumn column0 = new TableColumn(0);
  private final TableColumn column1 = new TableColumn(1);
  private final TableColumn column2 = new TableColumn(2);

  public TableColumnComponentPanelTest() {
    column0.setIdentifier(0);
    column1.setIdentifier(1);
    column2.setIdentifier(2);
  }

  @Test
  void wrongColumn() {
    DefaultFilteredTableModel<Object, Integer> tableModel = new DefaultFilteredTableModel<>(asList(column0, column1, column2),
            new ColumnValueProvider<Object, Integer>() {
              @Override
              public Object getValue(Object row, Integer columnIdentifier) {
                return null;
              }

              @Override
              public Class<?> getColumnClass(Integer columnIdentifier) {
                return null;
              }
            });
    FilteredTableColumnModel<Integer> columnModel = tableModel.columnModel();
    Map<TableColumn, JPanel> columnComponents = createColumnComponents(columnModel);
    columnComponents.put(new TableColumn(3), new JPanel());
    assertThrows(IllegalArgumentException.class, () -> new TableColumnComponentPanel<>(columnModel, columnComponents));
  }

  @Test
  void setColumnVisible() {
    DefaultFilteredTableModel<Object, Integer> tableModel = new DefaultFilteredTableModel<>(asList(column0, column1, column2),
            new ColumnValueProvider<Object, Integer>() {
              @Override
              public Object getValue(Object row, Integer columnIdentifier) {
                return null;
              }

              @Override
              public Class<?> getColumnClass(Integer columnIdentifier) {
                return null;
              }
            });
    FilteredTableColumnModel<Integer> columnModel = tableModel.columnModel();
    columnModel.setColumnVisible(1, false);

    TableColumnComponentPanel<JPanel> panel = new TableColumnComponentPanel<>(columnModel, createColumnComponents(columnModel));
    assertTrue(panel.columnComponents().containsKey(column1));

    assertNull(panel.columnComponents().get(column1).getParent());
    columnModel.setColumnVisible(1, true);
    assertNotNull(panel.columnComponents().get(column1).getParent());
    columnModel.setColumnVisible(2, false);
    assertNull(panel.columnComponents().get(column2).getParent());
    columnModel.setColumnVisible(2, true);
    assertNotNull(panel.columnComponents().get(column2).getParent());
  }

  @Test
  void width() {
    DefaultFilteredTableModel<Object, Integer> tableModel = new DefaultFilteredTableModel<>(asList(column0, column1, column2),
            new ColumnValueProvider<Object, Integer>() {
              @Override
              public Object getValue(Object row, Integer columnIdentifier) {
                return null;
              }

              @Override
              public Class<?> getColumnClass(Integer columnIdentifier) {
                return null;
              }
            });
    FilteredTableColumnModel<Integer> columnModel = tableModel.columnModel();
    TableColumnComponentPanel<JPanel> panel = new TableColumnComponentPanel<>(columnModel, createColumnComponents(columnModel));
    column0.setWidth(100);
    assertEquals(100, panel.columnComponents().get(column0).getPreferredSize().width);
    column1.setWidth(90);
    assertEquals(90, panel.columnComponents().get(column1).getPreferredSize().width);
    column2.setWidth(80);
    assertEquals(80, panel.columnComponents().get(column2).getPreferredSize().width);
  }

  private static Map<TableColumn, JPanel> createColumnComponents(FilteredTableColumnModel<?> columnModel) {
    Map<TableColumn, JPanel> columnComponents = new HashMap<>();
    columnModel.columns().forEach(column -> columnComponents.put(column, new JPanel()));

    return columnComponents;
  }
}
