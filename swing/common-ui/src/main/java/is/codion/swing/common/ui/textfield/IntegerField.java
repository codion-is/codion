/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.Formats;
import is.codion.common.event.EventDataListener;
import is.codion.swing.common.model.textfield.DocumentAdapter;

import java.text.NumberFormat;

import static is.codion.swing.common.ui.textfield.ParsingDocumentFilter.parsingDocumentFilter;

/**
 * A text field for integers.
 */
public final class IntegerField extends NumberField<Integer> {

  /**
   * Constructs a new IntegerField.
   */
  public IntegerField() {
    this(Formats.getNonGroupingIntegerFormat());
  }

  /**
   * Constructs a new IntegerField.
   * @param columns the number of columns
   */
  public IntegerField(final int columns) {
    this(Formats.getNonGroupingIntegerFormat(), columns);
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
    super(new NumberDocument<>(parsingDocumentFilter(new NumberParser<>(format, Integer.class), new NumberRangeValidator<>())), columns);
  }

  /**
   * @return the value
   */
  public Integer getInteger() {
    return getTypedDocument().getInteger();
  }

  /**
   * @param value the value to set
   */
  public void setInteger(final Integer value) {
    getTypedDocument().setNumber(value);
  }

  /**
   * @param listener a listener notified when the value changes
   */
  public void addIntegerListener(final EventDataListener<Integer> listener) {
    final NumberDocument<Integer> document = getTypedDocument();
    document.addDocumentListener((DocumentAdapter) e -> listener.onEvent(document.getInteger()));
  }
}