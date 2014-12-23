/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.ui.textfield.DoubleField;

/**
 * A InputProvider implementation for double values.
*/
public final class DoubleInputProvider extends AbstractInputProvider<Double, DoubleField> {

  /**
   * Instantiates a new DoubleInputProvider.
   * @param initialValue the initial value
   */
  public DoubleInputProvider(final Double initialValue) {
    this(initialValue, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
  }

  /**
   * Instantiates a new DoubleInputProvider.
   * @param initialValue the initial value
   * @param minValue the minimum value
   * @param maxValue the maximum value
   */
  public DoubleInputProvider(final Double initialValue, final double minValue, final double maxValue) {
    super(new DoubleField(minValue, maxValue));
    if (initialValue != null) {
      getInputComponent().setDouble(initialValue);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Double getValue() {
    return getInputComponent().getDouble();
  }
}