/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.ui.textfield.DoubleField;

/**
 * A InputProvider implementation for double values.
*/
public final class DoubleInputProvider extends AbstractInputProvider<Double, DoubleField> {

  public DoubleInputProvider(final Double currentValue) {
    super(new DoubleField());
    if (currentValue != null) {
      getInputComponent().setDouble(currentValue);
    }
  }

  @Override
  public Double getValue() {
    return getInputComponent().getDouble();
  }
}