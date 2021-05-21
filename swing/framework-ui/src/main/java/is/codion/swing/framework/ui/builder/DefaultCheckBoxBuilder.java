/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.model.checkbox.NullableToggleButtonModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;
import is.codion.swing.common.ui.value.ComponentValues;

import javax.swing.JCheckBox;
import java.awt.Dimension;
import java.util.function.Consumer;

final class DefaultCheckBoxBuilder extends AbstractComponentBuilder<Boolean, JCheckBox> implements CheckBoxBuilder {

  private boolean includeCaption;
  private boolean nullable = false;

  DefaultCheckBoxBuilder(final Property<Boolean> attribute, final Value<Boolean> value) {
    super(attribute, value);
  }

  @Override
  public CheckBoxBuilder preferredHeight(final int preferredHeight) {
    return (CheckBoxBuilder) super.preferredHeight(preferredHeight);
  }

  @Override
  public CheckBoxBuilder preferredWidth(final int preferredWidth) {
    return (CheckBoxBuilder) super.preferredWidth(preferredWidth);
  }

  @Override
  public CheckBoxBuilder preferredSize(final Dimension preferredSize) {
    return (CheckBoxBuilder) super.preferredSize(preferredSize);
  }

  @Override
  public CheckBoxBuilder transferFocusOnEnter(final boolean transferFocusOnEnter) {
    return (CheckBoxBuilder) super.transferFocusOnEnter(transferFocusOnEnter);
  }

  @Override
  public CheckBoxBuilder enabledState(final StateObserver enabledState) {
    return (CheckBoxBuilder) super.enabledState(enabledState);
  }

  @Override
  public CheckBoxBuilder onBuild(final Consumer<JCheckBox> onBuild) {
    return (CheckBoxBuilder) super.onBuild(onBuild);
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
  public JCheckBox build() {
    final JCheckBox checkBox;
    if (nullable) {
      checkBox = createNullableCheckBox();
    }
    else {
      checkBox = createCheckBox();
    }
    setPreferredSize(checkBox);
    onBuild(checkBox);
    if (transferFocusOnEnter) {
      Components.transferFocusOnEnter(checkBox);
    }

    return checkBox;
  }

  private NullableCheckBox createNullableCheckBox() {
    if (!property.isNullable()) {
      throw new IllegalArgumentException("Nullable boolean attribute required for createNullableCheckBox()");
    }
    final NullableCheckBox checkBox = new NullableCheckBox(new NullableToggleButtonModel(),
            includeCaption ? property.getCaption() : null);
    ComponentValues.toggleButton(checkBox).link(value);

    return setDescriptionAndEnabledState(checkBox, property.getDescription(), enabledState);
  }

  private JCheckBox createCheckBox() {
    final JCheckBox checkBox = includeCaption ? new JCheckBox(property.getCaption()) : new JCheckBox();
    ComponentValues.toggleButton(checkBox).link(value);

    return setDescriptionAndEnabledState(checkBox, property.getDescription(), enabledState);
  }
}
