/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.value.Nullable;
import is.codion.swing.common.ui.textfield.DecimalField;

import java.math.BigDecimal;

final class BigDecimalFieldValue extends AbstractTextComponentValue<BigDecimal, DecimalField> {

  BigDecimalFieldValue(final DecimalField decimalField, final UpdateOn updateOn) {
    super(decimalField, Nullable.YES, updateOn);
  }

  @Override
  protected BigDecimal getComponentValue(final DecimalField component) {
    return (BigDecimal) component.getNumber();
  }

  @Override
  protected void setComponentValue(final DecimalField component, final BigDecimal value) {
    component.setNumber(value);
  }
}
