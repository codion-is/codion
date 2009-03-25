/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.textfield;

import org.jminor.common.model.Util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

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

  /** {@inheritDoc} */
  public Object getValue() {
    return getDouble();
  }

  public String getDecimalSymbol() {
    return decimalSymbol;
  }

  public void setDecimalSymbol(final String decimalSymbol) {
    if (decimalSymbol == null || decimalSymbol.length() > 1)
      throw new IllegalArgumentException("Decimal symbols can only be one character long");

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

  /** {@inheritDoc} */
  protected Document createDefaultModel() {
    return new DoubleFieldDocument();
  }

  class DoubleFieldDocument extends PlainDocument {
    public void insertString(int offset, String string, AttributeSet a) throws BadLocationException {
      if (getMaxLength() >= 0 && getLength() >= getMaxLength())
        return;
      if (string.equals("")) {
        super.insertString(offset, string, a);
        return;
      }
      if (getDecimalSymbol().equals(POINT)) {
        if (string.contains(COMMA))
          string = string.replace(COMMA, POINT);
      }
      else if (string.contains(POINT))
        string = string.replace(POINT, COMMA);

      //convert "." or "," to "0." before proceeding
      if (getLength() == 0 && (isDecimalSymbol(string)))
        string = "0" + getDecimalSymbol();

      final String text = getText(0, getLength());
      double value = 0;
      if (text != null && !text.equals("") && !text.equals("-"))
        value = Util.getDouble(text);
      boolean valueOk = false;
      char c = string.charAt(0);
      if (offset == 0 && c == '-')
        valueOk = value >= 0;
      else if (Character.isDigit(c))
        valueOk = !((offset == 0) && (value < 0));
      else if (isDecimalSymbol(c) && offset != 0) {
        if (text != null && (text.contains(POINT) || text.contains(COMMA))) //not allow multiple decimal points
          return;
        valueOk = true;
        string = getDecimalSymbol();
      }
      // Range check
      if (valueOk) {
        StringBuffer sb = new StringBuffer(text);
        sb.insert(offset, string);
        valueOk = isWithinRange(Util.getDouble(sb.toString()));
      }

      if (valueOk)
        super.insertString(offset, string, a);
    }
  }
}
