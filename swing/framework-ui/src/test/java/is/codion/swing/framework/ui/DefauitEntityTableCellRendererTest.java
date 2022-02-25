/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
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
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  void test() {
    EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(TestDomain.T_EMP, CONNECTION_PROVIDER));
    tablePanel.getTableModel().refresh();
    EntityTableCellRenderer renderer = EntityTableCellRenderer.builder(tablePanel.getTableModel(),
            DOMAIN.getEntities().getDefinition(TestDomain.T_EMP).getProperty(TestDomain.EMP_NAME))
            .build();
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
  void entityMismatch() {
    EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(TestDomain.T_EMP, CONNECTION_PROVIDER));
    tablePanel.getTableModel().refresh();
    assertThrows(IllegalArgumentException.class, () -> EntityTableCellRenderer.builder(tablePanel.getTableModel(),
            DOMAIN.getEntities().getDefinition(TestDomain.T_DETAIL).getProperty(TestDomain.DEPARTMENT_NAME)));
  }
}
