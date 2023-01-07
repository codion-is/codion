/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.Util;

import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.text.Document;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * A text field which paints a hint text when it is not the focus owner and contains no text.
 */
public class HintTextField extends JTextField {

  private String hintText;
  private Color hintColor;

  public HintTextField(Document document) {
    this(document, null);
  }

  public HintTextField(Document document, String hintText) {
    this(hintText);
    setDocument(document);
  }

  public HintTextField(String hintText) {
    this.hintText = hintText;
    updateHintTextColor();
    addPropertyChangeListener("UI", new UpdateHintTextColorOnUIChange());
    addFocusListener(new UpdateHintFocusListener());
  }

  public final String getHintText() {
    return hintText;
  }

  public final void setHintText(String hintText) {
    this.hintText = hintText;
    repaint();
  }

  @Override
  public final void paint(Graphics graphics) {
    super.paint(graphics);
    if (!Util.nullOrEmpty(hintText) && !isFocusOwner() && getText().isEmpty()) {
      paintHintText(graphics);
    }
  }

  private void paintHintText(Graphics graphics) {
    ((Graphics2D) graphics).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    Insets insets = getInsets();
    FontMetrics fontMetrics = graphics.getFontMetrics();
    graphics.setColor(hintColor);
    graphics.drawString(hintText, insets.left, getHeight() - fontMetrics.getDescent() - insets.bottom);
  }

  private void updateHintTextColor() {
    hintColor = hintForegroundColor();
    repaint();
  }

  private Color hintForegroundColor() {
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
