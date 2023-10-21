/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.button.ToggleMenuItemBuilder.PersistMenu;

import javax.swing.JCheckBoxMenuItem;
import java.awt.event.MouseEvent;

final class PersistMenuCheckBoxMenuItem extends JCheckBoxMenuItem {

  private final PersistMenu persistMenu;

  PersistMenuCheckBoxMenuItem(PersistMenu persistMenu) {
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

  static boolean persistMenu(boolean controlDown, PersistMenu persistMenu) {
    switch (persistMenu) {
      case NEVER:
        return false;
      case ALWAYS:
        return true;
      case CTRL_DOWN:
        return controlDown;
      default:
        throw new IllegalArgumentException("Unknown value: " + persistMenu);
    }
  }
}
