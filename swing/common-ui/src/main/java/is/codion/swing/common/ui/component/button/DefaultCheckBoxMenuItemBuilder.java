/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import java.awt.event.MouseEvent;

final class DefaultCheckBoxMenuItemBuilder<B extends CheckBoxMenuItemBuilder<B>> extends AbstractToggleMenuItemBuilder<JCheckBoxMenuItem, B>
        implements CheckBoxMenuItemBuilder<B> {

  DefaultCheckBoxMenuItemBuilder(ToggleControl toggleControl, Value<Boolean> linkedValue) {
    super(toggleControl, linkedValue);
  }

  @Override
  protected JMenuItem createMenuItem() {
    return new JCheckBoxMenuItem() {
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
    };
  }
}
