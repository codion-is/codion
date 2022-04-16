/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.textfield.NumberField;

import java.math.BigDecimal;

final class BigDecimalFieldValue extends AbstractTextComponentValue<BigDecimal, NumberField<BigDecimal>> {

  BigDecimalFieldValue(NumberField<BigDecimal> doubleField, boolean nullable, UpdateOn updateOn) {
    super(doubleField, nullable ? null : BigDecimal.ZERO, updateOn);
  }

  @Override
  protected BigDecimal getComponentValue(NumberField<BigDecimal> component) {
    return component.getNumber();
  }

  @Override
  protected void setComponentValue(NumberField<BigDecimal> component, BigDecimal value) {
    component.setNumber(value);
  }
}
