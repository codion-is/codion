/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.swing.common.ui.value.UpdateOn;

import javax.swing.JFormattedTextField;

/**
 * Builds a formatted text field.
 */
public interface FormattedTextFieldBuilder extends ComponentBuilder<String, JFormattedTextField, FormattedTextFieldBuilder> {

  /**
   * @param formatMaskString the format mask string
   * @return this builder instance
   */
  FormattedTextFieldBuilder formatMaskString(String formatMaskString);

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
}
