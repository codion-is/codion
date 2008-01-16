/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.textfield;

import org.jminor.common.Constants;
import org.jminor.common.model.Util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class IntField extends TextFieldPlus {

  /** Constructs a new IntField. */
  public IntField() {
    this(0);
  }

  public IntField(final int columns) {
    this(false, columns);
  }

  public IntField(final boolean transferFocusOnEnter, final int columns) {
    super(columns, transferFocusOnEnter);
  }

  public IntField(final int min, final int max) {
    this();
    setRange(min, max);
  }

  /** {@inheritDoc} */
  public Object getValue() {
    return this.getInteger();
  }

  /** {@inheritDoc} */
  protected Document createDefaultModel() {
    return new IntFieldDocument();
  }

  class IntFieldDocument extends PlainDocument {
    /** {@inheritDoc} */
    public void insertString(int offset, String string, AttributeSet a) throws BadLocationException {
      if (getMaxLength() >= 0 && getLength() >= getMaxLength()
          || string.equals(Constants.INTEGER_NULL_VALUE.toString()))
        return;
      if (string.equals("")) {
        super.insertString(offset, string, a);
        return;
      }
      final String text = getText(0, getLength());
      int value = 0;
      if (text != null && !text.equals("") && !text.equals("-"))
        value = Integer.parseInt(text);
      boolean valueOk = false;
      char c = string.charAt(0);
      if (offset == 0 && c == '-')
        valueOk = value >= 0;
      else if ((c >= '0') && (c <= '9'))
        valueOk = !((offset == 0) && (value < 0));
      // Range check
      if (valueOk) {
        StringBuffer sb = new StringBuffer(text);
        sb.insert(offset, string);
        long lVal = Util.getLong(sb.toString());
        valueOk = ((lVal <= getMax()) && (lVal >= getMin()));
      }

      if (valueOk) {
        super.insertString(offset, string, a);
        valueUpdated();
      }
    }
  }

  public int getInt(final String text) {
    return Util.getInt(text);
  }

  /**
   * @return Value for property 'int'.
   */
  public int getInt() {
    return getInt(getText());
  }

  /**
   * @param value Value to set for property 'int'.
   */
  public void setInt(final int value) {
    setText(String.valueOf(value));
  }

  /**
   * @return Value for property 'integer'.
   */
  public Integer getInteger() {
    return getInt();
  }

  /**
   * @param value Value to set for property 'integer'.
   */
  public void setInteger(final Integer value) {
    setInt(value);
  }
}