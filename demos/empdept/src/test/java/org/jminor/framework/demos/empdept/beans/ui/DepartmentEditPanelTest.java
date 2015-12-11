/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.model.User;
import org.jminor.framework.demos.empdept.beans.ui.DepartmentEditPanel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.swing.framework.testing.EntityEditPanelTestUnit;
import org.jminor.swing.framework.ui.EntityEditPanel;

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
}
