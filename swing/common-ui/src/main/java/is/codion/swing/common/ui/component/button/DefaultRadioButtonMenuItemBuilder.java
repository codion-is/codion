/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;

import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

final class DefaultRadioButtonMenuItemBuilder<B extends RadioButtonMenuItemBuilder<B>> extends AbstractToggleMenuItemBuilder<JRadioButtonMenuItem, B>
        implements RadioButtonMenuItemBuilder<B> {

  DefaultRadioButtonMenuItemBuilder(Value<Boolean> linkedValue) {
    super(linkedValue);
  }

  @Override
  protected JMenuItem createMenuItem() {
    return new ControlDownRadioMenuItem();
  }
}
