/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class DefaultFrameworkIconsTest {

  @Test
  void test() {
    final DefaultFrameworkIcons icons = new DefaultFrameworkIcons();
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
