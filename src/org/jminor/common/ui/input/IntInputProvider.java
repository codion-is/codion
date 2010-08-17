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
    super(new IntField());
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
