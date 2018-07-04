/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.User;
import org.jminor.framework.demos.empdept.beans.EmployeeEditModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.testing.EntityEditPanelTestUnit;

import org.junit.jupiter.api.Test;

public class EmployeeEditPanelTest extends EntityEditPanelTestUnit {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  public EmployeeEditPanelTest() {
    super(EmpDept.class.getName(), EmployeeEditPanel.class, EmpDept.T_EMPLOYEE, UNIT_TEST_USER);
  }

  @Test
  public void initializePanel() throws Exception {
    testInitializePanel();
  }

  @Override
  protected SwingEntityEditModel createEditModel() {
    return new EmployeeEditModel(getConnectionProvider());
  }
}
