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

import javax.swing.JRadioButtonMenuItem;
import java.awt.event.MouseEvent;

import static is.codion.swing.common.ui.component.button.PersistMenuCheckBoxMenuItem.persistMenu;

final class PersistMenuRadioButtonMenuItem extends JRadioButtonMenuItem {

  private final ToggleMenuItemBuilder.PersistMenu persistMenu;

  PersistMenuRadioButtonMenuItem(ToggleMenuItemBuilder.PersistMenu persistMenu) {
    this.persistMenu = persistMenu;
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_RELEASED && persistMenu(e.isControlDown(), persistMenu)) {
      setSelected(!isSelected());
    }
    else {
      super.processMouseEvent(e);
    }
  }
}
