/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.textfield;

import org.jminor.common.model.Util;

import javax.swing.JTextField;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Implements a hint text for text fields, that is, text that is shown
 * when the field contains no data, is empty an unfocused.
 */
public final class TextFieldHint {

  private final JTextField txtField;
  private final String hintText;
  private final Color defaultForegroundColor;
  private final Color hintForegroundColor;

  /**
   * Instantiates a new TextFieldHint for the given field.
   * @param txtField the text field
   * @param hintText the hint text
   * @param hintForegroundColor the font color for the hint text
   */
  private TextFieldHint(final JTextField txtField, final String hintText, final Color hintForegroundColor) {
    Util.rejectNullValue(txtField, "txtField");
    Util.rejectNullValue(hintText, "hintText");
    Util.rejectNullValue(hintForegroundColor, "hintForegroundColor");
    if (hintText.isEmpty()) {
      throw new IllegalArgumentException("Hint text is null or empty");
    }
    this.txtField = txtField;
    this.hintText = hintText;
    this.defaultForegroundColor = txtField.getForeground();
    this.hintForegroundColor = hintForegroundColor;
    this.txtField.addFocusListener(initializeFocusListener());
    updateState();
  }

  /**
   * @return the search hint string
   */
  public String getHintText() {
    return hintText;
  }

  /**
   * Updates the hint state for the component,
   * showing the hint text if the component contains no text
   * and is not focused.
   */
  public void updateState() {
    final boolean hasFocus = txtField.hasFocus();
    final boolean hideHint = hasFocus && txtField.getText().equals(hintText);
    final boolean showHint = !hasFocus && txtField.getText().isEmpty();
    if (hideHint) {
      txtField.setText("");
    }
    else if (showHint) {
      txtField.setText(hintText);
    }
    final boolean specificForeground = !hasFocus && isHintTextVisible();
    txtField.setForeground(specificForeground ? hintForegroundColor : defaultForegroundColor);
  }

  /**
   * @return true if the hint text is visible
   */
  public boolean isHintTextVisible() {
    return txtField.getText().equals(hintText);
  }

  /**
   * Enables the search hint for the given field
   * @param txtField the text field
   * @param hintText the hint text
   * @return the TextFieldHint instance
   */
  public static TextFieldHint enable(final JTextField txtField, final String hintText) {
    return enable(txtField, hintText, Color.LIGHT_GRAY);
  }

  /**
   * Enables the search hint for the given field
   * @param txtField the text field
   * @param hintText the hint text
   * @param hintForegroundColor the font color for the hint text
   * @return the TextFieldHint instance
   */
  public static TextFieldHint enable(final JTextField txtField, final String hintText,
                                     final Color hintForegroundColor) {
    return new TextFieldHint(txtField, hintText, hintForegroundColor);
  }

  private FocusListener initializeFocusListener() {
    return new FocusListener() {
      /** {@inheritDoc} */
      public void focusGained(final FocusEvent e) {
        updateState();
      }
      /** {@inheritDoc} */
      public void focusLost(final FocusEvent e) {
        updateState();
      }
    };
  }
}
