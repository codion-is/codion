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

final class ControlDownRadioButtonMenuItem extends JRadioButtonMenuItem {

  @Override
  protected void processMouseEvent(MouseEvent e) {
    JRadioButtonMenuItem menuItem = (JRadioButtonMenuItem) e.getSource();
    if (e.getID() == MouseEvent.MOUSE_RELEASED && e.isControlDown()) {
      menuItem.setSelected(!menuItem.isSelected());
    }
    else {
      super.processMouseEvent(e);
    }
  }
}
