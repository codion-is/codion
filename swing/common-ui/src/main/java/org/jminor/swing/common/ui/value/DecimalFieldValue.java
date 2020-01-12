/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.swing.common.ui.textfield.DecimalField;

final class DecimalFieldValue extends NumberFieldValue<DecimalField, Double> {

  DecimalFieldValue(final DecimalField decimalField, final boolean nullable, final boolean updateOnKeystroke) {
    super(decimalField, nullable, updateOnKeystroke);
  }

  @Override
  public Double get() {
    final Number number = getComponent().getNumber();
    if (number == null) {
      return isNullable() ? null : 0d;
    }

    return number.doubleValue();
  }

  @Override
  protected void setInternal(final Double value) {
    getComponent().setNumber(value);
  }
}
