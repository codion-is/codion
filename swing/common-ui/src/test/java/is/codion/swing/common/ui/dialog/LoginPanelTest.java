/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.user.User;
import is.codion.swing.common.ui.icon.Logos;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;

public final class LoginPanelTest {

  @Test
  void test() {
    new LoginPanel(User.user("scott", "test".toCharArray()), user -> {}, Logos.logoTransparent(), new JLabel());
  }
}
