/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import javax.swing.JRadioButtonMenuItem;
import java.awt.event.MouseEvent;

final class PersistMenuRadioButtonMenuItem extends JRadioButtonMenuItem {

  private final ToggleMenuItemBuilder.PersistMenu persistMenu;

  PersistMenuRadioButtonMenuItem(ToggleMenuItemBuilder.PersistMenu persistMenu) {
    this.persistMenu = persistMenu;
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    JRadioButtonMenuItem menuItem = (JRadioButtonMenuItem) e.getSource();
    if (PersistMenuCheckBoxMenuItem.persistMenu(e, persistMenu)) {
      menuItem.setSelected(!menuItem.isSelected());
    }
    else {
      super.processMouseEvent(e);
    }
  }
}
