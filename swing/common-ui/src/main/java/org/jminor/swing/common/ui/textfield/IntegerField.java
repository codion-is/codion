/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.textfield;

import org.jminor.common.FormatUtil;

import java.text.NumberFormat;

/**
 * A text field for integers.
 */
public final class IntegerField extends NumberField {

  /**
   * Constructs a new IntegerField.
   */
  public IntegerField() {
    this(FormatUtil.getNonGroupingNumberFormat(true));
  }

  /**
   * Constructs a new IntegerField.
   * @param columns the number of columns
   */
  public IntegerField(final int columns) {
    this(FormatUtil.getNonGroupingNumberFormat(true), columns);
  }

  /**
   * Constructs a new IntegerField.
   * @param format the format to use
   */
  public IntegerField(final NumberFormat format) {
    this(format, 0);
  }

  /**
   * Constructs a new IntegerField.
   * @param format the format to use
   * @param columns the number of columns
   */
  public IntegerField(final NumberFormat format, final int columns) {
    super(new NumberDocument(new NumberDocumentFilter(format)), columns);
  }

  /**
   * @return the value
   */
  public Integer getInteger() {
    return ((NumberDocument) getDocument()).getInteger();
  }

  /**
   * @param value the value to set
   */
  public void setInteger(final Integer value) {
    ((NumberDocument) getDocument()).setNumber(value);
  }
}