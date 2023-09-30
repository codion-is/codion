/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JToggleButton;

import static java.util.Objects.requireNonNull;

class DefaultToggleButtonBuilder<C extends JToggleButton, B extends ToggleButtonBuilder<C, B>>
        extends AbstractButtonBuilder<Boolean, C, B> implements ToggleButtonBuilder<C, B> {

  private ToggleControl toggleControl;

  DefaultToggleButtonBuilder(Value<Boolean> linkedValue) {
    super(linkedValue);
  }

  @Override
  public final B toggleControl(ToggleControl toggleControl) {
    if (requireNonNull(toggleControl).value().nullable() && !supportsNull()) {
      throw new IllegalArgumentException("This toggle button builder does not support a nullable value");
    }
    this.toggleControl = toggleControl;
    action(toggleControl);
    return (B) this;
  }

  @Override
  public final B toggleControl(ToggleControl.Builder toggleControlBuilder) {
    return toggleControl(requireNonNull(toggleControlBuilder).build());
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
