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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui;

import java.awt.Color;

import static java.util.Objects.requireNonNull;

/**
 * Utilities class for Color.
 */
public final class Colors {

  private Colors() {}

  /**
   * Returns a darker version of the given color, using 0.8 as the mulitiplication factor.
   * @param color the color to darken
   * @return a darker version of the given color
   * @see Color#darker()
   */
  public static Color darker(Color color) {
    return darker(color, 0.8);
  }

  /**
   * Returns a darker version of the given color, using the given factor.
   * @param color the color to darken
   * @param factor a number between 0 and 1, non-inclusive
   * @return a darker version of the given color
   * @see Color#darker()
   */
  public static Color darker(Color color, double factor) {
    requireNonNull(color);
    if (factor <= 0 || factor >= 1) {
      throw new IllegalArgumentException("Factor must be between 0 and 1, non-inclusive");
    }

    return new Color(Math.max((int) (color.getRed() * factor), 0),
            Math.max((int) (color.getGreen() * factor), 0),
            Math.max((int) (color.getBlue() * factor), 0),
            color.getAlpha());
  }
}
