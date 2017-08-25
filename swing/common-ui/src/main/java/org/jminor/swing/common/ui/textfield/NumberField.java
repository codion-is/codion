/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;

/**
 * A text field for numbers.
 */
public class NumberField extends JTextField {

  /**
   * Instantiates a new NumberField
   * @param document the document to use
   * @param columns the number of columns
   */
  public NumberField(final NumberDocument document, final int columns) {
    super(document, null, columns);
    //todo remove this when grouping functionality is "bullet proof"
    document.getFormat().setGroupingUsed(false);
  }

  /**
   * Set whether or not grouping will be used in this field.
   * @param groupingUsed true if grouping should be used false otherwise
   */
  public void setGroupingUsed(final boolean groupingUsed) {
    ((NumberDocument) getDocument()).getFormat().setGroupingUsed(groupingUsed);
  }

  /**
   * @param number the number to display in this field
   */
  public final void setNumber(final Number number) {
    ((NumberDocument) getDocument()).setNumber(number);
  }

  /**
   * @return the number being displayed in this field
   */
  public final Number getNumber() {
    return ((NumberDocument) getDocument()).getNumber();
  }

  /**
   * Sets the range of values this field should allow
   * @param min the minimum value
   * @param max the maximum value
   */
  public final void setRange(final double min, final double max) {
    ((NumberDocumentFilter) ((NumberDocument) getDocument()).getDocumentFilter()).setRange(min, max);
  }

  /**
   * @return the minimum value this field should accept
   */
  public final double getMinimumValue() {
    return ((NumberDocumentFilter) ((NumberDocument) getDocument()).getDocumentFilter()).getMinimumValue();
  }

  /**
   * @return the maximum value this field should accept
   */
  public final double getMaximumValue() {
    return ((NumberDocumentFilter) ((NumberDocument) getDocument()).getDocumentFilter()).getMaximumValue();
  }

  /**
   * Set the decimal and grouping separators for this field
   * @param decimalSeparator the decimal separator
   * @param groupingSeparator the grouping separator
   * @throws IllegalArgumentException in case both separators are the same character
   */
  public void setSeparators(final char decimalSeparator, final char groupingSeparator) {
    ((NumberDocument) getDocument()).setSeparators(decimalSeparator, groupingSeparator);
  }

  /**
   * A Document implementation for numerical values
   */
  protected static class NumberDocument extends SizedDocument {

    protected NumberDocument(final NumberDocumentFilter documentFilter) {
      setDocumentFilterInternal(documentFilter);
    }

    protected final NumberFormat getFormat() {
      return ((NumberDocumentFilter) getDocumentFilter()).getFormat();
    }

    protected final void setNumber(final Number number) {
      if (number == null) {
        setText("");
      }
      else {
        setText(getFormat().format(number));
      }
    }

    protected final Number getNumber() {
      return NumberDocumentFilter.getNumber(getFormat(), getText());
    }

    protected final Integer getInteger() {
      final Number number = getNumber();

      return number == null ? null : number.intValue();
    }

    protected final Long getLong() {
      final Number number = getNumber();

      return number == null ? null : number.longValue();
    }

    protected final Double getDouble() {
      final Number number = getNumber();

      return number == null ? null : number.doubleValue();
    }

    protected final String getText() {
      try {
        return getText(0, getLength());
      }
      catch (final BadLocationException e) {
        throw new RuntimeException(e);
      }
    }

    protected final void setText(final String text) {
      try {
        remove(0, getLength());
        insertString(0, text, null);
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

  /**
   * A DocumentFilter for restricting input to numerical values
   */
  protected static class NumberDocumentFilter extends SizedDocument.SizedDocumentFilter {

    private static final String MINUS_SIGN = "-";

    private final NumberFormat format;

    private double minimumValue = Double.NEGATIVE_INFINITY;
    private double maximumValue = Double.POSITIVE_INFINITY;

    protected NumberDocumentFilter(final NumberFormat format) {
      this.format = format;
    }

    protected final NumberFormat getFormat() {
      return format;
    }

    /**
     * Sets the range of values this field should allow
     * @param min the minimum value
     * @param max the maximum value
     */
    protected final void setRange(final double min, final double max) {
      this.minimumValue = min;
      this.maximumValue = max;
    }

    /**
     * @return the minimum value this field should accept
     */
    protected final double getMinimumValue() {
      return minimumValue;
    }

    /**
     * @return the maximum value this field should accept
     */
    protected final double getMaximumValue() {
      return maximumValue;
    }

    @Override
    protected String transformString(final String string) {
      if (string.isEmpty() || MINUS_SIGN.equals(string)) {
        return string;
      }

      final StringBuilder builder = new StringBuilder();
      int index = 0;
      for (final char c : string.toCharArray()) {
        if (isValidCharacter(index++, c)) {
          builder.append(c);
        }
      }
      final Number number = getNumber(getFormat(), builder.toString());
      if (number != null && isValid(number)) {
        return builder.replace(0, builder.length(), getFormat().format(number)).toString();
      }

      return null;
    }

    protected boolean isValidCharacter(final int index, final char character) {
      return index == 0 && character == MINUS_SIGN.charAt(0) || Character.isDigit(character);
    }

    /**
     * @param value the value to check
     * @return true if this value falls within the allowed range for this document
     */
    protected final boolean isWithinRange(final double value) {
      return value >= minimumValue && value <= maximumValue;
    }

    protected final boolean isValid(final Number number) {
      return isWithinRange(number.doubleValue());
    }

    private static Number getNumber(final NumberFormat format, final String text) {
      if (text.isEmpty()) {
        return null;
      }

      try {
        return format.parse(handleMinusSign(text));
      }
      catch (final ParseException e) {
        return null;
      }
    }

    /**
     * We need to interpret - as -1
     */
    private static String handleMinusSign(final String text) {
      if (MINUS_SIGN.equals(text)) {
        return text + "1";
      }

      return text;
    }
  }
}
