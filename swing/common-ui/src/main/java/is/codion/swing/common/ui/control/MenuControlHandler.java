/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.StateObserver;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenu;

final class MenuControlHandler extends ControlHandler {

  private final JMenu menu;

  MenuControlHandler(final ControlList controls) {
    menu = new JMenu(controls.getName());
    final String description = controls.getDescription();
    if (description != null) {
      menu.setToolTipText(description);
    }
    final StateObserver enabledObserver = controls.getEnabledObserver();
    if (enabledObserver != null) {
      menu.setEnabled(enabledObserver.get());
      enabledObserver.addListener(() -> menu.setEnabled(enabledObserver.get()));
    }
    final Icon icon = controls.getIcon();
    if (icon != null) {
      menu.setIcon(icon);
    }
    final int mnemonic = controls.getMnemonic();
    if (mnemonic != -1) {
      menu.setMnemonic(mnemonic);
    }
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
      menu.add(control);
    }
  }

  @Override
  public void onControlList(final ControlList controls) {
    final MenuControlHandler controlHandler = new MenuControlHandler(controls);
    controls.getActions().forEach(controlHandler);
    menu.add(controlHandler.menu);
  }

  @Override
  public void onAction(final Action action) {
    menu.add(action);
  }

  public JMenu getMenu() {
    return menu;
  }
}
