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

import is.codion.swing.common.ui.component.button.ToggleMenuItemBuilder.PersistMenu;

import javax.swing.JCheckBoxMenuItem;
import java.awt.event.MouseEvent;

final class PersistsMenuCheckBoxMenuItem extends JCheckBoxMenuItem {

  private final PersistMenu persistMenu;

  PersistsMenuCheckBoxMenuItem(PersistMenu persistMenu) {
    this.persistMenu = persistMenu;
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) e.getSource();
    if (e.getID() == MouseEvent.MOUSE_RELEASED) {
      if (persistMenu(e, persistMenu)) {
        menuItem.setSelected(!menuItem.isSelected());
      }
      else {
        super.processMouseEvent(e);
      }
    }
  }

  static boolean persistMenu(MouseEvent e, PersistMenu persistMenu) {
    switch (persistMenu) {
      case NEVER:
        return false;
      case ALWAYS:
        return true;
      case CTRL_DOWN:
        return e.isControlDown();
      default:
        throw new IllegalArgumentException("Unknown value: " + persistMenu);
    }
  }
}
