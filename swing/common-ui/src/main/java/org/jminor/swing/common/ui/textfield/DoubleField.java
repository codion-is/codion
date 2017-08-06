/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import javax.swing.text.BadLocationException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

/**
 * A text field for doubles.
 */
public final class DoubleField extends NumberField {

  private static final int MAXIMUM_FRACTION_DIGITS = 340;

  /**
   * Instantiates a new DoubleField.
   */
  public DoubleField() {
    this(createDefaultFormat());
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
   * Set the decimal and grouping separators for this field
   * @param decimalSeparator the decimal separator
   * @param groupingSeparator the grouping separator
   */
  public void setSeparators(final char decimalSeparator, final char groupingSeparator) {
    ((DoubleDocument) getDocument()).setSeparators(decimalSeparator, groupingSeparator);
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
      try {
        remove(0, getLength());
      }
      catch (final BadLocationException e) {
        throw new RuntimeException(e);
      }
    }

    private void setSeparators(final char decimalSeparator, final char groupingSeparator) {
      if (decimalSeparator == groupingSeparator) {
        throw new IllegalArgumentException("Decimal separator must not be the same as grouping separator");
      }
      final DecimalFormatSymbols symbols = ((DecimalFormat) getFormat()).getDecimalFormatSymbols();
      symbols.setDecimalSeparator(decimalSeparator);
      symbols.setGroupingSeparator(groupingSeparator);
      ((DecimalFormat) getFormat()).setDecimalFormatSymbols(symbols);
      try {
        remove(0, getLength());
      }
      catch (final BadLocationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static final class DoubleDocumentFilter extends NumberDocumentFilter {

    private DoubleDocumentFilter(final NumberFormat format) {
      super(format);
    }

    @Override
    protected String transformString(final String string) {
      final char decimalSeparator = ((DecimalFormat) getFormat()).getDecimalFormatSymbols().getDecimalSeparator();
      if (string.equals(Character.toString(decimalSeparator))) {
        return "0" + decimalSeparator;
      }

      return removeExcessiveFractionDigits(removeExcessiveDecimalSeparators(
              super.transformString(replaceGroupingSeparators(string, decimalSeparator))));
    }

    @Override
    protected boolean isValidCharacter(final int index, final char character) {
      final char decimalSeparator = ((DecimalFormat) getFormat()).getDecimalFormatSymbols().getDecimalSeparator();

      return character == decimalSeparator || super.isValidCharacter(index, character);
    }

    private String replaceGroupingSeparators(final String string, final char decimalSeparator) {
      if (!getFormat().isGroupingUsed()) {
        final char groupingSeparator = ((DecimalFormat) getFormat()).getDecimalFormatSymbols().getGroupingSeparator();

        return string.replace(groupingSeparator, decimalSeparator);
      }

      return string;
    }

    private String removeExcessiveDecimalSeparators(final String string) {
      final char decimalSeparator = ((DecimalFormat) getFormat()).getDecimalFormatSymbols().getDecimalSeparator();
      final StringBuilder builder = new StringBuilder(string);
      boolean decimalSeparatorFound = false;
      int i = 0;
      while (i < builder.length()) {
        if (builder.charAt(i) == decimalSeparator) {
          if (decimalSeparatorFound) {
            builder.replace(i, i + 1, "");
          }
          else {
            decimalSeparatorFound = true;
          }
        }
        i++;
      }

      return builder.toString();
    }

    private String removeExcessiveFractionDigits(final String string) {
      final char decimalSeparator = ((DecimalFormat) getFormat()).getDecimalFormatSymbols().getDecimalSeparator();
      final int maximumFractionDigits = getFormat().getMaximumFractionDigits();
      if (maximumFractionDigits != -1) {
        final int decimalIndex = string.indexOf(decimalSeparator);
        if (decimalIndex != -1) {
          final int fractionDigits = string.length() - decimalIndex;
          if (fractionDigits > maximumFractionDigits) {
            return string.substring(0, decimalIndex + maximumFractionDigits + 1);
          }
        }
      }

      return string;
    }
  }
}
