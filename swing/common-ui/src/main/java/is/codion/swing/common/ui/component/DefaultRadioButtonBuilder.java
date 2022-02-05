/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

final class DefaultRadioButtonBuilder extends AbstractToggleButtonBuilder<JRadioButton, RadioButtonBuilder> implements RadioButtonBuilder {

  private int horizontalAlignment = SwingConstants.LEADING;

  DefaultRadioButtonBuilder(final Value<Boolean> value) {
    super(value);
  }

  DefaultRadioButtonBuilder(final ValueObserver<Boolean> valueObserver) {
    super(valueObserver);
  }

  @Override
  public RadioButtonBuilder horizontalAlignment(final int horizontalAlignment) {
    this.horizontalAlignment = horizontalAlignment;
    return this;
  }

  @Override
  protected JRadioButton createButton() {
    final JRadioButton radioButton = new JRadioButton();
    radioButton.setHorizontalAlignment(horizontalAlignment);

    return radioButton;
  }
}
