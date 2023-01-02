/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;

import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

final class DefaultRadioButtonBuilder extends DefaultToggleButtonBuilder<JRadioButton, RadioButtonBuilder> implements RadioButtonBuilder {

  DefaultRadioButtonBuilder(Value<Boolean> value) {
    super(value);
    horizontalAlignment(SwingConstants.LEADING);
  }

  @Override
  protected JRadioButton createButton() {
    return new JRadioButton();
  }
}
