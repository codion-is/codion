/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
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
