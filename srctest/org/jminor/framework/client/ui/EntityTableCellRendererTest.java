/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.DefaultEntityTableModel;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

public class EntityTableCellRendererTest {

  @Test
  public void test() {
    EmpDept.init();
    final EntityTablePanel tablePanel = new EntityTablePanel(new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.CONNECTION_PROVIDER));
    tablePanel.getEntityTableModel().refresh();
    final EntityTableCellRenderer renderer = new EntityTableCellRenderer(tablePanel.getEntityTableModel());
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
}
