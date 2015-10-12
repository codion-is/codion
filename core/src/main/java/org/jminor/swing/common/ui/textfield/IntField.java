/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.jminor.common.model.Util;

import javax.swing.JTextField;
import javax.swing.text.Document;
import java.text.NumberFormat;

/**
 * A text field for integers.
 */
public class IntField extends JTextField {

  private final transient ThreadLocal<NumberFormat> format = new LocalFormat();

  private double minimumValue = Double.NEGATIVE_INFINITY;
  private double maximumValue = Double.POSITIVE_INFINITY;

  /**
   * Constructs a new IntField.
   */
  public IntField() {
    this(0);
  }

  /**
   * Constructs a new IntField.
   * @param columns the number of columns
   */
  public IntField(final int columns) {
    super(columns);
  }

  /**
   * Constructs a new IntField.
   * @param min the minimum value
   * @param max the maximum value
   */
  public IntField(final int min, final int max) {
    setRange(min, max);
  }

  /**
   * Sets the range of values this field should allow
   * @param min the minimum value
   * @param max the maximum value
   */
  public final void setRange(final double min, final double max) {
    this.minimumValue = min;
    this.maximumValue = max;
  }

  /**
   * @return the minimum value this field should accept
   */
  public final double getMinimumValue() {
    return minimumValue;
  }

  /**
   * @return the maximum value this field should accept
   */
  public final double getMaximumValue() {
    return maximumValue;
  }

  /**
   * @param value the value to check
   * @return true if this value is within the allowed range for this field
   */
  public final boolean isWithinRange(final double value) {
    return ((value <= maximumValue) && (value >= minimumValue));
  }

  /**
   * @return the value
   */
  public final Integer getInt() {
    return Util.getInt(getText());
  }

  /**
   * @param value the value to set
   */
  public final void setInt(final Integer value) {
    setText(value == null ? "" : format.get().format(value));
  }

  /** {@inheritDoc} */
  @Override
  protected Document createDefaultModel() {
    return new IntFieldDocument();
  }

  private final class IntFieldDocument extends SizedDocument {

    @Override
    protected boolean validValue(final String string, final String documentText, final int offset) {
      int value = 0;
      if (documentText != null && !documentText.equals("") && !documentText.equals("-")) {
        value = Integer.parseInt(documentText);
      }
      boolean valueOk = false;
      final char c = string.charAt(0);
      if (offset == 0 && c == '-') {
        valueOk = value >= 0;
      }
      else if (Character.isDigit(c)) {
        valueOk = !((offset == 0) && (value < 0));
      }
      // Range check
      if (valueOk) {
        final StringBuilder sb = new StringBuilder(documentText);
        sb.insert(offset, string);
        valueOk = isWithinRange(Util.getLong(sb.toString()));
      }

      return valueOk;
    }
  }

  private static final class LocalFormat extends ThreadLocal<NumberFormat> {
    @Override
    protected NumberFormat initialValue() {
      return Util.getNonGroupingNumberFormat(true);
    }
  }
}