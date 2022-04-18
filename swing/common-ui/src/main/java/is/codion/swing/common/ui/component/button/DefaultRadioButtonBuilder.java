/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;

import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

final class DefaultRadioButtonBuilder extends DefaultToggleButtonBuilder<JRadioButton, RadioButtonBuilder> implements RadioButtonBuilder {

  private int horizontalAlignment = SwingConstants.LEADING;

  DefaultRadioButtonBuilder(Value<Boolean> value) {
    super(value);
  }

  @Override
  public RadioButtonBuilder horizontalAlignment(int horizontalAlignment) {
    this.horizontalAlignment = horizontalAlignment;
    return this;
  }

  @Override
  protected JRadioButton createButton() {
    JRadioButton radioButton = new JRadioButton();
    radioButton.setHorizontalAlignment(horizontalAlignment);

    return radioButton;
  }
}
