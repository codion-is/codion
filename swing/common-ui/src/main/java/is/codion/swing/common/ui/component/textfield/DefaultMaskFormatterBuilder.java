/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import javax.swing.JFormattedTextField;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

final class DefaultMaskFormatterBuilder implements MaskFormatterBuilder{

  private String mask;
  private boolean valueContainsLiteralCharacters = true;
  private String placeholder;
  private char placeholderCharacter = ' ';
  private boolean allowsInvalid = false;
  private boolean commitsOnValidEdit = true;
  private String validCharacters;
  private String invalidCharacters;
  private boolean overwriteMode = true;
  private boolean emptyStringToNullValue = true;

  @Override
  public MaskFormatterBuilder mask(String mask) {
    this.mask = requireNonNull(mask);
    return this;
  }

  @Override
  public MaskFormatterBuilder valueContainsLiteralCharacters(boolean valueContainsLiteralCharacters) {
    this.valueContainsLiteralCharacters = valueContainsLiteralCharacters;
    return this;
  }

  @Override
  public MaskFormatterBuilder placeholder(String placeholder) {
    this.placeholder = requireNonNull(placeholder);
    return this;
  }

  @Override
  public MaskFormatterBuilder placeholderCharacter(char placeholderCharacter) {
    this.placeholderCharacter = placeholderCharacter;
    return this;
  }

  @Override
  public MaskFormatterBuilder allowsInvalid(boolean allowsInvalid) {
    this.allowsInvalid = allowsInvalid;
    return this;
  }

  @Override
  public MaskFormatterBuilder commitsOnValidEdit(boolean commitsOnValidEdit) {
    this.commitsOnValidEdit = commitsOnValidEdit;
    return this;
  }

  @Override
  public MaskFormatterBuilder validCharacters(String validCharacters) {
    this.validCharacters = requireNonNull(validCharacters);
    return this;
  }

  @Override
  public MaskFormatterBuilder invalidCharacters(String invalidCharacters) {
    this.invalidCharacters = requireNonNull(invalidCharacters);
    return this;
  }

  @Override
  public MaskFormatterBuilder overwriteMode(boolean overwriteMode) {
    this.overwriteMode = overwriteMode;
    return this;
  }

  @Override
  public MaskFormatterBuilder emptyStringToNullValue(boolean emptyStringToNullValue) {
    this.emptyStringToNullValue = emptyStringToNullValue;
    return this;
  }

  @Override
  public MaskFormatter build() throws ParseException {
    return new DefaultMaskFormatter(this);
  }

  private static final class DefaultMaskFormatter extends MaskFormatter {

    private final boolean emptyStringToNullValue;

    private DefaultMaskFormatter(DefaultMaskFormatterBuilder builder) throws ParseException {
      super(builder.mask);
      this.emptyStringToNullValue = builder.emptyStringToNullValue;
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
    public Object stringToValue(String value) throws ParseException {
      if (emptyStringToNullValue && Objects.equals(value, valueToString(""))) {
        return null;
      }

      return super.stringToValue(value);
    }

    /**
     * Somewhat of a hack to keep the current field selection and caret position when
     * the field gains focus, in case the content length has not changed.<br>
     * <a href="https://stackoverflow.com/a/2202073/317760">https://stackoverflow.com/a/2202073/317760</a>
     * @param field the field
     */
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
