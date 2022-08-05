/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;

import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

final class DefaultCheckBoxBuilder extends DefaultToggleButtonBuilder<JCheckBox, CheckBoxBuilder> implements CheckBoxBuilder {

  private boolean nullable = false;

  DefaultCheckBoxBuilder(Value<Boolean> value) {
    super(value);
    if (value != null) {
      nullable = value.nullable();
    }
    horizontalAlignment(SwingConstants.LEADING);
  }

  @Override
  public CheckBoxBuilder nullable(boolean nullable) {
    this.nullable = nullable;
    return this;
  }

  @Override
  protected JCheckBox createButton() {
    return nullable ? new NullableCheckBox(new NullableToggleButtonModel()) : new JCheckBox();
  }
}
