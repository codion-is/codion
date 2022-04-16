/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JFormattedTextField;

/**
 * Builds a formatted text field.
 */
public interface MaskedTextFieldBuilder extends ComponentBuilder<String, JFormattedTextField, MaskedTextFieldBuilder> {

  /**
   * @param mask the format mask string
   * @return this builder instance
   * @see javax.swing.text.MaskFormatter#setMask(String)
   */
  MaskedTextFieldBuilder mask(String mask);

  /**
   * @param valueContainsLiteralCharacters true if the value should contain literal characters
   * @return this builder instance
   * @see javax.swing.text.MaskFormatter#setValueContainsLiteralCharacters(boolean)
   */
  MaskedTextFieldBuilder valueContainsLiteralCharacters(boolean valueContainsLiteralCharacters);

  /**
   * @param placeholder the placeholder
   * @return this builder instance
   * @see javax.swing.text.MaskFormatter#setPlaceholder(String)
   */
  MaskedTextFieldBuilder placeholder(String placeholder);

  /**
   * @param placeholderCharacter the placeholder character
   * @return this builder instance
   * @see javax.swing.text.MaskFormatter#setPlaceholderCharacter(char)
   */
  MaskedTextFieldBuilder placeholderCharacter(char placeholderCharacter);

  /**
   * @param allowsInvalid true if this field should allow invalid values
   * @return this builder instance
   * @see javax.swing.text.MaskFormatter#setAllowsInvalid(boolean)
   */
  MaskedTextFieldBuilder allowsInvalid(boolean allowsInvalid);

  /**
   * @param commitsOnValidEdit true if value should be committed on valid edit
   * @return this builder instance
   * @see javax.swing.text.MaskFormatter#setCommitsOnValidEdit(boolean)
   */
  MaskedTextFieldBuilder commitsOnValidEdit(boolean commitsOnValidEdit);

  /**
   * @param validCharacters the valid characters
   * @return this builder instance
   * @see javax.swing.text.MaskFormatter#setValidCharacters(String)
   */
  MaskedTextFieldBuilder validCharacters(String validCharacters);

  /**
   * @param invalidCharacters the invalid characters
   * @return this builder instance
   * @see javax.swing.text.MaskFormatter#setInvalidCharacters(String)
   */
  MaskedTextFieldBuilder invalidCharacters(String invalidCharacters);

  /**
   * @param overwriteMode true if new characters should overwrite existing characters
   * @return this builder instance
   * @see javax.swing.text.MaskFormatter#setOverwriteMode(boolean)
   */
  MaskedTextFieldBuilder overwriteMode(boolean overwriteMode);

  /**
   * @param columns the number of colums in the text field
   * @return this builder instance
   */
  MaskedTextFieldBuilder columns(int columns);

  /**
   * @param focusLostBehaviour the focus lost behaviour, {@link JFormattedTextField#COMMIT} by default
   * @return this builder instance
   * @see JFormattedTextField#COMMIT
   * @see JFormattedTextField#COMMIT_OR_REVERT
   * @see JFormattedTextField#REVERT
   * @see JFormattedTextField#PERSIST
   */
  MaskedTextFieldBuilder focusLostBehaviour(int focusLostBehaviour);
}
