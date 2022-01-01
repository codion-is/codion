/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.common.user.User;
import is.codion.swing.framework.ui.test.EntityApplicationPanelTestUnit;

import org.junit.jupiter.api.Test;

public class EmpDeptAppPanelTest extends EntityApplicationPanelTestUnit {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  public EmpDeptAppPanelTest() {
    super(EmpDeptAppPanel.class, UNIT_TEST_USER);
  }

  @Test
  void initializePanel() throws Exception {
    testInitializePanel();
  }
}
