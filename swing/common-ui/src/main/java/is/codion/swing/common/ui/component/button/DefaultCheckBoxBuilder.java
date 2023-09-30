/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;

import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

final class DefaultCheckBoxBuilder extends DefaultToggleButtonBuilder<JCheckBox, CheckBoxBuilder> implements CheckBoxBuilder {

  private boolean nullable = false;

  DefaultCheckBoxBuilder(Value<Boolean> linkedValue) {
    super(linkedValue);
    if (linkedValue != null && linkedValue.nullable()) {
      nullable = true;
    }
    horizontalAlignment(SwingConstants.LEADING);
  }

  @Override
  public CheckBoxBuilder nullable(boolean nullable) {
    this.nullable = nullable;
    return this;
  }

  @Override
  protected JToggleButton createToggleButton() {
    return nullable ? new NullableCheckBox(new NullableToggleButtonModel()) : new JCheckBox();
  }
}
