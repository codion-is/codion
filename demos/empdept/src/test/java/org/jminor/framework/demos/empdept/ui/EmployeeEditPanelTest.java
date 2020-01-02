/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.ui;

import org.jminor.common.User;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.demos.empdept.model.EmployeeEditModel;
import org.jminor.swing.framework.ui.test.EntityEditPanelTestUnit;

import org.junit.jupiter.api.Test;

public class EmployeeEditPanelTest extends EntityEditPanelTestUnit {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

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
