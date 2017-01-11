/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.client.ui;

import org.jminor.common.User;
import org.jminor.swing.framework.testing.EntityApplicationPanelTestUnit;

import org.junit.Test;

public class EmpDeptAppPanelTest extends EntityApplicationPanelTestUnit {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  public EmpDeptAppPanelTest() {
    super(EmpDeptAppPanel.class, UNIT_TEST_USER);
  }

  @Test
  public void initializePanel() throws Exception {
    testInitializePanel();
  }
}
