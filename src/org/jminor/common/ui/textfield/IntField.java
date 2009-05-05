/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.textfield;

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
    super(columns);
  }

  public IntField(final int min, final int max) {
    this();
    setRange(min, max);
  }

  /** {@inheritDoc} */
  @Override
  public Object getValue() {
    return this.getInt();
  }

  /**
   * @return the value
   */
  public Integer getInt() {
    return Util.getInt(getText());
  }

  /**
   * @param value the value to set
   */
  public void setInt(final Integer value) {
    setText(value == null ? "" : value.toString());
  }

  /** {@inheritDoc} */
  @Override
  protected Document createDefaultModel() {
    return new IntFieldDocument();
  }

  class IntFieldDocument extends PlainDocument {
    /** {@inheritDoc} */
    @Override
    public void insertString(int offset, String string, AttributeSet a) throws BadLocationException {
      if (getMaxLength() >= 0 && getLength() >= getMaxLength())
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
      else if (Character.isDigit(c))
        valueOk = !((offset == 0) && (value < 0));
      // Range check
      if (valueOk) {
        StringBuffer sb = new StringBuffer(text);
        sb.insert(offset, string);
        valueOk = isWithinRange(Util.getLong(sb.toString()));
      }

      if (valueOk)
        super.insertString(offset, string, a);
    }
  }
}