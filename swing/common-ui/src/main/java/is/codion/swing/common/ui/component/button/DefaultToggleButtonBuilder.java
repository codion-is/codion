/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JToggleButton;

import static java.util.Objects.requireNonNull;

class DefaultToggleButtonBuilder<C extends JToggleButton, B extends ToggleButtonBuilder<C, B>>
        extends AbstractButtonBuilder<Boolean, C, B> implements ToggleButtonBuilder<C, B> {

  private final ToggleControl toggleControl;

  DefaultToggleButtonBuilder(Value<Boolean> linkedValue) {
    super(linkedValue);
    this.toggleControl = null;
  }

  DefaultToggleButtonBuilder(ToggleControl toggleControl, Value<Boolean> linkedValue) {
    super(linkedValue);
    this.toggleControl = requireNonNull(toggleControl);
    action(toggleControl);
  }

  protected JToggleButton createToggleButton() {
    return new JToggleButton();
  }

  @Override
  protected final C createButton() {
    JToggleButton toggleButton = createToggleButton();
    if (toggleControl != null) {
      toggleButton.setModel(createButtonModel(toggleControl));
    }

    return (C) toggleButton;
  }

  @Override
  protected final ComponentValue<Boolean, C> createComponentValue(JToggleButton component) {
    if (component instanceof NullableCheckBox) {
      return (ComponentValue<Boolean, C>) new BooleanNullableCheckBoxValue((NullableCheckBox) component);
    }

    return (ComponentValue<Boolean, C>) new BooleanToggleButtonValue<>(component);
  }

  @Override
  protected final void setInitialValue(C component, Boolean initialValue) {
    component.setSelected(initialValue);
  }
}
