/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.swing.common.ui.textfield.IntegerField;

final class IntegerFieldValue extends NumberFieldValue<IntegerField, Integer> {

  IntegerFieldValue(final IntegerField integerField, final boolean nullable, final boolean updateOnKeystroke) {
    super(integerField, nullable, updateOnKeystroke);
  }

  @Override
  public Integer get() {
    final Number number = getComponent().getNumber();
    if (number == null) {
      return isNullable() ? null : 0;
    }

    return number.intValue();
  }

  @Override
  protected void setComponentValue(final Integer value) {
    getComponent().setNumber(value);
  }
}
