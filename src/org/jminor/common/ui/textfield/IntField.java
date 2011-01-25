/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.textfield;

import org.jminor.common.model.Util;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.text.NumberFormat;

/**
 * A text field for ints.
 */
public class IntField extends TextFieldPlus {

  private transient final ThreadLocal<NumberFormat> format = new ThreadLocal<NumberFormat>() {
    @Override
    protected NumberFormat initialValue() {
      final NumberFormat ret = NumberFormat.getIntegerInstance();
      ret.setGroupingUsed(false);
      return ret;
    }
  };

  /**
   * Constructs a new IntField.
   */
  public IntField() {
    this(0);
  }

  /**
   * Constructs a new IntField.
   * @param columns the number of columns
   */
  public IntField(final int columns) {
    super(columns);
  }

  /**
   * Constructs a new IntField.
   * @param min the minimum value
   * @param max the maximum value
   */
  public IntField(final int min, final int max) {
    this();
    setRange(min, max);
  }

  /** {@inheritDoc} */
  @Override
  public Object getValue() {
    return getInt();
  }

  /**
   * @return the value
   */
  public final Integer getInt() {
    return Util.getInt(getText());
  }

  /**
   * @param value the value to set
   */
  public final void setInt(final Integer value) {
    setText(value == null ? "" : format.get().format(value));
  }

  /** {@inheritDoc} */
  @Override
  protected Document createDefaultModel() {
    return new IntFieldDocument();
  }

  private final class IntFieldDocument extends PlainDocument {
    /** {@inheritDoc} */
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
      int value = 0;
      if (text != null && !text.equals("") && !text.equals("-")) {
        value = Integer.parseInt(text);
      }
      boolean valueOk = false;
      final char c = str.charAt(0);
      if (offs == 0 && c == '-') {
        valueOk = value >= 0;
      }
      else if (Character.isDigit(c)) {
        valueOk = !((offs == 0) && (value < 0));
      }
      // Range check
      if (valueOk) {
        final StringBuilder sb = new StringBuilder(text);
        sb.insert(offs, str);
        valueOk = isWithinRange(Util.getLong(sb.toString()));
      }

      if (valueOk) {
        super.insertString(offs, str, a);
      }
    }
  }
}