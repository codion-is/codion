/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class LogosTest {

  @Test
  void test() {
    assertNotNull(Logos.logoBlack());
    assertNotNull(Logos.logoTransparent());
    assertNotNull(Logos.logoRed());
  }
}
