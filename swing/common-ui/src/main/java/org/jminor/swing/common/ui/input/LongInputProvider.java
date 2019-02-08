/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.swing.common.ui.textfield.LongField;

/**
 * A InputProvider implementation for long values.
 */
public final class LongInputProvider extends AbstractInputProvider<Long, LongField> {

  /**
   * Instantiates a new LongInputProvider.
   * @param initialValue the initial value
   */
  public LongInputProvider(final Long initialValue) {
    this(initialValue, Long.MIN_VALUE, Long.MAX_VALUE);
  }

  /**
   * Instantiates a new LongInputProvider.
   * @param initialValue the initial value
   * @param minValue the minimum value
   * @param maxValue the maximum value
   */
  public LongInputProvider(final Long initialValue, final long minValue, final long maxValue) {
    super(new LongField());
    getInputComponent().setRange(minValue, maxValue);
    getInputComponent().setLong(initialValue);
  }

  /** {@inheritDoc} */
  @Override
  public Long getValue() {
    return getInputComponent().getLong();
  }
}
