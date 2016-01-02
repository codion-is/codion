/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import java.text.DecimalFormat;
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
    document.getFormat().setGroupingUsed(false);
  }

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
   * A Document implementation for numerical values
   */
  protected static class NumberDocument extends SizedDocument {

    protected NumberDocument(final NumberDocumentFilter documentFilter) {
      setDocumentFilter(documentFilter);
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
      final String text = getText();
      if (text.isEmpty()) {
        return null;
      }

      try {
        return getFormat().parse(text);
      }
      catch (final ParseException e) {
        throw new RuntimeException(e);
      }
    }

    protected final Integer getInt() {
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
  }

  /**
   * A DocumentFilter for numerical values
   */
  protected static class NumberDocumentFilter extends SizedDocument.SizedDocumentFilter {

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
    protected String prepareString(final String string) {
      final StringBuilder builder = new StringBuilder();
      int index = 0;
      for (final char c : string.toCharArray()) {
        if (isValidCharacter(index++, c)) {
          builder.append(c);
        }
      }

      return builder.toString();
    }

    protected boolean isValidCharacter(final int index, final char character) {
      final char groupingSeparator = ((DecimalFormat) getFormat()).getDecimalFormatSymbols().getGroupingSeparator();

      return index == 0 && character == '-' || (character == groupingSeparator && getFormat().isGroupingUsed()) || Character.isDigit(character);
    }

    /**
     * @param value the value to check
     * @return true if this value falls within the allowed range for this document
     */
    protected final boolean isWithinRange(final double value) {
      return value >= minimumValue && value <= maximumValue;
    }

    @Override
    protected final boolean validValue(final String value) {
      if (value.isEmpty()) {
        return true;
      }
      try {
        final Number number = format.parse(value);

        return isWithinRange(number.doubleValue());
      }
      catch (final ParseException e) {
        return false;
      }
    }
  }
}
