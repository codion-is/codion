/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.jminor.common.model.FormatUtil;

import java.text.NumberFormat;

/**
 * A text field for integers.
 */
public final class IntField extends NumberField {

  /**
   * Constructs a new IntField.
   */
  public IntField() {
    this(FormatUtil.getNonGroupingNumberFormat(true));
  }

  /**
   * Constructs a new IntField.
   * @param columns the number of columns
   */
  public IntField(final int columns) {
    this(FormatUtil.getNonGroupingNumberFormat(true), columns);
  }

  /**
   * Constructs a new IntField.
   * @param format the format to use
   */
  public IntField(final NumberFormat format) {
    this(format, 0);
  }

  /**
   * Constructs a new IntField.
   * @param format the format to use
   * @param columns the number of columns
   */
  public IntField(final NumberFormat format, final int columns) {
    super(new NumberDocument(new NumberDocumentFilter(format)), columns);
  }

  /**
   * @return the value
   */
  public Integer getInt() {
    return ((NumberDocument) getDocument()).getInt();
  }

  /**
   * @param value the value to set
   */
  public void setInt(final Integer value) {
    ((NumberDocument) getDocument()).setNumber(value);
  }
}