/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;

import javax.swing.JToggleButton;

final class DefaultToggleButtonBuilder<B extends ToggleButtonBuilder<JToggleButton, B>>
        extends AbstractToggleButtonBuilder<JToggleButton, B> implements ToggleButtonBuilder<JToggleButton, B> {

  DefaultToggleButtonBuilder(final Value<Boolean> linkedValue) {
    super(linkedValue);
  }

  @Override
  protected JToggleButton createButton() {
    return new JToggleButton();
  }
}
