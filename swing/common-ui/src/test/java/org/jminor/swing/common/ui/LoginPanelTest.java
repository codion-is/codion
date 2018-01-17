/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.User;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LoginPanelTest {

  @Test
  public void test() {
    final User user = new User("user", "pass".toCharArray());
    final LoginPanel panel = new LoginPanel(user);
    assertEquals(user, panel.getUser());
    assertNotNull(panel.getPasswordField());
    assertNotNull(panel.getUsernameField());
  }
}
