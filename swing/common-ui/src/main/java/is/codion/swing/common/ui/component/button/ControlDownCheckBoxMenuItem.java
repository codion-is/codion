/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import javax.swing.JCheckBoxMenuItem;
import java.awt.event.MouseEvent;

final class ControlDownCheckBoxMenuItem extends JCheckBoxMenuItem {

  @Override
  protected void processMouseEvent(MouseEvent e) {
    JCheckBoxMenuItem menuItem = (JCheckBoxMenuItem) e.getSource();
    if (e.getID() == MouseEvent.MOUSE_RELEASED && e.isControlDown()) {
      menuItem.setSelected(!menuItem.isSelected());
    }
    else {
      super.processMouseEvent(e);
    }
  }
}