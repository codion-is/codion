/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;

import javax.swing.JToggleButton;

abstract class AbstractToggleButtonBuilder<C extends JToggleButton, B extends ToggleButtonBuilder<C, B>>
        extends AbstractButtonBuilder<Boolean, C, B> implements ToggleButtonBuilder<C, B> {

  protected AbstractToggleButtonBuilder(final Value<Boolean> linkedValue) {
    super(linkedValue);
  }

  @Override
  protected final ComponentValue<Boolean, JToggleButton> buildComponentValue(final JToggleButton component) {
    return ComponentValues.toggleButton(component);
  }

  @Override
  protected final void setInitialValue(final JToggleButton component, final Boolean initialValue) {
    component.setSelected(initialValue);
  }
}
