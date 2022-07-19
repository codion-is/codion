/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.ComponentValue;

import javax.swing.JFormattedTextField;
import java.text.ParseException;

final class DefaultMaskedTextFieldBuilder
        extends AbstractComponentBuilder<String, JFormattedTextField, MaskedTextFieldBuilder>
        implements MaskedTextFieldBuilder {

  private final MaskFormatterBuilder maskFormatterBuilder = MaskFormatterBuilder.builder();

  private int columns = -1;
  private int focusLostBehaviour = JFormattedTextField.COMMIT;

  DefaultMaskedTextFieldBuilder(Value<String> linkedValue) {
    super(linkedValue);
  }

  @Override
  public MaskedTextFieldBuilder mask(String mask) {
    maskFormatterBuilder.mask(mask);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder valueContainsLiteralCharacters(boolean valueContainsLiteralCharacters) {
    maskFormatterBuilder.valueContainsLiteralCharacters(valueContainsLiteralCharacters);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder placeholder(String placeholder) {
    maskFormatterBuilder.placeholder(placeholder);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder placeholderCharacter(char placeholderCharacter) {
    maskFormatterBuilder.placeholderCharacter(placeholderCharacter);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder allowsInvalid(boolean allowsInvalid) {
    maskFormatterBuilder.allowsInvalid(allowsInvalid);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder commitsOnValidEdit(boolean commitsOnValidEdit) {
    maskFormatterBuilder.commitsOnValidEdit(commitsOnValidEdit);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder validCharacters(String validCharacters) {
    maskFormatterBuilder.validCharacters(validCharacters);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder invalidCharacters(String invalidCharacters) {
    maskFormatterBuilder.invalidCharacters(invalidCharacters);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder overwriteMode(boolean overwriteMode) {
    maskFormatterBuilder.overwriteMode(overwriteMode);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder emptyStringToNullValue(boolean emptyStringToNullValue) {
    maskFormatterBuilder.emptyStringToNullValue(emptyStringToNullValue);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder invalidStringToNullValue(boolean invalidStringToNullValue) {
    maskFormatterBuilder.invalidStringToNullValue(invalidStringToNullValue);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder columns(int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  public MaskedTextFieldBuilder focusLostBehaviour(int focusLostBehaviour) {
    this.focusLostBehaviour = focusLostBehaviour;
    return this;
  }

  @Override
  protected JFormattedTextField createComponent() {
    try {
      JFormattedTextField textField = new JFormattedTextField(maskFormatterBuilder.build());
      textField.setFocusLostBehavior(focusLostBehaviour);
      if (columns != -1) {
        textField.setColumns(columns);
      }

      return textField;
    }
    catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected ComponentValue<String, JFormattedTextField> createComponentValue(JFormattedTextField component) {
    return new MaskedTextFieldValue<>(component);
  }

  @Override
  protected void setInitialValue(JFormattedTextField component, String initialValue) {
    component.setText(initialValue);
  }
}
