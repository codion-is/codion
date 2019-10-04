/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.swing.common.ui.textfield.DecimalField;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * A InputProvider implementation for BigDecimal values.
 */
public final class BigDecimalInputProvider extends AbstractInputProvider<BigDecimal, DecimalField> {

  /**
   * Instantiates a new BigDecimalInputProvider.
   * @param initialValue the initial value
   */
  public BigDecimalInputProvider(final BigDecimal initialValue) {
    this(initialValue, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
  }

  /**
   * Instantiates a new BigDecimalInputProvider.
   * @param initialValue the initial value
   * @param minValue the minimum value
   * @param maxValue the maximum value
   */
  public BigDecimalInputProvider(final BigDecimal initialValue, final double minValue, final double maxValue) {
    super(new DecimalField(createFormat()));
    getInputComponent().setRange(minValue, maxValue);
    getInputComponent().setBigDecimal(initialValue);
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimal getValue() {
    return getInputComponent().getBigDecimal();
  }

  private static DecimalFormat createFormat() {
    final DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance();
    format.setParseBigDecimal(true);

    return format;
  }
}