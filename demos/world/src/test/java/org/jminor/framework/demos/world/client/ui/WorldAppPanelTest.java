/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.client.ui;

import org.jminor.common.User;
import org.jminor.swing.framework.ui.testing.EntityApplicationPanelTestUnit;

import org.junit.Test;

public class WorldAppPanelTest extends EntityApplicationPanelTestUnit {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  public WorldAppPanelTest() {
    super(WorldAppPanel.class, UNIT_TEST_USER);
  }

  @Test
  public void initializePanel() throws Exception {
    testInitializePanel();
  }
}
