/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.testing;

import org.jminor.common.model.User;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityEditPanelTestUnit;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentEditPanel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DepartmentEditPanelTest extends EntityEditPanelTestUnit {

  static {
    EmpDept.init();
  }

  public DepartmentEditPanelTest() {
    super(DepartmentEditPanel.class, EmpDept.T_DEPARTMENT, User.UNIT_TEST_USER);
  }

  @Test
  public void initializePanel() throws Exception {
    testInitializePanel();
  }

  @Test
  public void initialFocusProperty() throws Exception {
    final EntityEditPanel editPanel = createEditPanel().initializePanel();
    final Entity operations = editPanel.getEditModel().getConnectionProvider()
            .getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "OPERATIONS");
    assertEquals(editPanel.getComponent(EmpDept.DEPARTMENT_ID), editPanel.getComponent(editPanel.getInitialFocusProperty()));
    editPanel.getEditModel().setEntity(operations);
    assertEquals(editPanel.getComponent(EmpDept.DEPARTMENT_NAME), editPanel.getComponent(editPanel.getInitialFocusProperty()));
    editPanel.getEditModel().setEntity(null);
    assertEquals(editPanel.getComponent(EmpDept.DEPARTMENT_ID), editPanel.getComponent(editPanel.getInitialFocusProperty()));
  }
}
