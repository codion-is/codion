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
public class DoubleField extends IntField {

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
    return new PlainDocument() {
      @Override
      public void insertString(final int offs, final String str, final AttributeSet a) throws BadLocationException {
        if (getMaxLength() > 0 && getLength() + (str != null ? str.length() : 0) > getMaxLength()) {
          return;
        }
        if (str == null || str.equals("")) {
          super.insertString(offs, str, a);
          return;
        }
        String string = str;
        if (getDecimalSymbol().equals(POINT)) {
          if (string.contains(COMMA)) {
            string = string.replace(COMMA, POINT);
          }
        }
        else if (string.contains(POINT)) {
          string = string.replace(POINT, COMMA);
        }

        //convert "." or "," to "0." before proceeding
        if (getLength() == 0 && (isDecimalSymbol(string))) {
          string = "0" + getDecimalSymbol();
        }

        final String text = getText(0, getLength());
        double value = 0;
        if (text != null && !text.equals("") && !text.equals("-")) {
          value = Util.getDouble(text);
        }
        boolean valueOk = false;
        char c = string.charAt(0);
        if (offs == 0 && c == '-') {
          valueOk = value >= 0;
        }
        else if (Character.isDigit(c)) {
          valueOk = !((offs == 0) && (value < 0));
        }
        else if (isDecimalSymbol(c) && offs != 0) {
          if (text != null && (text.contains(POINT) || text.contains(COMMA))) { //not allow multiple decimal points
            return;
          }
          valueOk = true;
          string = getDecimalSymbol();
        }
        // Range check
        if (valueOk) {
          final StringBuilder sb = new StringBuilder(text);
          sb.insert(offs, string);
          valueOk = isWithinRange(Util.getDouble(sb.toString()));
        }

        if (valueOk) {
          super.insertString(offs, string, a);
        }
      }
    };
  }
}
