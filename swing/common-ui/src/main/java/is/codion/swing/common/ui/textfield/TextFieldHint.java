/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import javax.swing.JTextField;

/**
 * A hint text for text fields, that is, text that is shown
 * when the field contains no data, is empty and unfocused.
 * @see TextFieldHint#create(JTextField, String)
 */
public interface TextFieldHint {

  /**
   * @return the search hint string
   */
  String getHintText();

  /**
   * @return true if the field does not have focus and is displayint the hint text
   */
  boolean isHintVisible();

  /**
   * Updates the hint state for the component, showing the hint text if the component
   * contains no text and is not focused. This is done automatically on focus gained/lost events,
   * but sometimes it may be necessary to update manually.
   */
  void updateHint();

  /**
   * Enables the hint text for the given field
   * @param textField the text field
   * @param hintText the hint text
   * @return the {@link TextFieldHint} instance
   */
  static TextFieldHint create(final JTextField textField, final String hintText) {
    return new DefaultTextFieldHint(textField, hintText);
  }
}
