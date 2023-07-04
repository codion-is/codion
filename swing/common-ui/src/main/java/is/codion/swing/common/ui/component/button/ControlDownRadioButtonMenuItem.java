/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import javax.swing.JRadioButtonMenuItem;
import java.awt.event.MouseEvent;

final class ControlDownRadioButtonMenuItem extends JRadioButtonMenuItem {

  @Override
  protected void processMouseEvent(MouseEvent e) {
    JRadioButtonMenuItem menuItem = (JRadioButtonMenuItem) e.getSource();
    if (e.getID() == MouseEvent.MOUSE_RELEASED && e.isControlDown()) {
      menuItem.setSelected(!menuItem.isSelected());
    }
    else {
      super.processMouseEvent(e);
    }
  }
}
