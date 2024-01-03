/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.ui;

import is.codion.common.user.User;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.swing.framework.ui.test.EntityApplicationPanelTestUnit;

import org.junit.jupiter.api.Test;

public class EmployeesAppPanelTest extends EntityApplicationPanelTestUnit<EmployeesAppModel> {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public EmployeesAppPanelTest() {
    super(EmployeesAppModel.class, EmployeesAppPanel.class, UNIT_TEST_USER);
  }

  @Test
  void initialize() {
    testInitialize();
  }
}
