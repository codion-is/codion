/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.TestDomain;
import org.jminor.swing.framework.model.SwingEntityTableModel;

import org.junit.Test;

public class EntityTableCellRenderersTest {

  @Test
  public void test() {
    TestDomain.init();
    final EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER));
    tablePanel.getEntityTableModel().refresh();
    final EntityTableCellRenderer renderer = EntityTableCellRenderers.getTableCellRenderer(tablePanel.getEntityTableModel(),
            Entities.getProperty(TestDomain.T_EMP, TestDomain.EMP_NAME));
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

  @Test(expected = IllegalArgumentException.class)
  public void entityMismatch() {
    TestDomain.init();
    final EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER));
    tablePanel.getEntityTableModel().refresh();
    EntityTableCellRenderers.getTableCellRenderer(tablePanel.getEntityTableModel(),
            Entities.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME));
  }
}
