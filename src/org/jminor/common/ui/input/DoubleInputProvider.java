/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.ui.textfield.DoubleField;

/**
 * A InputManager implementation for double values.
*/
public class DoubleInputProvider extends AbstractInputProvider<Double> {

  public DoubleInputProvider(final Double currentValue) {
    super(new DoubleField());
    if (currentValue != null)
      ((DoubleField) getInputComponent()).setDouble(currentValue);
  }

  @Override
  public Double getValue() {
    return ((DoubleField) getInputComponent()).getDouble();
  }
}