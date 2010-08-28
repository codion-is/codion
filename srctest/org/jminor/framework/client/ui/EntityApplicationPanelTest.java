/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.User;
import org.jminor.framework.demos.chinook.client.ui.ChinookAppPanel;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class EntityApplicationPanelTest {

  @Test
  public void test() throws Exception {
    final ChinookAppPanel panel = new ChinookAppPanel();
    panel.setLoginRequired(false);
    panel.setShowStartupDialog(false);
    panel.startApplication("Test", null, false, null, User.UNIT_TEST_USER, false);
    assertNotNull(panel.getMainApplicationPanel(Chinook.T_CUSTOMER));
    assertNull(panel.getMainApplicationPanel(EmpDept.T_DEPARTMENT));

    panel.logout();
  }
}
