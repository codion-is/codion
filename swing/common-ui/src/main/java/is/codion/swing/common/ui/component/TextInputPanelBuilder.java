/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.textfield.TextInputPanel;

/**
 * Builds a TextInputPanel.
 */
public interface TextInputPanelBuilder extends ComponentBuilder<String, TextInputPanel, TextInputPanelBuilder>,
        TextInputPanel.Builder<TextInputPanelBuilder> {

  /**
   * @param updateOn specifies when the underlying value should be updated
   * @return this builder instance
   */
  TextInputPanelBuilder updateOn(UpdateOn updateOn);

  /**
   * @param columns the number of colums in the text field
   * @return this builder instance
   */
  TextInputPanelBuilder columns(int columns);

  /**
   * @param upperCase if true the text component convert all lower case input to upper case
   * @return this builder instance
   */
  TextInputPanelBuilder upperCase(boolean upperCase);

  /**
   * @param lowerCase if true the text component convert all upper case input to lower case
   * @return this builder instance
   */
  TextInputPanelBuilder lowerCase(boolean lowerCase);

  /**
   * Makes the text field select all when it gains focus
   * @param selectAllOnFocusGained if true the component will select contents on focus gained
   * @return this builder instance
   */
  TextInputPanelBuilder selectAllOnFocusGained(boolean selectAllOnFocusGained);
}
