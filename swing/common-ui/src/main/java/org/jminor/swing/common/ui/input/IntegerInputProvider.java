/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.swing.common.ui.textfield.IntegerField;

/**
 * A InputProvider implementation for integer values.
 */
public final class IntegerInputProvider extends AbstractInputProvider<Integer, IntegerField> {

  /**
   * Instantiates a new IntegerInputProvider.
   * @param initialValue the initial value
   */
  public IntegerInputProvider(final Integer initialValue) {
    this(initialValue, (int) Double.NEGATIVE_INFINITY, (int) Double.POSITIVE_INFINITY);
  }

  /**
   * Instantiates a new IntegerInputProvider.
   * @param initialValue the initial value
   * @param minValue the minimum value
   * @param maxValue the maximum value
   */
  public IntegerInputProvider(final Integer initialValue, final int minValue, final int maxValue) {
    super(new IntegerField());
    getInputComponent().setRange(minValue, maxValue);
    getInputComponent().setInteger(initialValue);
  }

  /** {@inheritDoc} */
  @Override
  public Integer getValue() {
    return getInputComponent().getInteger();
  }

  /** {@inheritDoc} */
  @Override
  public void setValue(final Integer value) {
    getInputComponent().setInteger(value);
  }
}
