/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.textfield;

import org.jminor.common.model.Util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

/**
 * A text field for doubles.
 */
public final class DoubleField extends IntField {

  public static final String POINT = ".";
  public static final String COMMA = ",";

  private String decimalSymbol = COMMA;

  /** Constructs a new DoubleField. */
  public DoubleField() {
    this(0);
  }

  public DoubleField(final int columns) {
    super(columns);
  }

  public DoubleField(final double min, final double max) {
    this(0);
    setRange(min, max);
  }

  @Override
  public Object getValue() {
    return getDouble();
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

  /**
   * @return the value
   */
  public Double getDouble() {
    return Util.getDouble(getText());
  }

  /**
   * @param value the value to set
   */
  public void setDouble(final Double value) {
    setText(value == null ? "" : value.toString());
  }

  public static boolean isDecimalSymbol(final Character character) {
    return POINT.charAt(0) == character || COMMA.charAt(0) == character;
  }

  public static boolean isDecimalSymbol(final String string) {
    return POINT.equals(string) || COMMA.equals(string);
  }

  @Override
  protected Document createDefaultModel() {
    return new DoubleFieldDocument();
  }

  private String prepareString(final String str, final String documentText, final int offset) {
    String preparedString = str;
    if (getDecimalSymbol().equals(POINT)) {
      if (str.contains(COMMA)) {
        preparedString = str.replace(COMMA, POINT);
      }
    }
    else if (str.contains(POINT)) {
      preparedString = str.replace(POINT, COMMA);
    }

    //convert "." or "," to "0." before proceeding
    if (documentText.length() == 0 && (isDecimalSymbol(preparedString))) {
      preparedString = "0" + preparedString;
    }

    if (isDecimalSymbol(str)) {
      if (documentText.length() == 0) {
        preparedString = "0" + preparedString;
      }
      else {
        if (offset != 0 && (documentText.contains(POINT) || documentText.contains(COMMA))) {
          return "";//not allow multiple decimal points
        }
        else {
          preparedString = getDecimalSymbol();
        }
      }
    }

    return preparedString;
  }

  private class DoubleFieldDocument extends PlainDocument {
    @Override
    public void insertString(final int offs, final String str, final AttributeSet a) throws BadLocationException {
      if (getMaxLength() > 0 && getLength() + (str != null ? str.length() : 0) > getMaxLength()) {
        return;
      }
      if (str == null || str.equals("")) {
        super.insertString(offs, str, a);
        return;
      }

      final String text = getText(0, getLength());
      final String preparedString = prepareString(str, text, offs);
      if (preparedString.isEmpty()) {
        return;
      }
      double value = 0;
      if (text != null && !text.equals("") && !text.equals("-")) {
        value = Util.getDouble(text);
      }
      boolean valueOk = false;
      final char c = preparedString.charAt(0);
      if (offs == 0 && c == '-') {
        valueOk = value >= 0;
      }
      else if (Character.isDigit(c)) {
        valueOk = !((offs == 0) && (value < 0));
      }
      else if (isDecimalSymbol(preparedString)) {
        valueOk = true;
      }
      // Range check
      if (valueOk) {
        final StringBuilder sb = new StringBuilder(text);
        sb.insert(offs, preparedString);
        valueOk = isWithinRange(Util.getDouble(sb.toString()));
      }

      if (valueOk) {
        super.insertString(offs, preparedString, a);
      }
    }
  }
}
