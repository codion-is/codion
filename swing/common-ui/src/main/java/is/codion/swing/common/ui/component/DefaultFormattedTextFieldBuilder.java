/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.FieldFormatter;

import javax.swing.JFormattedTextField;
import java.text.ParseException;

import static java.util.Objects.requireNonNull;

final class DefaultFormattedTextFieldBuilder
        extends AbstractComponentBuilder<String, JFormattedTextField, FormattedTextFieldBuilder>
        implements FormattedTextFieldBuilder {

  private String formatMask;
  private boolean valueContainsLiterals = true;
  private UpdateOn updateOn = UpdateOn.KEYSTROKE;
  private int columns;
  private int focusLostBehaviour = JFormattedTextField.COMMIT;

  DefaultFormattedTextFieldBuilder(final Value<String> linkedValue) {
    super(linkedValue);
  }

  @Override
  public FormattedTextFieldBuilder formatMask(final String formatMask) {
    this.formatMask = requireNonNull(formatMask);
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
  public FormattedTextFieldBuilder focusLostBehaviour(final int focusLostBehaviour) {
    this.focusLostBehaviour = focusLostBehaviour;
    return this;
  }

  @Override
  protected JFormattedTextField buildComponent() {
    try {
      JFormattedTextField textField = new JFormattedTextField(FieldFormatter.create(formatMask, valueContainsLiterals));
      textField.setFocusLostBehavior(focusLostBehaviour);
      textField.setColumns(columns);

      return textField;
    }
    catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected ComponentValue<String, JFormattedTextField> buildComponentValue(final JFormattedTextField component) {
    return ComponentValues.textComponent(component, null, updateOn);
  }

  @Override
  protected void setInitialValue(final JFormattedTextField component, final String initialValue) {
    component.setText(initialValue);
  }
}
