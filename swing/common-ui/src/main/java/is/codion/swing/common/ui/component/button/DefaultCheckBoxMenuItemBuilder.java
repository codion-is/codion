/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

final class DefaultCheckBoxMenuItemBuilder<B extends CheckBoxMenuItemBuilder<B>> extends AbstractToggleMenuItemBuilder<JCheckBoxMenuItem, B>
        implements CheckBoxMenuItemBuilder<B> {

  DefaultCheckBoxMenuItemBuilder(Value<Boolean> linkedValue) {
    super(linkedValue);
  }

  @Override
  protected JMenuItem createMenuItem() {
    return new ControlDownMenuItem();
  }
}
