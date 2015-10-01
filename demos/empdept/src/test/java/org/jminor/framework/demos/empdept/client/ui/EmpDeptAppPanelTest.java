/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.client.ui;

import org.jminor.common.model.User;
import org.jminor.framework.demos.empdept.client.ui.EmpDeptAppPanel;
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
