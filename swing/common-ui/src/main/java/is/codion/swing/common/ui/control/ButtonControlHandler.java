/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

final class ButtonControlHandler extends ControlHandler {

  private final JPanel panel;
  private final boolean vertical;

  ButtonControlHandler(final JPanel panel, final Controls controls, final boolean vertical) {
    this.panel = panel;
    this.vertical = vertical;
    controls.getActions().forEach(this);
  }

  @Override
  public void onSeparator() {
    panel.add(new JLabel());
  }

  @Override
  public void onControl(final Control control) {
    if (control instanceof ToggleControl) {
      panel.add(((ToggleControl) control).createCheckBox());
    }
    else {
      panel.add(control.createButton());
    }
  }

  @Override
  public void onControls(final Controls controls) {
    panel.add(vertical ? controls.createVerticalButtonPanel() : controls.createHorizontalButtonPanel());
  }

  @Override
  public void onAction(final Action action) {
    panel.add(new JButton(action));
  }
}
