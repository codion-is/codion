/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import javax.swing.JRadioButtonMenuItem;
import java.awt.event.MouseEvent;

import static is.codion.swing.common.ui.component.button.PersistMenuCheckBoxMenuItem.persistMenu;

final class PersistMenuRadioButtonMenuItem extends JRadioButtonMenuItem {

  private final ToggleMenuItemBuilder.PersistMenu persistMenu;

  PersistMenuRadioButtonMenuItem(ToggleMenuItemBuilder.PersistMenu persistMenu) {
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
}
