/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;

import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

final class DefaultCheckBoxBuilder extends DefaultToggleButtonBuilder<JCheckBox, CheckBoxBuilder> implements CheckBoxBuilder {

  private boolean nullable = false;
  private int horizontalAlignment = SwingConstants.LEADING;

  DefaultCheckBoxBuilder(Value<Boolean> value) {
    super(value);
    if (value != null) {
      nullable = value.isNullable();
    }
  }

  @Override
  public CheckBoxBuilder nullable(boolean nullable) {
    this.nullable = nullable;
    return this;
  }

  @Override
  public CheckBoxBuilder horizontalAlignment(int horizontalAlignment) {
    this.horizontalAlignment = horizontalAlignment;
    return this;
  }

  @Override
  protected JCheckBox createButton() {
    JCheckBox checkBox = nullable ? new NullableCheckBox(new NullableToggleButtonModel()) : new JCheckBox();
    checkBox.setHorizontalAlignment(horizontalAlignment);

    return checkBox;
  }
}
