/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.textfield.BigDecimalField;

import java.math.BigDecimal;

final class BigDecimalFieldValue extends AbstractTextComponentValue<BigDecimal, BigDecimalField> {

  BigDecimalFieldValue(BigDecimalField doubleField, boolean nullable, UpdateOn updateOn) {
    super(doubleField, nullable ? null : BigDecimal.ZERO, updateOn);
  }

  @Override
  protected BigDecimal getComponentValue(BigDecimalField component) {
    return component.getNumber();
  }

  @Override
  protected void setComponentValue(BigDecimalField component, BigDecimal value) {
    component.setNumber(value);
  }
}
