/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.DefaultEntityTableModel;
import org.jminor.framework.db.local.LocalEntityConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;

import org.junit.Test;

public class EntityTableCellRenderersTest {

  @Test
  public void test() {
    EmpDept.init();
    final EntityTablePanel tablePanel = new EntityTablePanel(new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, LocalEntityConnectionTest.CONNECTION_PROVIDER));
    tablePanel.getEntityTableModel().refresh();
    final EntityTableCellRenderer renderer = EntityTableCellRenderers.getTableCellRenderer(tablePanel.getEntityTableModel(),
            Entities.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_NAME));
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
    EmpDept.init();
    final EntityTablePanel tablePanel = new EntityTablePanel(new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, LocalEntityConnectionTest.CONNECTION_PROVIDER));
    tablePanel.getEntityTableModel().refresh();
    EntityTableCellRenderers.getTableCellRenderer(tablePanel.getEntityTableModel(),
            Entities.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME));
  }
}
