/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.common.ui.textfield;

import org.jminor.common.model.Util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class DoubleField extends IntField {

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

  /**
   * @return Value for property 'double'.
   */
  public Double getDouble() {
    return Util.getDouble(getText());
  }

  /**
   * @param value Value to set for property 'double'.
   */
  public void setDouble(final Double value) {
    setText(value == null ? "" : value.toString());
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
      //convert "." or "," to "0." before proceeding
      if (getLength() == 0 && (string.equals(".") || string.equals(",")))
        string = "0.";

      final String text = getText(0, getLength());
      double value = 0;
      if (text != null && !text.equals("") && !text.equals("-"))
        value = Util.getDouble(text);
      boolean valueOk = false;
      char c = string.charAt(0);
      if (offset == 0 && c == '-')
        valueOk = value >= 0;
      else if ((c >= '0') && (c <= '9'))
        valueOk = !((offset == 0) && (value < 0));
      else if ((c == ',' || c == '.') && (offset != 0)) {
        if (text != null && text.indexOf(".") >= 0) //not allow multiple decimal points
          return;
        valueOk = true;
        string = ".";
      }
      // Range check
      if (valueOk) {
        StringBuffer sb = new StringBuffer(text);
        sb.insert(offset, string);
        double lVal = Util.getDouble(sb.toString());
        valueOk = ((lVal <= getMax()) && (lVal >= getMin()));
      }

      if (valueOk) {
        super.insertString(offset, string, a);
        valueUpdated();
      }
    }
  }
}
