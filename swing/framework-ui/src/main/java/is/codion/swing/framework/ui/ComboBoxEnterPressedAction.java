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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import java.awt.event.ActionEvent;

final class ComboBoxEnterPressedAction extends AbstractAction {

  private static final String ENTER_PRESSED = "enterPressed";

  private final JComboBox<?> comboBox;
  private final Action action;
  private final Action enterPressedAction;

  ComboBoxEnterPressedAction(JComboBox<?> comboBox, Action action) {
    this.comboBox = comboBox;
    this.action = action;
    this.enterPressedAction = comboBox.getActionMap().get(ENTER_PRESSED);
    this.comboBox.getActionMap().put(ENTER_PRESSED, this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (comboBox.isPopupVisible()) {
      enterPressedAction.actionPerformed(e);
    }
    else if (action.isEnabled()) {
      action.actionPerformed(e);
    }
  }
}
