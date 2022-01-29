/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class DefaultIconsTest {

  @Test
  void test() {
    final DefaultIcons icons = new DefaultIcons();
    assertNotNull(icons.filter());
    assertNotNull(icons.configure());
    assertNotNull(icons.logoBlack());
    assertNotNull(icons.logoTransparent());
    assertNotNull(icons.logoRed());
    assertNotNull(icons.filter());
  }
}
