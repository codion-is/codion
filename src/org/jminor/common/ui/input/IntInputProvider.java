/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import org.jminor.common.ui.textfield.IntField;

/**
 * A InputProvider implementation for int values.
*/
public final class IntInputProvider extends AbstractInputProvider<Integer, IntField> {

  public IntInputProvider(final Integer currentValue) {
    super(new IntField());
    if (currentValue != null) {
      getInputComponent().setInt(currentValue);
    }
  }

  @Override
  public Integer getValue() {
    return getInputComponent().getInt();
  }
}
