/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.textfield;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

/**
 * A normal text field that allows setting max number of chars and uppercase.
 * Also includes basic numerical range checking facilities.
 */
public class TextFieldPlus extends JTextField {

  private boolean upperCase = false;
  private int maxLength = -1;

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
   * @return Value for property 'value'.
   */
  public Object getValue() {
    return this.getText();
  }

  /**
   * @param maxLength Value to set for property 'maxLength'.
   */
  public void setMaxLength(final int maxLength) {
    this.maxLength = maxLength;
  }

  /**
   * @return Value for property 'maxLength'.
   */
  public int getMaxLength() {
    return maxLength;
  }

  /**
   * @param upperCase Value to set for property 'upperCase'.
   */
  public void setUpperCase(final boolean upperCase) {
    this.upperCase = upperCase;
  }

  public void setRange(final int min, final int max) {
    setRange((double) min, (double) max);
  }

  public void setRange(final double min, final double max) {
    this.min = min;
    this.max = max;
  }

  /**
   * @return the minimum value this field should accept
   */
  public double getMinimumValue() {
    return min;
  }

  /**
   * @return the maximum value this field should accept
   */
  public double getMaximumValue() {
    return max;
  }

  /** {@inheritDoc} */
  public void setText(final String string) {
    super.setText(string == null ? "" : string);
  }

  protected boolean isWithinRange(final double value) {
    return ((value <= max) && (value >= min));
  }

  /** {@inheritDoc} */
  protected Document createDefaultModel() {
    return new DefaultDocument();
  }

  private class DefaultDocument extends PlainDocument {
    public void insertString(int offset, String string, AttributeSet a) throws BadLocationException {
      if (getMaxLength() >= 0 && getLength() >= getMaxLength())
        return;
      if (upperCase)
        string = string.toUpperCase();
      super.insertString(offset, string, a);
    }
  }
}