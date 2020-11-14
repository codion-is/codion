/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.textfield.BigDecimalField;

import java.math.BigDecimal;

final class BigDecimalFieldValue extends AbstractTextComponentValue<BigDecimal, BigDecimalField> {

  BigDecimalFieldValue(final BigDecimalField doubleField, final UpdateOn updateOn) {
    super(doubleField, null, updateOn);
  }

  @Override
  protected BigDecimal getComponentValue(final BigDecimalField component) {
    return component.getNumber();
  }

  @Override
  protected void setComponentValue(final BigDecimalField component, final BigDecimal value) {
    component.setNumber(value);
  }
}
