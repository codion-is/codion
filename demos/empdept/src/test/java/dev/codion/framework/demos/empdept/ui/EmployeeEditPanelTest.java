/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.empdept.ui;

import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProviders;
import dev.codion.framework.demos.empdept.domain.EmpDept;
import dev.codion.framework.demos.empdept.model.EmployeeEditModel;
import dev.codion.swing.framework.ui.test.EntityEditPanelTestUnit;

import org.junit.jupiter.api.Test;

public class EmployeeEditPanelTest extends EntityEditPanelTestUnit {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

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
