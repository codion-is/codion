/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

final class ButtonControlHandler extends ControlHandler {

  private final JPanel panel;
  private final boolean vertical;

  ButtonControlHandler(JPanel panel, Controls controls, boolean vertical) {
    this.panel = panel;
    this.vertical = vertical;
    controls.getActions().forEach(this);
  }

  @Override
  public void onSeparator() {
    panel.add(new JLabel());
  }

  @Override
  public void onControl(Control control) {
    if (control instanceof ToggleControl) {
      panel.add(((ToggleControl) control).createCheckBox());
    }
    else {
      panel.add(control.createButton());
    }
  }

  @Override
  public void onControls(Controls controls) {
    panel.add(vertical ? controls.createVerticalButtonPanel() : controls.createHorizontalButtonPanel());
  }

  @Override
  public void onAction(Action action) {
    panel.add(new JButton(action));
  }
}
