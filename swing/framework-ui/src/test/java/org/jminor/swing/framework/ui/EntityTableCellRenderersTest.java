/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.swing.framework.model.SwingEntityTableModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntityTableCellRenderersTest {

  private static final Entities ENTITIES = new TestDomain();

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(ENTITIES, new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray()), Databases.getInstance());

  @Test
  public void test() {
    final EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(TestDomain.T_EMP, CONNECTION_PROVIDER));
    tablePanel.getEntityTableModel().refresh();
    final EntityTableCellRenderer renderer = EntityTableCellRenderers.getTableCellRenderer(tablePanel.getEntityTableModel(),
            ENTITIES.getProperty(TestDomain.T_EMP, TestDomain.EMP_NAME));
    renderer.getTableCellRendererComponent(tablePanel.getJTable(), null, false, false, 0, 0);
    renderer.getTableCellRendererComponent(tablePanel.getJTable(), null, true, false, 0, 0);
    renderer.getTableCellRendererComponent(tablePanel.getJTable(), null, true, true, 0, 0);

    renderer.getTableCellRendererComponent(tablePanel.getJTable(), null, false, false, 0, 1);
    renderer.getTableCellRendererComponent(tablePanel.getJTable(), null, true, false, 0, 1);
    renderer.getTableCellRendererComponent(tablePanel.getJTable(), null, true, true, 0, 1);

    renderer.getTableCellRendererComponent(tablePanel.getJTable(), null, false, false, 0, 7);
    renderer.getTableCellRendererComponent(tablePanel.getJTable(), null, true, false, 0, 7);
    renderer.getTableCellRendererComponent(tablePanel.getJTable(), null, true, true, 0, 7);
  }

  @Test
  public void entityMismatch() {
    final EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(TestDomain.T_EMP, CONNECTION_PROVIDER));
    tablePanel.getEntityTableModel().refresh();
    assertThrows(IllegalArgumentException.class, () -> EntityTableCellRenderers.getTableCellRenderer(tablePanel.getEntityTableModel(),
            ENTITIES.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME)));
  }
}
