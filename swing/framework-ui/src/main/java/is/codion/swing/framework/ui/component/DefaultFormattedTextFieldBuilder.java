/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.textfield.TextFields;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.common.ui.value.UpdateOn;

import javax.swing.JFormattedTextField;

import static is.codion.swing.common.ui.textfield.TextFields.createFormattedField;
import static java.util.Objects.requireNonNull;

final class DefaultFormattedTextFieldBuilder
        extends AbstractComponentBuilder<String, JFormattedTextField, FormattedTextFieldBuilder>
        implements FormattedTextFieldBuilder {

  private String formatMaskString;
  private boolean valueContainsLiterals = true;
  private UpdateOn updateOn = UpdateOn.KEYSTROKE;
  private int columns;

  DefaultFormattedTextFieldBuilder(final Property<String> attribute, final Value<String> value) {
    super(attribute, value);
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
  protected JFormattedTextField buildComponent() {
    final JFormattedTextField textField = createFormattedField(formatMaskString,
            valueContainsLiterals ? TextFields.ValueContainsLiterals.YES : TextFields.ValueContainsLiterals.NO);
    ComponentValues.textComponent(textField, null, updateOn).link(value);
    textField.setColumns(columns);

    return textField;
  }
}
