/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
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
  public void setMaxLength(final int maxLength) {
    this.maxLength = maxLength;
  }

  /**
   * @return the maximum number of characters this text field allows
   */
  public int getMaxLength() {
    return maxLength;
  }

  /**
   * @param upperCase true if this text field should automatically convert text to uppercase
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
  @Override
  public void setText(final String string) {
    super.setText(string == null ? "" : string);
  }

  protected boolean isWithinRange(final double value) {
    return ((value <= max) && (value >= min));
  }

  /** {@inheritDoc} */
  @Override
  protected Document createDefaultModel() {
    return new PlainDocument() {
      @Override
      public void insertString(final int offset, final String string, final AttributeSet a) throws BadLocationException {
        if (getMaxLength() > 0 && getLength() + string.length() > getMaxLength()) {
          return;
        }

        super.insertString(offset, upperCase ? string.toUpperCase() : string, a);
      }
    };
  }
}