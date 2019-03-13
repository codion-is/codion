/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import java.text.DecimalFormat;

/**
 * A text field for doubles.
 */
public final class DoubleField extends NumberField {

  private static final int MAXIMUM_FRACTION_DIGITS = 340;

  /**
   * Instantiates a new DoubleField.
   */
  public DoubleField() {
    this(0);
  }

  /**
   * Instantiates a new DoubleField
   * @param columns the number of columns
   */
  public DoubleField(final int columns) {
    this(createDefaultFormat(), columns);
  }

  /**
   * Instantiates a new DoubleField
   * @param format the format to use
   */
  public DoubleField(final DecimalFormat format) {
    this(format, 0);
  }

  /**
   * Instantiates a new DoubleField
   * @param format the format to use
   * @param columns the number of columns
   */
  public DoubleField(final DecimalFormat format, final int columns) {
    super(new DoubleDocument(format), columns);
  }

  /**
   * @return the maximum number of fraction digits this field shows
   */
  public int getMaximumFractionDigits() {
    return ((DoubleDocument) getDocument()).getMaximumFractionDigits();
  }

  /**
   * @param maximumFractionDigits the maximum number of fraction digits this field shows
   */
  public void setMaximumFractionDigits(final int maximumFractionDigits) {
    ((DoubleDocument) getDocument()).setMaximumFractionDigits(maximumFractionDigits);
  }

  /**
   * @return the current value
   */
  public Double getDouble() {
    return ((DoubleDocument) getDocument()).getDouble();
  }

  /**
   * @param value the value to set
   */
  public void setDouble(final Double value) {
    ((DoubleDocument) getDocument()).setNumber(value);
  }

  private static DecimalFormat createDefaultFormat() {
    final DecimalFormat format = new DecimalFormat();
    format.setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);

    return format;
  }

  private static final class DoubleDocument extends NumberDocument {

    private DoubleDocument(final DecimalFormat format) {
      super(new DoubleDocumentFilter(format));
    }

    private int getMaximumFractionDigits() {
      if (getFormat().getMaximumFractionDigits() == MAXIMUM_FRACTION_DIGITS) {
        return -1;
      }

      return getFormat().getMaximumFractionDigits();
    }

    private void setMaximumFractionDigits(final int maximumFractionDigits) {
      if (maximumFractionDigits < 1 && maximumFractionDigits != -1) {
        throw new IllegalArgumentException("Maximum fraction digits must be larger than 0, or -1 for no maximum");
      }
      if (maximumFractionDigits == -1) {
        getFormat().setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);
      }
      else {
        getFormat().setMaximumFractionDigits(maximumFractionDigits);
      }
      setText("");
    }
  }

  private static final class DoubleDocumentFilter extends NumberDocumentFilter {

    private DoubleDocumentFilter(final DecimalFormat format) {
      super(format);
    }

    @Override
    protected FormatResult format(final String string) {
      final char decimalSeparator = ((DecimalFormat) getFormat()).getDecimalFormatSymbols().getDecimalSeparator();
      if (string.equals(Character.toString(decimalSeparator))) {
        return new FormatResult(1, "0" + decimalSeparator);
      }

      return super.format(string);
    }
  }
}
