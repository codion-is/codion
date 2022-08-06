/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.model.component.text.DocumentAdapter;

import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

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
    if (Arrays.stream(textField.getFocusListeners()).anyMatch(HintFocusListener.class::isInstance)) {
      throw new IllegalStateException("Hint text has already been enabled for text field: " + textField);
    }
    if (nullOrEmpty(hintText)) {
      throw new IllegalArgumentException("Hint text may not be null or empty");
    }
    this.textField = textField;
    this.hintText = hintText;
    this.textField.addFocusListener(new HintFocusListener());
    this.textField.addAncestorListener(new HintAncestorListener());
    this.textField.getDocument().addDocumentListener(new UpdateColorsListener());
    configureColors();
    textField.addPropertyChangeListener("UI", new ConfigureColorsListener());
    updateHint();
  }

  @Override
  public String hintText() {
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
      textField.setCaretPosition(0);
    }
    updateColor();
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

  private final class HintFocusListener implements FocusListener {
    @Override
    public void focusGained(FocusEvent e) {
      updateHint();
    }

    @Override
    public void focusLost(FocusEvent e) {
      updateHint();
    }
  }

  private final class HintAncestorListener implements AncestorListener {
    @Override
    public void ancestorAdded(AncestorEvent event) {
      updateHint();
    }

    @Override
    public void ancestorRemoved(AncestorEvent event) {}

    @Override
    public void ancestorMoved(AncestorEvent event) {}
  }

  private final class UpdateColorsListener implements DocumentAdapter {
    @Override
    public void contentsChanged(DocumentEvent event) {
      updateColor();
    }
  }

  private final class ConfigureColorsListener implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent event) {
      configureColors();
    }
  }
}
