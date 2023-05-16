/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icon;

import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.foundation.Foundation;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class IconsTest {

  @Test
  void test() {
    Icons icons = Icons.icons();
    assertThrows(IllegalArgumentException.class, () -> icons.icon(Foundation.ALERT));
    icons.add(Foundation.ALERT, Foundation.FOUNDATION);
    assertThrows(IllegalArgumentException.class, () -> icons.add(Foundation.FOUNDATION));
    assertNotNull(icons.icon(Foundation.ALERT));
    assertNotNull(icons.icon(Foundation.FOUNDATION));
    Icons.ICON_COLOR.set(Color.RED);
  }
}
