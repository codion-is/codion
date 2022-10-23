/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import javax.swing.Action;
import javax.swing.JToolBar;

final class ToolBarControlHandler extends ControlHandler {

  private final JToolBar toolbar;

  ToolBarControlHandler(JToolBar owner) {
    this.toolbar = owner;
  }

  @Override
  public void onSeparator() {
    toolbar.addSeparator();
  }

  @Override
  public void onControl(Control control) {
    if (control instanceof ToggleControl) {
      toolbar.add(((ToggleControl) control).createToggleButton());
    }
    else {
      toolbar.add(control);
    }
  }

  @Override
  public void onControls(Controls controls) {
    controls.actions().forEach(new ToolBarControlHandler(toolbar));
  }

  @Override
  public void onAction(Action action) {
    toolbar.add(action);
  }
}
