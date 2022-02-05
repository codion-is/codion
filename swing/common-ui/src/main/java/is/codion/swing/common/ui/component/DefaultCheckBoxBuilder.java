/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;

import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

final class DefaultCheckBoxBuilder extends AbstractToggleButtonBuilder<JCheckBox, CheckBoxBuilder> implements CheckBoxBuilder {

  private boolean nullable = false;
  private int horizontalAlignment = SwingConstants.LEADING;

  DefaultCheckBoxBuilder(final Value<Boolean> value) {
    super(value);
  }

  DefaultCheckBoxBuilder(final ValueObserver<Boolean> valueObserver) {
    super(valueObserver);
  }

  @Override
  public CheckBoxBuilder nullable(final boolean nullable) {
    this.nullable = nullable;
    return this;
  }

  @Override
  public CheckBoxBuilder horizontalAlignment(final int horizontalAlignment) {
    this.horizontalAlignment = horizontalAlignment;
    return this;
  }

  @Override
  protected JCheckBox createButton() {
    final JCheckBox checkBox = nullable ? new NullableCheckBox(new NullableToggleButtonModel()) : new JCheckBox();
    checkBox.setHorizontalAlignment(horizontalAlignment);

    return checkBox;
  }
}
