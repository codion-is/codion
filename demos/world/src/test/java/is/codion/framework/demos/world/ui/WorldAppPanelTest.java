/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.ui;

import is.codion.common.user.User;
import is.codion.swing.framework.ui.test.EntityApplicationPanelTestUnit;

import org.junit.jupiter.api.Test;

public class WorldAppPanelTest extends EntityApplicationPanelTestUnit {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public WorldAppPanelTest() {
    super(WorldAppPanel.class, UNIT_TEST_USER);
  }

  @Test
  void initializePanel() throws Exception {
    testInitializePanel();
  }
}
