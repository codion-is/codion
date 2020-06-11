/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.event.EventDataListener;
import is.codion.swing.common.model.textfield.DocumentAdapter;

import java.text.DecimalFormat;

/**
 * A text field for Double.
 */
public final class DoubleField extends NumberField<Double> {

  /**
   * Instantiates a new DecimalField.
   */
  public DoubleField() {
    this(0);
  }

  /**
   * Instantiates a new DecimalField
   * @param columns the number of columns
   */
  public DoubleField(final int columns) {
    this(createDefaultFormat(), columns);
  }

  /**
   * Instantiates a new DecimalField
   * @param format the format to use
   */
  public DoubleField(final DecimalFormat format) {
    this(format, 0);
  }

  /**
   * Instantiates a new DecimalField
   * @param format the format to use
   * @param columns the number of columns
   */
  public DoubleField(final DecimalFormat format, final int columns) {
    super(new DecimalDocument<>(format), columns);
  }

  /**
   * @return the maximum number of fraction digits this field shows
   */
  public int getMaximumFractionDigits() {
    return ((DecimalDocument<Double>) getTypedDocument()).getMaximumFractionDigits();
  }

  /**
   * @param maximumFractionDigits the maximum number of fraction digits this field shows
   */
  public void setMaximumFractionDigits(final int maximumFractionDigits) {
    ((DecimalDocument<Double>) getTypedDocument()).setMaximumFractionDigits(maximumFractionDigits);
  }

  /**
   * @return the current value
   */
  public Double getDouble() {
    return getTypedDocument().getDouble();
  }

  /**
   * @param value the value to set
   */
  public void setDouble(final Double value) {
    getTypedDocument().setNumber(value);
  }

  /**
   * @param listener a listener notified when the value changes
   */
  public void addDoubleListener(final EventDataListener<Double> listener) {
    final NumberDocument<Double> document = getTypedDocument();
    document.addDocumentListener((DocumentAdapter) e -> listener.onEvent(document.getDouble()));
  }

  private static DecimalFormat createDefaultFormat() {
    final DecimalFormat format = new DecimalFormat();
    format.setMaximumFractionDigits(DecimalDocument.MAXIMUM_FRACTION_DIGITS);

    return format;
  }
}
