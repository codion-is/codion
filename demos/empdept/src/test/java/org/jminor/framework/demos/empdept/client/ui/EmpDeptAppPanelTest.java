/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.client.ui;

import org.jminor.common.User;
import org.jminor.swing.framework.testing.EntityApplicationPanelTestUnit;

import org.junit.Test;

public class EmpDeptAppPanelTest extends EntityApplicationPanelTestUnit {

  public EmpDeptAppPanelTest() {
    super(EmpDeptAppPanel.class, User.UNIT_TEST_USER);
  }

  @Test
  public void initializePanel() throws Exception {
    testInitializePanel();
  }
}
