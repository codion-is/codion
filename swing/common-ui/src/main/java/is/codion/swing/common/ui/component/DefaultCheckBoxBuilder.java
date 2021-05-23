/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JCheckBox;

final class DefaultCheckBoxBuilder extends AbstractComponentBuilder<Boolean, JCheckBox, CheckBoxBuilder>
        implements CheckBoxBuilder {

  private String caption;
  private boolean includeCaption;//todo configuration value for default
  private boolean nullable = false;

  DefaultCheckBoxBuilder(final Value<Boolean> value) {
    super(value);
  }

  @Override
  public CheckBoxBuilder caption(final String caption) {
    this.caption = caption;
    return this;
  }

  @Override
  public CheckBoxBuilder includeCaption(final boolean includeCaption) {
    this.includeCaption = includeCaption;
    return this;
  }

  @Override
  public CheckBoxBuilder nullable(final boolean nullable) {
    this.nullable = nullable;
    return this;
  }

  @Override
  protected JCheckBox buildComponent() {
    return nullable ? createNullableCheckBox() : createCheckBox();
  }

  private NullableCheckBox createNullableCheckBox() {
    final NullableCheckBox checkBox = new NullableCheckBox(new NullableToggleButtonModel(), includeCaption ? caption : null);
    ComponentValues.toggleButton(checkBox).link(value);

    return checkBox;
  }

  private JCheckBox createCheckBox() {
    final JCheckBox checkBox = includeCaption ? new JCheckBox(caption) : new JCheckBox();
    ComponentValues.toggleButton(checkBox).link(value);

    return checkBox;
  }
}
