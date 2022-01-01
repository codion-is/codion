/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import javax.swing.Action;
import javax.swing.JToolBar;

final class ToolBarControlHandler extends ControlHandler {

  private final JToolBar toolbar;

  ToolBarControlHandler(final JToolBar owner) {
    this.toolbar = owner;
  }

  @Override
  public void onSeparator() {
    toolbar.addSeparator();
  }

  @Override
  public void onControl(final Control control) {
    if (control instanceof ToggleControl) {
      toolbar.add(((ToggleControl) control).createToggleButton());
    }
    else {
      toolbar.add(control);
    }
  }

  @Override
  public void onControls(final Controls controls) {
    controls.getActions().forEach(new ToolBarControlHandler(toolbar));
  }

  @Override
  public void onAction(final Action action) {
    toolbar.add(action);
  }
}
