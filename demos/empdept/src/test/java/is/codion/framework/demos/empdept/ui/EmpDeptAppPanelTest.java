/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.ui;

import is.codion.common.user.User;
import is.codion.framework.demos.empdept.model.EmpDeptAppModel;
import is.codion.swing.framework.ui.test.EntityApplicationPanelTestUnit;

import org.junit.jupiter.api.Test;

public class EmpDeptAppPanelTest extends EntityApplicationPanelTestUnit<EmpDeptAppModel> {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public EmpDeptAppPanelTest() {
    super(EmpDeptAppModel.class, EmpDeptAppPanel.class, UNIT_TEST_USER);
  }

  @Test
  void initialize() {
    testInitialize();
  }
}
