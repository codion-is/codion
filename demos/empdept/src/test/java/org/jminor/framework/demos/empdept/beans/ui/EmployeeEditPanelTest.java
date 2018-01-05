/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.User;
import org.jminor.framework.demos.empdept.beans.EmployeeEditModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.swing.framework.ui.testing.EntityEditPanelTestUnit;

import org.junit.Test;

public class EmployeeEditPanelTest extends EntityEditPanelTestUnit {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  public EmployeeEditPanelTest() {
    super(EmpDept.class.getName(), EmployeeEditPanel.class, EmpDept.T_EMPLOYEE, UNIT_TEST_USER);
  }

  @Test
  public void initializePanel() throws Exception {
    testInitializePanel();
  }

  @Override
  protected EntityEditModel createEditModel() {
    return new EmployeeEditModel(getConnectionProvider());
  }
}
