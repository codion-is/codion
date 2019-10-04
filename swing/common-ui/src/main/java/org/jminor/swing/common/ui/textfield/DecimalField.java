/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * A text field for decimals, e.i. Double or BigDecimal.
 */
public final class DecimalField extends NumberField {

  private static final int MAXIMUM_FRACTION_DIGITS = 340;

  /**
   * Instantiates a new DecimalField.
   */
  public DecimalField() {
    this(0);
  }

  /**
   * Instantiates a new DecimalField
   * @param columns the number of columns
   */
  public DecimalField(final int columns) {
    this(createDefaultFormat(), columns);
  }

  /**
   * Instantiates a new DecimalField
   * @param format the format to use
   */
  public DecimalField(final DecimalFormat format) {
    this(format, 0);
  }

  /**
   * Instantiates a new DecimalField
   * @param format the format to use
   * @param columns the number of columns
   */
  public DecimalField(final DecimalFormat format, final int columns) {
    super(new DecimalDocument(format), columns);
  }

  /**
   * @return the maximum number of fraction digits this field shows
   */
  public int getMaximumFractionDigits() {
    return ((DecimalDocument) getDocument()).getMaximumFractionDigits();
  }

  /**
   * @param maximumFractionDigits the maximum number of fraction digits this field shows
   */
  public void setMaximumFractionDigits(final int maximumFractionDigits) {
    ((DecimalDocument) getDocument()).setMaximumFractionDigits(maximumFractionDigits);
  }

  /**
   * @return the current value
   */
  public Double getDouble() {
    return ((DecimalDocument) getDocument()).getDouble();
  }

  /**
   * @param value the value to set
   */
  public void setDouble(final Double value) {
    ((DecimalDocument) getDocument()).setNumber(value);
  }

  /**
   * @return the current value
   * @see DecimalFormat#setParseBigDecimal(boolean)
   */
  public BigDecimal getBigDecimal() {
    return ((DecimalDocument) getDocument()).getBigDecimal();
  }

  /**
   * @param value the value to set
   */
  public void setBigDecimal(final BigDecimal value) {
    ((DecimalDocument) getDocument()).setNumber(value);
  }

  private static DecimalFormat createDefaultFormat() {
    final DecimalFormat format = new DecimalFormat();
    format.setMaximumFractionDigits(MAXIMUM_FRACTION_DIGITS);

    return format;
  }

  private static final class DecimalDocument extends NumberDocument {

    private DecimalDocument(final DecimalFormat format) {
      super(new DecimalDocumentFilter(format));
    }

    private int getMaximumFractionDigits() {
      final int maximumFractionDigits = getFormat().getMaximumFractionDigits();

      return maximumFractionDigits == MAXIMUM_FRACTION_DIGITS ? -1 : maximumFractionDigits;
    }

    private void setMaximumFractionDigits(final int maximumFractionDigits) {
      if (maximumFractionDigits < 1 && maximumFractionDigits != -1) {
        throw new IllegalArgumentException("Maximum fraction digits must be larger than 0, or -1 for no maximum");
      }
      getFormat().setMaximumFractionDigits(maximumFractionDigits == -1 ? MAXIMUM_FRACTION_DIGITS : maximumFractionDigits);
      setText("");
    }
  }

  private static final class DecimalDocumentFilter extends NumberDocumentFilter {

    private DecimalDocumentFilter(final DecimalFormat format) {
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
