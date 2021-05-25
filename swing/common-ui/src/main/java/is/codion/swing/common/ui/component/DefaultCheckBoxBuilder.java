/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

final class DefaultCheckBoxBuilder extends AbstractComponentBuilder<Boolean, JCheckBox, CheckBoxBuilder>
        implements CheckBoxBuilder {

  private String caption;
  private boolean includeCaption = true;
  private boolean nullable = false;
  private int horizontalAlignment = SwingConstants.LEADING;

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
  public CheckBoxBuilder horizontalAlignment(final int horizontalAlignment) {
    this.horizontalAlignment = horizontalAlignment;
    return this;
  }

  @Override
  protected JCheckBox buildComponent() {
    return nullable ? createNullableCheckBox() : createCheckBox();
  }

  @Override
  protected ComponentValue<Boolean, JCheckBox> buildComponentValue(final JCheckBox component) {
    return ComponentValues.toggleButton(component);
  }

  @Override
  protected void setInitialValue(final JCheckBox component, final Boolean initialValue) {
    component.setSelected(initialValue);
  }

  private NullableCheckBox createNullableCheckBox() {
    final NullableCheckBox checkBox = new NullableCheckBox(new NullableToggleButtonModel(), includeCaption ? caption : null);
    checkBox.setHorizontalAlignment(horizontalAlignment);

    return checkBox;
  }

  private JCheckBox createCheckBox() {
    final JCheckBox checkBox = includeCaption ? new JCheckBox(caption) : new JCheckBox();
    checkBox.setHorizontalAlignment(horizontalAlignment);

    return checkBox;
  }
}
