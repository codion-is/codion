/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.swing.ui.textfield;

import org.jminor.common.model.Util;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.text.NumberFormat;

/**
 * A text field for doubles.
 */
public class DoubleField extends IntField {

  public static final String POINT = ".";
  public static final String COMMA = ",";

  private final transient ThreadLocal<NumberFormat> format = new LocalFormat();

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
    super(columns);
  }

  /**
   * Instantiates a new DoubleField
   * @param min the minimum value the field can contain
   * @param max the maximum value the field can contain
   */
  public DoubleField(final double min, final double max) {
    setRange(min, max);
  }

  /**
   * @return the decimal symbol being used by this field
   */
  public String getDecimalSymbol() {
    return ((DoubleFieldDocument) getDocument()).getDecimalSymbol();
  }

  /**
   * @param decimalSymbol the decimal symbol to be used by this field
   */
  public void setDecimalSymbol(final String decimalSymbol) {
    ((DoubleFieldDocument) getDocument()).setDecimalSymbol(decimalSymbol);
  }

  /**
   * @return the maximum number of fraction digits this field shows
   */
  public int getMaximumFractionDigits() {
    return ((DoubleFieldDocument) getDocument()).getMaximumFractionDigits();
  }

  /**
   * @param maximumFractionDigits the maximum number of fraction digits this field shows
   */
  public void setMaximumFractionDigits(final int maximumFractionDigits) {
    ((DoubleFieldDocument) getDocument()).setMaximumFractionDigits(maximumFractionDigits);
  }

  /**
   * @return the current value
   */
  public Double getDouble() {
    return Util.getDouble(getText());
  }

  /**
   * @param value the value to set
   */
  public void setDouble(final Double value) {
    setText(value == null ? "" : format.get().format(value));
  }

  /**
   * @param string the string
   * @return true if the given string is a point or a comma
   */
  private static boolean isDecimalSymbol(final String string) {
    return POINT.equals(string) || COMMA.equals(string);
  }

  @Override
  protected Document createDefaultModel() {
    return new DoubleFieldDocument();
  }

  private class DoubleFieldDocument extends SizedDocument {

    private static final int DECIMAL_SPLIT_COUNT = 2;

    private String decimalSymbol = COMMA;
    private int maximumFractionDigits = -1;

    public int getMaximumFractionDigits() {
      return maximumFractionDigits;
    }

    public void setMaximumFractionDigits(final int maximumFractionDigits) {
      if (maximumFractionDigits < 1 && maximumFractionDigits != -1) {
        throw new IllegalArgumentException("Maximum fraction digits must be larger than 0, or -1 for no maximum");
      }
      this.maximumFractionDigits = maximumFractionDigits;
    }

    public String getDecimalSymbol() {
      return decimalSymbol;
    }

    public void setDecimalSymbol(final String decimalSymbol) {
      Util.rejectNullValue(decimalSymbol, "decimalSymbol");
      if (decimalSymbol.length() > 1) {
        throw new IllegalArgumentException("Decimal symbols can only be one character long");
      }

      this.decimalSymbol = decimalSymbol;
    }

    @Override
    protected String prepareString(final String string, final String documentText, final int offset) {
      String preparedString = string;
      if (decimalSymbol.equals(POINT)) {
        if (string.contains(COMMA)) {
          preparedString = string.replace(COMMA, POINT);
        }
      }
      else if (string.contains(POINT)) {
        preparedString = string.replace(POINT, COMMA);
      }

      if (isDecimalSymbol(string)) {
        //convert "." or "," to "0." before proceeding
        if (documentText.length() == 0) {
          preparedString = "0" + preparedString;
        }
        else {
          if (offset != 0 && (documentText.contains(POINT) || documentText.contains(COMMA))) {
            return "";//not allow multiple decimal points
          }
          else {
            preparedString = decimalSymbol;
          }
        }
      }

      return preparedString;
    }

    @Override
    protected boolean validValue(final String string, final String documentText, final int offset) {
      final String preparedString = prepareString(string, documentText, offset);
      if (preparedString.length() == 0) {
        return true;
      }
      double value = 0;
      if (documentText != null && !documentText.equals("") && !documentText.equals("-")) {
        value = Util.getDouble(documentText);
      }
      boolean valueOk = false;
      final char c = preparedString.charAt(0);
      if (offset == 0 && c == '-') {
        valueOk = value >= 0;
      }
      else if (Character.isDigit(c)) {
        valueOk = !((offset == 0) && (value < 0));
      }
      else if (isDecimalSymbol(preparedString)) {
        valueOk = true;
      }
      // Range check
      if (valueOk) {
        final StringBuilder sb = new StringBuilder(documentText);
        sb.insert(offset, preparedString);
        valueOk = isWithinRange(Util.getDouble(sb.toString()));
      }

      return valueOk;
    }

    @Override
    protected void postInsert() {//not exactly elegant :(
      try {
        final String text = getText(0, getLength());
        //silently remove fraction digits exceeding the maximum
        if (maximumFractionDigits != -1) {
          final String fixedText = removeFractionDigits(text);
          if (!text.equals(fixedText)) {
            setText(fixedText);
          }
        }
      }
      catch (final BadLocationException e) {
        throw new RuntimeException(e);
      }
    }

    private String removeFractionDigits(final String preparedString) {
      final String[] splitResult = preparedString.split(getDecimalSymbol());
      if (splitResult.length < DECIMAL_SPLIT_COUNT) {//no decimal symbol
        return preparedString;
      }

      final int fractionDigits = splitResult[1].length();
      if (fractionDigits > maximumFractionDigits) {
        final int digitsToRemove = fractionDigits - maximumFractionDigits;
        return preparedString.substring(0, preparedString.length() - digitsToRemove);
      }

      return preparedString;
    }
  }

  private static final class LocalFormat extends ThreadLocal<NumberFormat> {
    @Override
    protected NumberFormat initialValue() {
      return Util.getNonGroupingNumberFormat();
    }
  }
}
