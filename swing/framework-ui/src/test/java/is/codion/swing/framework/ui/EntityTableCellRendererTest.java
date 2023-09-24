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
 * Copyright (c) 2013 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntityTableCellRendererTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void test() {
    EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER));
    tablePanel.tableModel().refresh();
    FilteredTableCellRenderer renderer = EntityTableCellRenderer.builder(tablePanel.tableModel(), Employee.NAME).build();
    renderer.getTableCellRendererComponent(tablePanel.table(), null, false, false, 0, 0);
    renderer.getTableCellRendererComponent(tablePanel.table(), null, true, false, 0, 0);
    renderer.getTableCellRendererComponent(tablePanel.table(), null, true, true, 0, 0);

    renderer.getTableCellRendererComponent(tablePanel.table(), null, false, false, 0, 1);
    renderer.getTableCellRendererComponent(tablePanel.table(), null, true, false, 0, 1);
    renderer.getTableCellRendererComponent(tablePanel.table(), null, true, true, 0, 1);

    renderer.getTableCellRendererComponent(tablePanel.table(), null, false, false, 0, 7);
    renderer.getTableCellRendererComponent(tablePanel.table(), null, true, false, 0, 7);
    renderer.getTableCellRendererComponent(tablePanel.table(), null, true, true, 0, 7);
  }

  @Test
  void entityMismatch() {
    EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(Employee.TYPE, CONNECTION_PROVIDER));
    tablePanel.tableModel().refresh();
    assertThrows(IllegalArgumentException.class, () -> EntityTableCellRenderer.builder(tablePanel.tableModel(), Department.NAME));
  }
}
