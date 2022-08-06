/*
 * Copyright (c) 2013 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.Domain;
import is.codion.swing.framework.model.SwingEntityTableModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefauitEntityTableCellRendererTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void test() {
    EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(TestDomain.T_EMP, CONNECTION_PROVIDER));
    tablePanel.tableModel().refresh();
    EntityTableCellRenderer renderer = EntityTableCellRenderer.builder(tablePanel.tableModel(),
            DOMAIN.entities().getDefinition(TestDomain.T_EMP).property(TestDomain.EMP_NAME))
            .build();
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
    EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(TestDomain.T_EMP, CONNECTION_PROVIDER));
    tablePanel.tableModel().refresh();
    assertThrows(IllegalArgumentException.class, () -> EntityTableCellRenderer.builder(tablePanel.tableModel(),
            DOMAIN.entities().getDefinition(TestDomain.T_DETAIL).property(TestDomain.DEPARTMENT_NAME)));
  }
}
