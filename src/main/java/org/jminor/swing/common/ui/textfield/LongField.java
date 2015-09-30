/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.jminor.common.model.Util;

import javax.swing.text.Document;
import java.text.NumberFormat;

/**
 * A text field for longs.
 */
public class LongField extends DoubleField {

  private final transient ThreadLocal<NumberFormat> format = new LocalFormat();

    /**
   * Instantiates a new LongField.
   */
  public LongField() {
    this(0);
  }

  /**
   * Instantiates a new LongField
   * @param columns the number of columns
   */
  public LongField(final int columns) {
    super(columns);
  }

  /**
   * Instantiates a new LongField
   * @param min the minimum value the field can contain
   * @param max the maximum value the field can contain
   */
  public LongField(final long min, final long max) {
    setRange(min, max);
  }

  /**
   * @return the current value
   */
  public Long getLong() {
    return Util.getLong(getText());
  }

  /**
   * @param value the value to set
   */
  public void setLong(final Long value) {
    setText(value == null ? "" : format.get().format(value));
  }

  @Override
  protected Document createDefaultModel() {
    return new LongFieldDocument();
  }

  private class LongFieldDocument extends SizedDocument {

    @Override
    protected boolean validValue(final String string, final String documentText, final int offset) {
      long value = 0;
      if (documentText != null && !documentText.equals("") && !documentText.equals("-")) {
        value = Long.parseLong(documentText);
      }
      boolean valueOk = false;
      final char c = string.charAt(0);
      if (offset == 0 && c == '-') {
        valueOk = value >= 0;
      }
      else if (Character.isDigit(c)) {
        valueOk = !((offset == 0) && (value < 0));
      }
      // Range check
      if (valueOk) {
        final StringBuilder sb = new StringBuilder(documentText);
        sb.insert(offset, string);
        valueOk = isWithinRange(Util.getLong(sb.toString()));
      }

      return valueOk;
    }
  }

  private static final class LocalFormat extends ThreadLocal<NumberFormat> {
    @Override
    protected NumberFormat initialValue() {
      return Util.getNonGroupingNumberFormat();
    }
  }
}