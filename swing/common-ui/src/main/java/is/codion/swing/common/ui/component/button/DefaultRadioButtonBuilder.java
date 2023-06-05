/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

final class DefaultRadioButtonBuilder extends DefaultToggleButtonBuilder<JRadioButton, RadioButtonBuilder> implements RadioButtonBuilder {

  DefaultRadioButtonBuilder(ToggleControl toggleControl, Value<Boolean> linkedValue) {
    super(toggleControl, linkedValue);
    if (toggleControl.value().isNullable()) {
      throw new IllegalArgumentException("A radio button does not support a nullable value");
    }
  }

  DefaultRadioButtonBuilder(Value<Boolean> value) {
    super(value);
    if (value.isNullable()) {
      throw new IllegalArgumentException("A radio button does not support a nullable value");
    }
    horizontalAlignment(SwingConstants.LEADING);
  }

  @Override
  protected JToggleButton createToggleButton() {
    return new JRadioButton();
  }
}
