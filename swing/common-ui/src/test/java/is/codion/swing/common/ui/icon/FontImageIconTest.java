/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.icon;

import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.foundation.Foundation;

import java.awt.Color;

public final class FontImageIconTest {

  @Test
  void builder() {
    FontImageIcon.builder(Foundation.FOUNDATION)
            .size(42)
            .color(Color.PINK)
            .imageIconFactory(new FontImageIcon.ImageIconFactory() {})
            .iconPainter(new FontImageIcon.IconPainter() {})
            .build();
  }
}
