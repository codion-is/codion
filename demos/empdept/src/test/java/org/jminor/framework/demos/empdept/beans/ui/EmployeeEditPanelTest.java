/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.User;
import org.jminor.framework.demos.empdept.beans.EmployeeEditModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.swing.framework.testing.EntityEditPanelTestUnit;

import org.junit.Test;

public class EmployeeEditPanelTest extends EntityEditPanelTestUnit {

  public EmployeeEditPanelTest() {
    super(EmployeeEditPanel.class, EmpDept.T_EMPLOYEE, User.UNIT_TEST_USER);
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
