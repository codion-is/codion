/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.jminor.swing.common.model.DocumentAdapter;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * Implements a hint text for text fields, that is, text that is shown
 * when the field contains no data, is empty and unfocused.
 */
public final class TextFieldHint {

  private final JTextField textField;
  private final String hintText;
  private final Color defaultForegroundColor;
  private final Color hintForegroundColor;

  /**
   * Instantiates a new TextFieldHint for the given field.
   * @param textField the text field
   * @param hintText the hint text
   * @param hintForegroundColor the font color for the hint text
   */
  private TextFieldHint(final JTextField textField, final String hintText, final Color hintForegroundColor) {
    requireNonNull(textField, "textField");
    if (nullOrEmpty(hintText)) {
      throw new IllegalArgumentException("Hint text is null or empty");
    }
    requireNonNull(hintForegroundColor, "hintForegroundColor");
    this.textField = textField;
    this.hintText = hintText;
    this.defaultForegroundColor = textField.getForeground();
    this.hintForegroundColor = hintForegroundColor;
    this.textField.addFocusListener(initializeFocusListener());
    this.textField.getDocument().addDocumentListener(initializeDocumentListener());
    updateState();
  }

  /**
   * @return the search hint string
   */
  public String getHintText() {
    return hintText;
  }

  /**
   * @return true if the hint text is visible
   */
  public boolean isHintTextVisible() {
    return textField.getText().equals(hintText);
  }

  /**
   * Enables the search hint for the given field
   * @param textField the text field
   * @param hintText the hint text
   * @return the TextFieldHint instance
   */
  public static TextFieldHint enable(final JTextField textField, final String hintText) {
    return enable(textField, hintText, Color.LIGHT_GRAY);
  }

  /**
   * Enables the search hint for the given field
   * @param textField the text field
   * @param hintText the hint text
   * @param hintForegroundColor the font color for the hint text
   * @return the TextFieldHint instance
   */
  public static TextFieldHint enable(final JTextField textField, final String hintText,
                                     final Color hintForegroundColor) {
    return new TextFieldHint(textField, hintText, hintForegroundColor);
  }

  private FocusListener initializeFocusListener() {
    return new FocusListener() {
      @Override
      public void focusGained(final FocusEvent e) {
        updateState();
      }
      @Override
      public void focusLost(final FocusEvent e) {
        updateState();
      }
    };
  }

  private DocumentAdapter initializeDocumentListener() {
    return new DocumentAdapter() {
      @Override
      public void contentsChanged(final DocumentEvent e) {
        updateColor();
      }
    };
  }

  /**
   * Updates the hint state for the component,
   * showing the hint text if the component contains no text
   * and is not focused.
   */
  private void updateState() {
    final boolean hasFocus = textField.hasFocus();
    final boolean hideHint = hasFocus && textField.getText().equals(hintText);
    final boolean showHint = !hasFocus && textField.getText().length() == 0;
    if (hideHint) {
      textField.setText("");
    }
    else if (showHint) {
      textField.setText(hintText);
    }
    updateColor();
  }

  private void updateColor() {
    final boolean hintForeground = !textField.hasFocus() && isHintTextVisible();
    textField.setForeground(hintForeground ? hintForegroundColor : defaultForegroundColor);
  }
}
