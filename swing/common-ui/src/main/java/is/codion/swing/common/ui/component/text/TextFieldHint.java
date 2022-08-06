/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.JTextField;

/**
 * A hint text for text fields, shown when the field is empty and unfocused.
 * Note that the hint text is inserted into the text field document.
 * @see TextFieldHint#create(JTextField, String)
 */
public interface TextFieldHint {

  /**
   * @return the search hint string
   */
  String hintText();

  /**
   * @return true if the field does not have focus and is displaying the hint text
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
  static TextFieldHint create(JTextField textField, String hintText) {
    return new DefaultTextFieldHint(textField, hintText);
  }
}
