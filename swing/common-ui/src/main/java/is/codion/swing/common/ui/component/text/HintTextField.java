/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.text.Document;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * A text field which displays a hint text when it is not the focus owner and contains no text.
 * The hint text foreground color is a simplistic average of the text field background and foreground color.
 */
public class HintTextField extends JTextField {

  private String hintText;
  private Color hintColor;

  /**
   * @param document the document
   */
  public HintTextField(Document document) {
    this(document, null);
  }

  /**
   * @param hintText the hint text
   */
  public HintTextField(String hintText) {
    this(null, hintText);
  }

  /**
   * @param document the document
   * @param hintText the hint text
   */
  public HintTextField(Document document, String hintText) {
    super(document, null, 0);
    setHintText(hintText);
    updateHintTextColor();
    addPropertyChangeListener("UI", new UpdateHintTextColorOnUIChange());
    addFocusListener(new UpdateHintFocusListener());
  }

  /**
   * @return the hint text
   */
  public final String getHintText() {
    return hintText;
  }

  /**
   * @param hintText the hint text
   */
  public final void setHintText(String hintText) {
    this.hintText = hintText;
    repaint();
  }

  @Override
  public final void paint(Graphics graphics) {
    super.paint(graphics);
    if (!nullOrEmpty(hintText) && !isFocusOwner() && getText().isEmpty()) {
      paintHintText(graphics);
    }
  }

  @Override
  public final void addPropertyChangeListener(String property, PropertyChangeListener listener) {
    super.addPropertyChangeListener(property, listener);
  }

  @Override
  public final synchronized void addFocusListener(FocusListener listener) {
    super.addFocusListener(listener);
  }

  private void paintHintText(Graphics graphics) {
    ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    Insets insets = getInsets();
    graphics.setColor(hintColor);
    graphics.drawString(hintText, insets.left, getHeight() - graphics.getFontMetrics().getDescent() - insets.bottom);
  }

  private void updateHintTextColor() {
    hintColor = hintForegroundColor();
    repaint();
  }

  private static Color hintForegroundColor() {
    Color foreground = UIManager.getColor("TextField.foreground");
    Color background = UIManager.getColor("TextField.background");

    //simplistic averaging of background and foreground
    int r = (int) sqrt((pow(background.getRed(), 2) + pow(foreground.getRed(), 2)) / 2);
    int g = (int) sqrt((pow(background.getGreen(), 2) + pow(foreground.getGreen(), 2)) / 2);
    int b = (int) sqrt((pow(background.getBlue(), 2) + pow(foreground.getBlue(), 2)) / 2);

    return new Color(r, g, b, foreground.getAlpha());
  }

  private final class UpdateHintTextColorOnUIChange implements PropertyChangeListener {

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      updateHintTextColor();
    }
  }

  private final class UpdateHintFocusListener implements FocusListener {

    @Override
    public void focusGained(FocusEvent e) {
      repaint();
    }

    @Override
    public void focusLost(FocusEvent e) {
      repaint();
    }
  }
}
