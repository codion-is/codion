/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.swing.common.model.textfield.DocumentAdapter;

import javax.swing.JTextField;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

final class DefaultTextFieldHint implements TextFields.Hint {

  private final JTextField textField;
  private final String hintText;
  private final Color defaultForegroundColor;
  private final Color hintForegroundColor;

  DefaultTextFieldHint(final JTextField textField, final String hintText) {
    requireNonNull(textField, "textField");
    if (nullOrEmpty(hintText)) {
      throw new IllegalArgumentException("Hint text is null or empty");
    }
    this.textField = textField;
    this.hintText = hintText;
    this.defaultForegroundColor = textField.getForeground();
    this.hintForegroundColor = defaultForegroundColor.brighter().brighter().brighter();
    this.textField.addFocusListener(initializeFocusListener());
    this.textField.getDocument().addDocumentListener((DocumentAdapter) e -> updateColor());
    updateHint();
  }

  @Override
  public String getHintText() {
    return hintText;
  }

  @Override
  public boolean isHintVisible() {
    return textField.getText().equals(hintText);
  }

  @Override
  public void updateHint() {
    final boolean hasFocus = textField.hasFocus();
    final boolean hideHint = hasFocus && textField.getText().equals(hintText);
    final boolean showHint = !hasFocus && textField.getText().isEmpty();
    if (hideHint) {
      textField.setText("");
    }
    else if (showHint) {
      textField.setText(hintText);
    }
    updateColor();
  }

  private FocusListener initializeFocusListener() {
    return new FocusListener() {
      @Override
      public void focusGained(final FocusEvent e) {
        updateHint();
      }

      @Override
      public void focusLost(final FocusEvent e) {
        updateHint();
      }
    };
  }

  private void updateColor() {
    final boolean hintForeground = !textField.hasFocus() && isHintVisible();
    textField.setForeground(hintForeground ? hintForegroundColor : defaultForegroundColor);
  }
}
