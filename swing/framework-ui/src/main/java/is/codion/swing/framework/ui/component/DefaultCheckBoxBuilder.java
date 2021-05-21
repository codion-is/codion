/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JCheckBox;

final class DefaultCheckBoxBuilder extends AbstractComponentBuilder<Boolean, JCheckBox, CheckBoxBuilder>
        implements CheckBoxBuilder {

  private boolean includeCaption;
  private boolean nullable = false;

  DefaultCheckBoxBuilder(final Property<Boolean> attribute, final Value<Boolean> value) {
    super(attribute, value);
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
    if (!property.isNullable()) {
      throw new IllegalArgumentException("Nullable boolean attribute required for createNullableCheckBox()");
    }
    final NullableCheckBox checkBox = new NullableCheckBox(new NullableToggleButtonModel(),
            includeCaption ? property.getCaption() : null);
    ComponentValues.toggleButton(checkBox).link(value);

    return checkBox;
  }

  private JCheckBox createCheckBox() {
    final JCheckBox checkBox = includeCaption ? new JCheckBox(property.getCaption()) : new JCheckBox();
    ComponentValues.toggleButton(checkBox).link(value);

    return checkBox;
  }
}
