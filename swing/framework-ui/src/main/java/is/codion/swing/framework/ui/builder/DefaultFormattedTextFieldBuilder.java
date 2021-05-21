/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.common.ui.value.UpdateOn;

import javax.swing.JFormattedTextField;
import java.awt.Dimension;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.textfield.TextFields.createFormattedField;
import static java.util.Objects.requireNonNull;

final class DefaultFormattedTextFieldBuilder extends AbstractComponentBuilder<String, JFormattedTextField> implements FormattedTextFieldBuilder {

  private String formatMaskString;
  private boolean valueContainsLiterals = true;
  private UpdateOn updateOn = UpdateOn.KEYSTROKE;
  private int columns;

  DefaultFormattedTextFieldBuilder(final Property<String> attribute, final Value<String> value) {
    super(attribute, value);
  }

  @Override
  public FormattedTextFieldBuilder preferredHeight(final int preferredHeight) {
    return (FormattedTextFieldBuilder) super.preferredHeight(preferredHeight);
  }

  @Override
  public FormattedTextFieldBuilder preferredWidth(final int preferredWidth) {
    return (FormattedTextFieldBuilder) super.preferredWidth(preferredWidth);
  }

  @Override
  public FormattedTextFieldBuilder preferredSize(final Dimension preferredSize) {
    return (FormattedTextFieldBuilder) super.preferredSize(preferredSize);
  }

  @Override
  public FormattedTextFieldBuilder transferFocusOnEnter(final boolean transferFocusOnEnter) {
    return (FormattedTextFieldBuilder) super.transferFocusOnEnter(transferFocusOnEnter);
  }

  @Override
  public FormattedTextFieldBuilder enabledState(final StateObserver enabledState) {
    return (FormattedTextFieldBuilder) super.enabledState(enabledState);
  }

  @Override
  public FormattedTextFieldBuilder onBuild(final Consumer<JFormattedTextField> onBuild) {
    return (FormattedTextFieldBuilder) super.onBuild(onBuild);
  }

  @Override
  public FormattedTextFieldBuilder formatMaskString(final String formatMaskString) {
    this.formatMaskString = requireNonNull(formatMaskString);
    return this;
  }

  @Override
  public FormattedTextFieldBuilder valueContainsLiterals(final boolean valueContainsLiterals) {
    this.valueContainsLiterals = valueContainsLiterals;
    return this;
  }

  @Override
  public FormattedTextFieldBuilder updateOn(final UpdateOn updateOn) {
    this.updateOn = requireNonNull(updateOn);
    return this;
  }

  @Override
  public FormattedTextFieldBuilder columns(final int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  public JFormattedTextField build() {
    final JFormattedTextField textField = setDescriptionAndEnabledState(createFormattedField(formatMaskString,
            valueContainsLiterals ? TextFields.ValueContainsLiterals.YES : TextFields.ValueContainsLiterals.NO),
            property.getDescription(), enabledState);
    ComponentValues.textComponent(textField, null, updateOn).link(value);
    setPreferredSize(textField);
    onBuild(textField);
    textField.setColumns(columns);
    if (transferFocusOnEnter) {
      Components.transferFocusOnEnter(textField);
    }

    return textField;
  }
}
