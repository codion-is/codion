/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;

import javax.swing.JToggleButton;

abstract class AbstractToggleButtonBuilder<C extends JToggleButton, B extends ButtonBuilder<Boolean, C, B>>
        extends AbstractButtonBuilder<Boolean, C, B> implements ButtonBuilder<Boolean, C, B> {

  protected AbstractToggleButtonBuilder(Value<Boolean> linkedValue) {
    super(linkedValue);
  }

  @Override
  protected final ComponentValue<Boolean, JToggleButton> buildComponentValue(JToggleButton component) {
    return ComponentValues.toggleButton(component);
  }

  @Override
  protected final void setInitialValue(JToggleButton component, Boolean initialValue) {
    component.setSelected(initialValue);
  }
}
