/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
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
    super(new DoubleField());
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