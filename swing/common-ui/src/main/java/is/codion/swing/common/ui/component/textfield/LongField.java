/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import is.codion.common.formats.Formats;

import java.text.NumberFormat;

/**
 * A text field for longs.
 */
public final class LongField extends NumberField<Long> {

  /**
   * Instantiates a new LongField.
   */
  public LongField() {
    this(Formats.getNonGroupingIntegerFormat());
  }

  /**
   * Instantiates a new LongField
   * @param columns the number of columns
   */
  public LongField(int columns) {
    this(Formats.getNonGroupingIntegerFormat(), columns);
  }

  /**
   * Instantiates a new LongField
   * @param format the format to use
   */
  public LongField(NumberFormat format) {
    this(format, 0);
  }

  /**
   * Instantiates a new LongField
   * @param format the format to use
   * @param columns the number of columns
   */
  public LongField(NumberFormat format, int columns) {
    super(new NumberDocument<>(new NumberParsingDocumentFilter<>(new NumberParser<>(format, Long.class))), columns);
  }

  /**
   * @return the value
   */
  public Long getLong() {
    return getTypedDocument().getLong();
  }

  /**
   * @param value the value to set
   */
  public void setLong(Long value) {
    getTypedDocument().setNumber(value);
  }
}