/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;

import javax.swing.JToggleButton;

final class DefaultToggleButtonBuilder<B extends ButtonBuilder<Boolean, JToggleButton, B>>
        extends AbstractButtonBuilder<Boolean, JToggleButton, B> implements ButtonBuilder<Boolean, JToggleButton, B> {

  DefaultToggleButtonBuilder(Value<Boolean> linkedValue) {
    super(linkedValue);
  }

  @Override
  protected JToggleButton createButton() {
    return new JToggleButton();
  }

  @Override
  protected ComponentValue<Boolean, JToggleButton> buildComponentValue(JToggleButton component) {
    return ComponentValues.toggleButton(component);
  }

  @Override
  protected void setInitialValue(JToggleButton component, Boolean initialValue) {
    component.setSelected(initialValue);
  }
}
