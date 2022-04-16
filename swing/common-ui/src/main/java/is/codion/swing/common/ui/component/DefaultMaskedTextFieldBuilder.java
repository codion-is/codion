/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;

import javax.swing.JFormattedTextField;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;

import static java.util.Objects.requireNonNull;

final class DefaultMaskedTextFieldBuilder
        extends AbstractComponentBuilder<String, JFormattedTextField, MaskedTextFieldBuilder>
        implements MaskedTextFieldBuilder {

  private String mask;
  private boolean valueContainsLiteralCharacters = true;
  private String placeholder;
  private char placeholderCharacter = ' ';
  private boolean allowsInvalid = false;
  private boolean commitsOnValidEdit = true;
  private String validCharacters;
  private String invalidCharacters;
  private boolean overwriteMode = true;
  private int columns;
  private int focusLostBehaviour = JFormattedTextField.COMMIT;

  DefaultMaskedTextFieldBuilder(Value<String> linkedValue) {
    super(linkedValue);
  }

  @Override
  public MaskedTextFieldBuilder mask(String mask) {
    this.mask = requireNonNull(mask);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder valueContainsLiteralCharacters(boolean valueContainsLiteralCharacters) {
    this.valueContainsLiteralCharacters = valueContainsLiteralCharacters;
    return this;
  }

  @Override
  public MaskedTextFieldBuilder placeholder(String placeholder) {
    this.placeholder = requireNonNull(placeholder);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder placeholderCharacter(char placeholderCharacter) {
    this.placeholderCharacter = placeholderCharacter;
    return this;
  }

  @Override
  public MaskedTextFieldBuilder allowsInvalid(boolean allowsInvalid) {
    this.allowsInvalid = allowsInvalid;
    return this;
  }

  @Override
  public MaskedTextFieldBuilder commitsOnValidEdit(boolean commitsOnValidEdit) {
    this.commitsOnValidEdit = commitsOnValidEdit;
    return this;
  }

  @Override
  public MaskedTextFieldBuilder validCharacters(String validCharacters) {
    this.validCharacters = requireNonNull(validCharacters);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder invalidCharacters(String invalidCharacters) {
    this.invalidCharacters = requireNonNull(invalidCharacters);
    return this;
  }

  @Override
  public MaskedTextFieldBuilder overwriteMode(boolean overwriteMode) {
    this.overwriteMode = overwriteMode;
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
      JFormattedTextField textField = new JFormattedTextField(new DefaultMaskFormatter(this));
      textField.setFocusLostBehavior(focusLostBehaviour);
      if (columns > 0) {
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
    return new FormattedTextFieldValue<>(component);
  }

  @Override
  protected void setInitialValue(JFormattedTextField component, String initialValue) {
    component.setText(initialValue);
  }

  private static final class DefaultMaskFormatter extends MaskFormatter {

    private DefaultMaskFormatter(DefaultMaskedTextFieldBuilder builder) throws ParseException {
      super(builder.mask);
      setValueContainsLiteralCharacters(builder.valueContainsLiteralCharacters);
      setPlaceholder(builder.placeholder);
      setPlaceholderCharacter(builder.placeholderCharacter);
      setAllowsInvalid(builder.allowsInvalid);
      setCommitsOnValidEdit(builder.commitsOnValidEdit);
      setValidCharacters(builder.validCharacters);
      setInvalidCharacters(builder.invalidCharacters);
      setOverwriteMode(builder.overwriteMode);
    }

    @Override
    public void install(JFormattedTextField field) {
      int previousLength = field.getDocument().getLength();
      int currentCaretPosition = field.getCaretPosition();
      int currentSelectionStart = field.getSelectionStart();
      int currentSelectionEnd = field.getSelectionEnd();
      super.install(field);
      if (previousLength == field.getDocument().getLength()) {
        if (currentSelectionEnd - currentSelectionStart > 0) {
          field.setCaretPosition(currentSelectionStart);
          field.moveCaretPosition(currentSelectionEnd);
        }
        else {
          field.setCaretPosition(currentCaretPosition);
        }
      }
    }
  }
}
