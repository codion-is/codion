/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.icon;

import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.Ikon;

import javax.swing.ImageIcon;

import static org.junit.jupiter.api.Assertions.*;

public final class DefaultFrameworkIconsTest {

  @Test
  void test() {
    DefaultFrameworkIcons icons = new DefaultFrameworkIcons();
    assertNotNull(icons.filter());
    assertNotNull(icons.search());
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
    assertNotNull(icons.settings());
    assertNotNull(icons.search());
    assertNotNull(icons.calendar());
    assertNotNull(icons.editText());
    assertNotNull(icons.logo());
    ImageIcon logo12 = icons.logo(12);
    assertNotNull(logo12);
    assertSame(logo12, icons.logo(12));
    assertThrows(NullPointerException.class, () -> icons.add((Ikon[]) null));
    assertThrows(NullPointerException.class, () -> icons.add(null, FrameworkIkons.SETTINGS));
    assertThrows(IllegalArgumentException.class, () -> icons.add(FrameworkIkons.SETTINGS));

    //can't do exact size checking, depends on font metrics, so just assert that it is bigger
    int width = icons.delete().getIconWidth();
    FrameworkIcons.ICON_SIZE.set(FrameworkIcons.ICON_SIZE.get() + 2);
    assertTrue(icons.delete().getIconWidth() > width);
  }
}
