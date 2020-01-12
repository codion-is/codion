/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.swing.common.ui.textfield.DecimalField;

import java.math.BigDecimal;

final class BigDecimalFieldValue extends NumberFieldValue<DecimalField, BigDecimal> {

  BigDecimalFieldValue(final DecimalField decimalField, final boolean updateOnKeystroke) {
    super(decimalField, true, updateOnKeystroke);
  }

  @Override
  public BigDecimal get() {
    return (BigDecimal) getComponent().getNumber();
  }

  @Override
  protected void setComponentValue(final BigDecimal value) {
    getComponent().setNumber(value);
  }
}
