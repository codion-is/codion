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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.model.component.table.FilteredTableModel;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.util.HashMap;
import java.util.Map;

import static is.codion.swing.common.model.component.table.FilteredTableColumn.filteredTableColumn;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class FilteredTableColumnComponentPanelTest {

  private final FilteredTableColumn<Integer> column0 = filteredTableColumn(0);
  private final FilteredTableColumn<Integer> column1 = filteredTableColumn(1);
  private final FilteredTableColumn<Integer> column2 = filteredTableColumn(2);

  @Test
  void wrongColumn() {
    FilteredTableModel<Object, Integer> tableModel =
            FilteredTableModel.<Object, Integer>builder(() -> asList(column0, column1, column2), (row, columnIdentifier) -> null)
                    .build();
    FilteredTableColumnModel<Integer> columnModel = tableModel.columnModel();
    Map<Integer, JPanel> columnComponents = createColumnComponents(columnModel);
    columnComponents.put(3, new JPanel());
    assertThrows(IllegalArgumentException.class, () -> FilteredTableColumnComponentPanel.filteredTableColumnComponentPanel(columnModel, columnComponents));
  }

  @Test
  void setColumnVisible() {
    FilteredTableModel<Object, Integer> tableModel =
            FilteredTableModel.<Object, Integer>builder(() -> asList(column0, column1, column2), (row, columnIdentifier) -> null)
                    .build();
    FilteredTableColumnModel<Integer> columnModel = tableModel.columnModel();
    columnModel.visible(1).set(false);

    FilteredTableColumnComponentPanel<Integer, JPanel> panel = FilteredTableColumnComponentPanel.filteredTableColumnComponentPanel(columnModel, createColumnComponents(columnModel));
    assertTrue(panel.components().containsKey(1));

    assertNull(panel.components().get(1).getParent());
    columnModel.visible(1).set(true);
    assertNotNull(panel.components().get(1).getParent());
    columnModel.visible(2).set(false);
    assertNull(panel.components().get(2).getParent());
    columnModel.visible(2).set(true);
    assertNotNull(panel.components().get(2).getParent());
  }

  @Test
  void width() {
    FilteredTableModel<Object, Integer> tableModel =
            FilteredTableModel.<Object, Integer>builder(() -> asList(column0, column1, column2), (row, columnIdentifier) -> null)
                    .build();
    FilteredTableColumnModel<Integer> columnModel = tableModel.columnModel();
    FilteredTableColumnComponentPanel<Integer, JPanel> panel = FilteredTableColumnComponentPanel.filteredTableColumnComponentPanel(columnModel, createColumnComponents(columnModel));
    column0.setWidth(100);
    assertEquals(100, panel.components().get(0).getPreferredSize().width);
    column1.setWidth(90);
    assertEquals(90, panel.components().get(1).getPreferredSize().width);
    column2.setWidth(80);
    assertEquals(80, panel.components().get(2).getPreferredSize().width);
  }

  private static Map<Integer, JPanel> createColumnComponents(FilteredTableColumnModel<Integer> columnModel) {
    Map<Integer, JPanel> columnComponents = new HashMap<>();
    columnModel.columns().forEach(column -> columnComponents.put(column.getIdentifier(), new JPanel()));

    return columnComponents;
  }
}
