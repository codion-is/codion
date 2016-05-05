/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.jminor.common.model.Util;
import org.jminor.swing.common.model.DocumentAdapter;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Implements a hint text for text fields, that is, text that is shown
 * when the field contains no data, is empty and unfocused.
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
    if (Util.nullOrEmpty(hintText)) {
      throw new IllegalArgumentException("Hint text is null or empty");
    }
    Util.rejectNullValue(hintForegroundColor, "hintForegroundColor");
    this.txtField = txtField;
    this.hintText = hintText;
    this.defaultForegroundColor = txtField.getForeground();
    this.hintForegroundColor = hintForegroundColor;
    this.txtField.addFocusListener(initializeFocusListener());
    this.txtField.getDocument().addDocumentListener(initializeDocumentListener());
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
    final boolean hasFocus = txtField.hasFocus();
    final boolean hideHint = hasFocus && txtField.getText().equals(hintText);
    final boolean showHint = !hasFocus && txtField.getText().length() == 0;
    if (hideHint) {
      txtField.setText("");
    }
    else if (showHint) {
      txtField.setText(hintText);
    }
    updateColor();
  }

  private void updateColor() {
    final boolean hintForeground = !txtField.hasFocus() && isHintTextVisible();
    txtField.setForeground(hintForeground ? hintForegroundColor : defaultForegroundColor);
  }
}
