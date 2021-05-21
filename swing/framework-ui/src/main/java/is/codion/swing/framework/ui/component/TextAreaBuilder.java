/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.swing.common.ui.value.UpdateOn;

import javax.swing.JTextArea;

/**
 * Builds a JTextArea.
 */
public interface TextAreaBuilder extends ComponentBuilder<String, JTextArea, TextAreaBuilder> {

  /**
   * @param updateOn specifies when the underlying value should be updated
   * @return this builder instance
   */
  TextAreaBuilder updateOn(UpdateOn updateOn);

  /**
   * @param rows the number of rows in the text area
   * @return this builder instance
   */
  TextAreaBuilder rows(int rows);

  /**
   * @param columns the number of colums in the text area
   * @return this builder instance
   */
  TextAreaBuilder columns(int columns);
}
