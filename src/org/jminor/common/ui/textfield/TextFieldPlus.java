/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.textfield;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.Insets;

/**
 * A normal text field that allows setting max number of chars and uppercase.
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
    setMargin(new Insets(0, 2, 0, 0));
  }

  /**
   * @return Value for property 'value'.
   */
  public Object getValue() {
    return this.getText();
  }

  /**
   * @param l Value to set for property 'maxLength'.
   */
  public void setMaxLength(int l) {
    maxLength = l;
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
   * @return Value for property 'minDouble'.
   */
  public double getMinDouble() {
    return min;
  }

  /**
   * @return Value for property 'maxDouble'.
   */
  public double getMaxDouble() {
    return max;
  }

  /**
   * @return Value for property 'min'.
   */
  public int getMin() {
    return (int) min;
  }

  /**
   * @return Value for property 'max'.
   */
  public int getMax() {
    return (int) max;
  }

  /** {@inheritDoc} */
  public void setText(final String string) {
    super.setText(string == null ? "" : string);
  }

  /** {@inheritDoc} */
  protected Document createDefaultModel() {
    return new defaultDocument();
  }

  private class defaultDocument extends PlainDocument {
    public void insertString(int offset, String string, AttributeSet a) throws BadLocationException {
      if (getMaxLength() >= 0 && getLength() >= getMaxLength())
        return;
      if (upperCase)
        string = string.toUpperCase();
      super.insertString(offset, string, a);
    }
  }
}