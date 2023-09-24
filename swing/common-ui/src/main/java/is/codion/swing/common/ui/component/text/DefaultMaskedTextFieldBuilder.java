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

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;

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
