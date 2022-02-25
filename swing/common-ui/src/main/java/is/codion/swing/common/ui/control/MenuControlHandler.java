/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import javax.swing.Action;
import javax.swing.JMenu;

final class MenuControlHandler extends ControlHandler {

  private final JMenu menu;

  MenuControlHandler(final JMenu menu, final Controls controls) {
    this.menu = menu;
    controls.getActions().forEach(this);
  }

  @Override
  public void onSeparator() {
    menu.addSeparator();
  }

  @Override
  public void onControl(final Control control) {
    if (control instanceof ToggleControl) {
      menu.add(((ToggleControl) control).createCheckBoxMenuItem());
    }
    else {
      menu.add(control.createMenuItem());
    }
  }

  @Override
  public void onControls(final Controls controls) {
    JMenu subMenu = new JMenu(controls);
    new MenuControlHandler(subMenu, controls);
    this.menu.add(subMenu);
  }

  @Override
  public void onAction(final Action action) {
    menu.add(action);
  }
}
