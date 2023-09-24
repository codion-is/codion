/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.JFormattedTextField;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

final class DefaultMaskFormatter extends MaskFormatter {

  private final boolean emptyStringToNullValue;
  private final boolean invalidStringToNullValue;

  private DefaultMaskFormatter(DefaultMaskFormatterBuilder builder) throws ParseException {
    super(builder.mask);
    this.emptyStringToNullValue = builder.emptyStringToNullValue;
    this.invalidStringToNullValue = builder.invalidStringToNullValue;
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

    try {
      return super.stringToValue(value);
    }
    catch (ParseException e) {
      if (invalidStringToNullValue) {
        return null;
      }

      throw e;
    }
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

  static final class DefaultMaskFormatterBuilder implements MaskFormatterBuilder {

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
    private boolean invalidStringToNullValue = false;

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
    public MaskFormatterBuilder invalidStringToNullValue(boolean invalidStringToNullValue) {
      this.invalidStringToNullValue = invalidStringToNullValue;
      return this;
    }

    @Override
    public MaskFormatter build() throws ParseException {
      return new DefaultMaskFormatter(this);
    }
  }
}
