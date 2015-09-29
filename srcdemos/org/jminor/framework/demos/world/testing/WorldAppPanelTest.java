/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.testing;

import org.jminor.common.model.User;
import org.jminor.framework.demos.world.client.ui.WorldAppPanel;
import org.jminor.swing.framework.testing.EntityApplicationPanelTestUnit;

import org.junit.Test;

public class WorldAppPanelTest extends EntityApplicationPanelTestUnit {

  public WorldAppPanelTest() {
    super(WorldAppPanel.class, User.UNIT_TEST_USER);
  }

  @Test
  public void initializePanel() throws Exception {
    testInitializePanel();
  }
}
