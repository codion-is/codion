/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;

import javax.swing.JRadioButton;

final class DefaultRadioButtonBuilder extends DefaultToggleButtonBuilder<JRadioButton, RadioButtonBuilder> implements RadioButtonBuilder {

  DefaultRadioButtonBuilder(Value<Boolean> value) {
    super(value);
  }

  @Override
  protected JRadioButton createButton() {
    return new JRadioButton();
  }
}
