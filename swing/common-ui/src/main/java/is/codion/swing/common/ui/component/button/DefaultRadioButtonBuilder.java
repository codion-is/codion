/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;

import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

final class DefaultRadioButtonBuilder extends DefaultToggleButtonBuilder<JRadioButton, RadioButtonBuilder> implements RadioButtonBuilder {

  DefaultRadioButtonBuilder(Value<Boolean> linkedValue) {
    super(linkedValue);
    horizontalAlignment(SwingConstants.LEADING);
  }

  @Override
  protected JToggleButton createToggleButton() {
    return new JRadioButton();
  }

  @Override
  protected boolean supportsNull() {
    return false;
  }
}
