/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.User;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.demos.empdept.beans.EmployeeEditModel;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.swing.framework.ui.testing.EntityEditPanelTestUnit;

import org.junit.jupiter.api.Test;

public class EmployeeEditPanelTest extends EntityEditPanelTestUnit {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  public EmployeeEditPanelTest() {
    super(new EmployeeEditModel(EntityConnectionProviders.connectionProvider()
                    .setDomainClassName(EmpDept.class.getName())
                    .setClientTypeId(EmployeeEditPanelTest.class.getName()).setUser(UNIT_TEST_USER)),
            EmployeeEditPanel.class);
  }

  @Test
  public void initializePanel() throws Exception {
    testInitializePanel();
  }
}
