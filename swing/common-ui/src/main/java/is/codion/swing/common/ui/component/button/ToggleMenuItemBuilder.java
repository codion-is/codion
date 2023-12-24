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

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JMenuItem;

/**
 * Builds a toggle menu item.
 */
public interface ToggleMenuItemBuilder<C extends JMenuItem, B extends ToggleMenuItemBuilder<C, B>> extends ButtonBuilder<Boolean, C, B> {

  /**
   * Specifies whether a menu is kept open after a toggle menu item has been toggled.
   */
  enum PersistMenu {
    /**
     * Always keep the menu open.
     */
    ALWAYS,
    /**
     * Keep the menu open if CTRL is down when clicked
     */
    CTRL_DOWN,
    /**
     * Always close the menu when clicked.
     */
    NEVER
  }

  /**
   * Specifies whether a menu persists after a toggle menu item has been toggled<br>
   * Value type: {@link PersistMenu}<br>
   * Default value: {@link PersistMenu#ALWAYS}
   */
  PropertyValue<PersistMenu> PERSIST_MENU =
          Configuration.enumValue("is.codion.swing.common.ui.component.button.ToggleMenuItemBuilder.persistMenu",
                  PersistMenu.class, PersistMenu.ALWAYS);

  /**
   * @param toggleControl the toggle control to base this toggle menu item on
   * @return this builder instance
   */
  B toggleControl(ToggleControl toggleControl);

  /**
   * @param toggleControlBuilder the builder for the toggle control to base this toggle menu on
   * @return this builder instance
   */
  B toggleControl(Control.Builder<ToggleControl, ?> toggleControlBuilder);

  /**
   * @param persistMenu specifies when a menu persists after a toggle button click
   * @return this builder instance
   */
  B persistMenu(PersistMenu persistMenu);
}
