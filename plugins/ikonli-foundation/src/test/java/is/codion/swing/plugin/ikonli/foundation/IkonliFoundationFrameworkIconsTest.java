/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.plugin.ikonli.foundation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class IkonliFoundationFrameworkIconsTest {

  @Test
  void test() {
    final IkonliFoundationFrameworkIcons icons = new IkonliFoundationFrameworkIcons();
    assertNotNull(icons.filter());
    assertNotNull(icons.configure());
    assertNotNull(icons.logoBlack());
    assertNotNull(icons.logoTransparent());
    assertNotNull(icons.logoRed());
    assertNotNull(icons.filter());

    assertNotNull(icons.add());
    assertNotNull(icons.delete());
    assertNotNull(icons.update());
    assertNotNull(icons.copy());
    assertNotNull(icons.refresh());
    assertNotNull(icons.refreshRequired());
    assertNotNull(icons.clear());
    assertNotNull(icons.up());
    assertNotNull(icons.down());
    assertNotNull(icons.detail());
    assertNotNull(icons.print());
    assertNotNull(icons.clearSelection());
    assertNotNull(icons.edit());
    assertNotNull(icons.summary());
    assertNotNull(icons.editPanel());
    assertNotNull(icons.dependencies());
  }
}
