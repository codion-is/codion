/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;

public final class LoginPanelTest {

  @Test
  public void test() {
    new LoginPanel(User.user("scott", "test".toCharArray()), user -> {}, new JLabel());
  }
}
