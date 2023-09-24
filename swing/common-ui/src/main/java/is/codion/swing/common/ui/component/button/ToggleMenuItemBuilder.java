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
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JMenuItem;

/**
 * Builds a toggle menu item.
 */
public interface ToggleMenuItemBuilder<C extends JMenuItem, B extends ToggleMenuItemBuilder<C, B>> extends ButtonBuilder<Boolean, C, B> {

  /**
   * @param toggleControl the toggle control to base this toggle menu item on
   * @return this builder instance
   */
  B toggleControl(ToggleControl toggleControl);

  /**
   * @param toggleControlBuilder the builder for the toggle control to base this toggle menu on
   * @return this builder instance
   */
  B toggleControl(ToggleControl.Builder toggleControlBuilder);
}
