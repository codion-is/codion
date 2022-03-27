/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;

import javax.swing.JToggleButton;

class DefaultToggleButtonBuilder<C extends JToggleButton, B extends ButtonBuilder<Boolean, C, B>>
        extends AbstractButtonBuilder<Boolean, C, B> implements ButtonBuilder<Boolean, C, B> {

  DefaultToggleButtonBuilder(Value<Boolean> linkedValue) {
    super(linkedValue);
  }

  @Override
  protected C createButton() {
    return (C) new JToggleButton();
  }

  @Override
  protected final ComponentValue<Boolean, C> buildComponentValue(JToggleButton component) {
    if (component instanceof NullableCheckBox) {
      return (ComponentValue<Boolean, C>) new BooleanNullableCheckBoxValue((NullableCheckBox) component);
    }

    return (ComponentValue<Boolean, C>) new BooleanToggleButtonValue(component);
  }

  @Override
  protected final void setInitialValue(C component, Boolean initialValue) {
    component.setSelected(initialValue);
  }
}
