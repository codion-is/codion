/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import is.codion.common.user.User;
import is.codion.common.user.Users;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LoginPanelTest {

  @Test
  public void test() {
    final User user = Users.user("user", "pass".toCharArray());
    final LoginPanel panel = new LoginPanel(user);
    assertEquals(user, panel.getUser());
    assertNotNull(panel.getPasswordField());
    assertNotNull(panel.getUsernameField());
  }
}
