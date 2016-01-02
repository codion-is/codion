/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.jminor.common.model.Util;

import java.text.NumberFormat;

/**
 * A text field for longs.
 */
public final class LongField extends NumberField {

  /**
   * Instantiates a new LongField.
   */
  public LongField() {
    this(Util.getNonGroupingNumberFormat(true));
  }

  /**
   * Instantiates a new LongField
   * @param columns the number of columns
   */
  public LongField(final int columns) {
    this(Util.getNonGroupingNumberFormat(true), columns);
  }

  /**
   * Instantiates a new LongField
   * @param format the format to use
   */
  public LongField(final NumberFormat format) {
    this(format, 0);
  }

  /**
   * Instantiates a new LongField
   * @param format the format to use
   * @param columns the number of columns
   */
  public LongField(final NumberFormat format, final int columns) {
    super(new NumberDocument(new NumberDocumentFilter(format)), columns);
  }

  /**
   * @return the value
   */
  public Long getLong() {
    return ((NumberDocument) getDocument()).getLong();
  }

  /**
   * @param value the value to set
   */
  public void setLong(final Long value) {
    ((NumberDocument) getDocument()).setNumber(value);
  }
}