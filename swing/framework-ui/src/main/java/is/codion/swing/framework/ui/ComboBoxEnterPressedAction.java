/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
