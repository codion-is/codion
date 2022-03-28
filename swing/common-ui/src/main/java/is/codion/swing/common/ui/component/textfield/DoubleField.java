/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

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
  public DoubleField(int columns) {
    this(createDefaultFormat(), columns);
  }

  /**
   * Instantiates a new DecimalField
   * @param format the format to use
   */
  public DoubleField(DecimalFormat format) {
    this(format, 0);
  }

  /**
   * Instantiates a new DecimalField
   * @param format the format to use
   * @param columns the number of columns
   */
  public DoubleField(DecimalFormat format, int columns) {
    super(new DecimalDocument<>(format, false), columns);
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
  public void setMaximumFractionDigits(int maximumFractionDigits) {
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
  public void setDouble(Double value) {
    getTypedDocument().setNumber(value);
  }

  private static DecimalFormat createDefaultFormat() {
    DecimalFormat format = new DecimalFormat();
    format.setMaximumFractionDigits(DecimalDocument.MAXIMUM_FRACTION_DIGITS);

    return format;
  }
}
