/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.Formats;
import is.codion.common.event.EventDataListener;
import is.codion.swing.common.model.textfield.DocumentAdapter;

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
  public LongField(final int columns) {
    this(Formats.getNonGroupingIntegerFormat(), columns);
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
    super(new NumberDocument<>(new NumberParsingDocumentFilter<>(new NumberParser<>(format, Long.class),
            new NumberRangeValidator<>())), columns);
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
  public void setLong(final Long value) {
    getTypedDocument().setNumber(value);
  }

  /**
   * @param listener a listener notified when the value changes
   */
  public void addLongListener(final EventDataListener<Long> listener) {
    final NumberDocument<Long> document = getTypedDocument();
    document.addDocumentListener((DocumentAdapter) e -> listener.onEvent(document.getLong()));
  }
}