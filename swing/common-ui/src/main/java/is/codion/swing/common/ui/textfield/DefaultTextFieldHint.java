/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.swing.common.model.textfield.DocumentAdapter;

import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import static is.codion.common.Util.nullOrEmpty;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.util.Objects.requireNonNull;

final class DefaultTextFieldHint implements TextFieldHint {

  private final JTextField textField;
  private final String hintText;

  private Color foregroundColor;
  private Color hintForegroundColor;

  DefaultTextFieldHint(JTextField textField, String hintText) {
    requireNonNull(textField, "textField");
    if (nullOrEmpty(hintText)) {
      throw new IllegalArgumentException("Hint text is null or empty");
    }
    this.textField = textField;
    this.hintText = hintText;
    this.textField.addFocusListener(initializeFocusListener());
    this.textField.getDocument().addDocumentListener((DocumentAdapter) e -> updateColor());
    configureColors();
    textField.addPropertyChangeListener("UI", event -> configureColors());
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
    boolean hasFocus = textField.hasFocus();
    boolean hideHint = hasFocus && textField.getText().equals(hintText);
    boolean showHint = !hasFocus && textField.getText().isEmpty();
    if (hideHint) {
      textField.setText("");
    }
    else if (showHint) {
      textField.setText(hintText);
      textField.moveCaretPosition(0);
    }
    updateColor();
  }

  private FocusListener initializeFocusListener() {
    return new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        updateHint();
      }

      @Override
      public void focusLost(FocusEvent e) {
        updateHint();
      }
    };
  }

  private void updateColor() {
    boolean hintForeground = !textField.hasFocus() && isHintVisible();
    textField.setForeground(hintForeground ? hintForegroundColor : foregroundColor);
  }

  private void configureColors() {
    Color foreground = UIManager.getColor("TextField.foreground");
    Color background = UIManager.getColor("TextField.background");
    foregroundColor = foreground;
    hintForegroundColor = getHintForegroundColor(background, foreground);
    updateColor();
  }

  private static Color getHintForegroundColor(Color background, Color foreground) {
    //simplistic averaging of background and foreground
    int r = (int) sqrt((pow(background.getRed(), 2) + pow(foreground.getRed(), 2)) / 2);
    int g = (int) sqrt((pow(background.getGreen(), 2) + pow(foreground.getGreen(), 2)) / 2);
    int b = (int) sqrt((pow(background.getBlue(), 2) + pow(foreground.getBlue(), 2)) / 2);

    return new Color(r, g, b, foreground.getAlpha());
  }
}
