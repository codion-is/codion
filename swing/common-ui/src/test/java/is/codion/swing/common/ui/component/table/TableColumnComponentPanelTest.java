/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.table.DefaultFilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnValueProvider;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.util.HashMap;
import java.util.Map;

import static is.codion.swing.common.model.component.table.FilteredTableColumn.filteredTableColumn;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class TableColumnComponentPanelTest {

  private final FilteredTableColumn<Integer> column0 = filteredTableColumn(0);
  private final FilteredTableColumn<Integer> column1 = filteredTableColumn(1);
  private final FilteredTableColumn<Integer> column2 = filteredTableColumn(2);

  @Test
  void wrongColumn() {
    DefaultFilteredTableModel<Object, Integer> tableModel = new DefaultFilteredTableModel<>(asList(column0, column1, column2),
            new ColumnValueProvider<Object, Integer>() {
              @Override
              public Object value(Object row, Integer columnIdentifier) {
                return null;
              }

              @Override
              public Class<?> columnClass(Integer columnIdentifier) {
                return null;
              }
            });
    FilteredTableColumnModel<Integer> columnModel = tableModel.columnModel();
    Map<Integer, JPanel> columnComponents = createColumnComponents(columnModel);
    columnComponents.put(3, new JPanel());
    assertThrows(IllegalArgumentException.class, () -> TableColumnComponentPanel.tableColumnComponentPanel(columnModel, columnComponents));
  }

  @Test
  void setColumnVisible() {
    DefaultFilteredTableModel<Object, Integer> tableModel = new DefaultFilteredTableModel<>(asList(column0, column1, column2),
            new ColumnValueProvider<Object, Integer>() {
              @Override
              public Object value(Object row, Integer columnIdentifier) {
                return null;
              }

              @Override
              public Class<?> columnClass(Integer columnIdentifier) {
                return null;
              }
            });
    FilteredTableColumnModel<Integer> columnModel = tableModel.columnModel();
    columnModel.setColumnVisible(1, false);

    TableColumnComponentPanel<Integer, JPanel> panel = TableColumnComponentPanel.tableColumnComponentPanel(columnModel, createColumnComponents(columnModel));
    assertTrue(panel.columnComponents().containsKey(1));

    assertNull(panel.columnComponents().get(1).getParent());
    columnModel.setColumnVisible(1, true);
    assertNotNull(panel.columnComponents().get(1).getParent());
    columnModel.setColumnVisible(2, false);
    assertNull(panel.columnComponents().get(2).getParent());
    columnModel.setColumnVisible(2, true);
    assertNotNull(panel.columnComponents().get(2).getParent());
  }

  @Test
  void width() {
    DefaultFilteredTableModel<Object, Integer> tableModel = new DefaultFilteredTableModel<>(asList(column0, column1, column2),
            new ColumnValueProvider<Object, Integer>() {
              @Override
              public Object value(Object row, Integer columnIdentifier) {
                return null;
              }

              @Override
              public Class<?> columnClass(Integer columnIdentifier) {
                return null;
              }
            });
    FilteredTableColumnModel<Integer> columnModel = tableModel.columnModel();
    TableColumnComponentPanel<Integer, JPanel> panel = TableColumnComponentPanel.tableColumnComponentPanel(columnModel, createColumnComponents(columnModel));
    column0.setWidth(100);
    assertEquals(100, panel.columnComponents().get(0).getPreferredSize().width);
    column1.setWidth(90);
    assertEquals(90, panel.columnComponents().get(1).getPreferredSize().width);
    column2.setWidth(80);
    assertEquals(80, panel.columnComponents().get(2).getPreferredSize().width);
  }

  private static Map<Integer, JPanel> createColumnComponents(FilteredTableColumnModel<Integer> columnModel) {
    Map<Integer, JPanel> columnComponents = new HashMap<>();
    columnModel.columns().forEach(column -> columnComponents.put(column.getIdentifier(), new JPanel()));

    return columnComponents;
  }
}
