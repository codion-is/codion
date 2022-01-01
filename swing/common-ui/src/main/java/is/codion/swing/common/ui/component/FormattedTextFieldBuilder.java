/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JFormattedTextField;

/**
 * Builds a formatted text field.
 */
public interface FormattedTextFieldBuilder extends ComponentBuilder<String, JFormattedTextField, FormattedTextFieldBuilder> {

  /**
   * @param formatMask the format mask string
   * @return this builder instance
   */
  FormattedTextFieldBuilder formatMask(String formatMask);

  /**
   * @param valueContainsLiterals true if the value should contain literal characters
   * @return this builder instance
   */
  FormattedTextFieldBuilder valueContainsLiterals(boolean valueContainsLiterals);

  /**
   * @param updateOn specifies when the underlying value should be updated
   * @return this builder instance
   */
  FormattedTextFieldBuilder updateOn(UpdateOn updateOn);

  /**
   * @param columns the number of colums in the text field
   * @return this builder instance
   */
  FormattedTextFieldBuilder columns(int columns);

  /**
   * @param focusLostBehaviour the focus lost behaviour, JFormattedTextField.COMMIT by default
   * @return this builder instance
   * @see JFormattedTextField#COMMIT
   * @see JFormattedTextField#COMMIT_OR_REVERT
   * @see JFormattedTextField#REVERT
   * @see JFormattedTextField#PERSIST
   */
  FormattedTextFieldBuilder focusLostBehaviour(int focusLostBehaviour);
}
