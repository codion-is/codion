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
package is.codion.swing.common.ui.border;

import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

/**
 * Utility class for working with Borders.
 */
public final class Borders {

  private Borders() {}

  /**
   * Creates en empty border using the value of {@link Layouts#GAP}
   * as the top, bottom, left and right values.
   * @return a new empty border
   */
  public static Border emptyBorder() {
    int gap = Layouts.GAP.get();

    return BorderFactory.createEmptyBorder(gap, gap, gap, gap);
  }
}
