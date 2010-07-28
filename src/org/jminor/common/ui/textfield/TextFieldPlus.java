/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.textfield;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.util.Locale;

/**
 * A normal text field that allows setting max number of chars and uppercase.
 * Also includes basic numerical range checking facilities.
 */
public class TextFieldPlus extends JTextField {

  private boolean upperCase = false;
  private int maxLength = 0;

  private double min = Double.NEGATIVE_INFINITY;
  private double max = Double.POSITIVE_INFINITY;

  public TextFieldPlus() {
    this(0);
  }

  /** Constructs a new TextFieldPlus.
   * @param columns the number of columns
   */
  public TextFieldPlus(final int columns) {
    super(columns);
  }

  /**
   * @return the value in this text field
   */
  public Object getValue() {
    return this.getText();
  }

  /**
   * @param maxLength the maximum number of characters this text field should allow
   */
  public final void setMaxLength(final int maxLength) {
    this.maxLength = maxLength;
  }

  /**
   * @return the maximum number of characters this text field allows
   */
  public final int getMaxLength() {
    return maxLength;
  }

  /**
   * @param upperCase true if this text field should automatically convert text to uppercase
   */
  public final void setUpperCase(final boolean upperCase) {
    this.upperCase = upperCase;
  }

  public boolean isUpperCase() {
    return upperCase;
  }

  public final void setRange(final int min, final int max) {
    setRange((double) min, (double) max);
  }

  public final void setRange(final double min, final double max) {
    this.min = min;
    this.max = max;
  }

  /**
   * @return the minimum value this field should accept
   */
  public final double getMinimumValue() {
    return min;
  }

  /**
   * @return the maximum value this field should accept
   */
  public final double getMaximumValue() {
    return max;
  }

  @Override
  public final void setText(final String t) {
    super.setText(t == null ? "" : t);
  }

  protected final boolean isWithinRange(final double value) {
    return ((value <= max) && (value >= min));
  }

  @Override
  protected Document createDefaultModel() {
    return new PlainDocument() {
      @Override
      public void insertString(final int offs, final String str, final AttributeSet a) throws BadLocationException {
        if (getMaxLength() > 0 && getLength() + str.length() > getMaxLength()) {
          return;
        }

        super.insertString(offs, upperCase ? str.toUpperCase(Locale.getDefault()) : str, a);
      }
    };
  }
}