/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.ui.textfield.IntField;

/**
 * A InputProvider implementation for int values.
*/
public final class IntInputProvider extends AbstractInputProvider<Integer, IntField> {

  /**
   * Instantiates a new IntInputProvider.
   * @param initialValue the initial value
   */
  public IntInputProvider(final Integer initialValue) {
    this(initialValue, (int) Double.NEGATIVE_INFINITY, (int) Double.POSITIVE_INFINITY);
  }

  /**
   * Instantiates a new IntInputProvider.
   * @param initialValue the initial value
   * @param minValue the minimum value
   * @param maxValue the maximum value
   */
  public IntInputProvider(final Integer initialValue, final int minValue, final int maxValue) {
    super(new IntField(minValue, maxValue));
    if (initialValue != null) {
      getInputComponent().setInt(initialValue);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Integer getValue() {
    return getInputComponent().getInt();
  }
}
