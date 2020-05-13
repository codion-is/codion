/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.world.ui;

import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.swing.framework.ui.test.EntityApplicationPanelTestUnit;

import org.junit.jupiter.api.Test;

public class WorldAppPanelTest extends EntityApplicationPanelTestUnit {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  public WorldAppPanelTest() {
    super(WorldAppPanel.class, UNIT_TEST_USER);
  }

  @Test
  public void initializePanel() throws Exception {
    testInitializePanel();
  }
}
