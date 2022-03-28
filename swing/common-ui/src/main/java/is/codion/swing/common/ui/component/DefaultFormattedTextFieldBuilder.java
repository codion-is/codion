/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.textfield.FieldFormatter;

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

  DefaultFormattedTextFieldBuilder(Value<String> linkedValue) {
    super(linkedValue);
  }

  @Override
  public FormattedTextFieldBuilder formatMask(String formatMask) {
    this.formatMask = requireNonNull(formatMask);
    return this;
  }

  @Override
  public FormattedTextFieldBuilder valueContainsLiterals(boolean valueContainsLiterals) {
    this.valueContainsLiterals = valueContainsLiterals;
    return this;
  }

  @Override
  public FormattedTextFieldBuilder updateOn(UpdateOn updateOn) {
    this.updateOn = requireNonNull(updateOn);
    return this;
  }

  @Override
  public FormattedTextFieldBuilder columns(int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  public FormattedTextFieldBuilder focusLostBehaviour(int focusLostBehaviour) {
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
  protected ComponentValue<String, JFormattedTextField> buildComponentValue(JFormattedTextField component) {
    return new FormattedTextComponentValue<>(component, null, updateOn);
  }

  @Override
  protected void setInitialValue(JFormattedTextField component, String initialValue) {
    component.setText(initialValue);
  }
}
