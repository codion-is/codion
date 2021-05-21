/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.swing.common.ui.value.UpdateOn;

import javax.swing.JFormattedTextField;

/**
 * Builds a formatted text field.
 */
public interface FormattedTextFieldBuilder extends ComponentBuilder<String, JFormattedTextField, FormattedTextFieldBuilder> {

  /**
   * @return this builder instance
   */
  FormattedTextFieldBuilder formatMaskString(String formatMaskString);

  /**
   * @return this builder instance
   */
  FormattedTextFieldBuilder valueContainsLiterals(boolean valueContainsLiterals);

  /**
   * @param updateOn specifies when the underlying value should be updated
   * @return this builder instance
   */
  FormattedTextFieldBuilder updateOn(UpdateOn updateOn);

  /**
   * @return this builder instance
   */
  FormattedTextFieldBuilder columns(int columns);
}
