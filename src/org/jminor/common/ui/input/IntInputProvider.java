/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.ui.textfield.IntField;

/**
 * A InputManager implementation for int values.
*/
public class IntInputProvider extends AbstractInputProvider<Integer> {

  public IntInputProvider(final Integer currentValue) {
    super(new IntField());
    if (currentValue != null)
      ((IntField) getInputComponent()).setInt(currentValue);
  }

  @Override
  public Integer getValue() {
    return ((IntField) getInputComponent()).getInt();
  }
}
