/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.User;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.swing.framework.ui.testing.EntityEditPanelTestUnit;

import org.junit.Test;

public class DepartmentEditPanelTest extends EntityEditPanelTestUnit {

  private static final Entities ENTITIES = new EmpDept();

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  public DepartmentEditPanelTest() {
    super(ENTITIES, DepartmentEditPanel.class, EmpDept.T_DEPARTMENT, UNIT_TEST_USER);
  }

  @Test
  public void initializePanel() throws Exception {
    testInitializePanel();
  }
}
