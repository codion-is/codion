/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.ui;

import dev.codion.common.db.database.Databases;
import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.local.LocalEntityConnectionProvider;
import dev.codion.framework.domain.Domain;
import dev.codion.swing.framework.model.SwingEntityTableModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntityTableCellRenderersTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  public void test() {
    final EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(TestDomain.T_EMP, CONNECTION_PROVIDER));
    tablePanel.getTableModel().refresh();
    final EntityTableCellRenderer renderer = EntityTableCellRenderers.createTableCellRenderer(tablePanel.getTableModel(),
            DOMAIN.getDefinition(TestDomain.T_EMP).getProperty(TestDomain.EMP_NAME));
    renderer.getTableCellRendererComponent(tablePanel.getTable(), null, false, false, 0, 0);
    renderer.getTableCellRendererComponent(tablePanel.getTable(), null, true, false, 0, 0);
    renderer.getTableCellRendererComponent(tablePanel.getTable(), null, true, true, 0, 0);

    renderer.getTableCellRendererComponent(tablePanel.getTable(), null, false, false, 0, 1);
    renderer.getTableCellRendererComponent(tablePanel.getTable(), null, true, false, 0, 1);
    renderer.getTableCellRendererComponent(tablePanel.getTable(), null, true, true, 0, 1);

    renderer.getTableCellRendererComponent(tablePanel.getTable(), null, false, false, 0, 7);
    renderer.getTableCellRendererComponent(tablePanel.getTable(), null, true, false, 0, 7);
    renderer.getTableCellRendererComponent(tablePanel.getTable(), null, true, true, 0, 7);
  }

  @Test
  public void entityMismatch() {
    final EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(TestDomain.T_EMP, CONNECTION_PROVIDER));
    tablePanel.getTableModel().refresh();
    assertThrows(IllegalArgumentException.class, () -> EntityTableCellRenderers.createTableCellRenderer(tablePanel.getTableModel(),
            DOMAIN.getDefinition(TestDomain.T_DETAIL).getProperty(TestDomain.DEPARTMENT_NAME)));
  }
}
