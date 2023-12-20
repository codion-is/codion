/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;

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
 * A text field which displays a hint text when it is not the focus owner and contains no text.
 * The hint text foreground color is a simplistic average of the text field background and foreground color.
 */
public class HintTextField extends JTextField {

  private final Value<String> hintText;

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
    this.hintText = Value.value(hintText, "");
    this.hintText.addListener(this::repaint);
    updateHintTextColor();
    addPropertyChangeListener("UI", new UpdateHintTextColorOnUIChange());
    addFocusListener(new UpdateHintFocusListener());
  }

  /**
   * @return the Value controlling the hint text
   */
  public final Value<String> hintText() {
    return hintText;
  }

  @Override
  public final void paint(Graphics graphics) {
    super.paint(graphics);
    if (!hintText.get().isEmpty() && !isFocusOwner() && getText().isEmpty()) {
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
    FontMetrics fontMetrics = graphics.getFontMetrics();
    graphics.setColor(hintColor);
    graphics.drawString(adjustHintTextLength(fontMetrics, insets.left + insets.right),
            insets.left, getHeight() - fontMetrics.getDescent() - insets.bottom);
  }

  private String adjustHintTextLength(FontMetrics fontMetrics, int insets) {
    String adjustedText = hintText.get();
    int hintWidth = fontMetrics.stringWidth(adjustedText) + insets;
    while (hintWidth > getWidth() && !adjustedText.isEmpty()) {
      adjustedText = adjustedText.substring(0, adjustedText.length() - 1);
      hintWidth = fontMetrics.stringWidth(adjustedText) + insets;
    }

    return adjustedText;
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
