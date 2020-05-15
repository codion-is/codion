/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.value.Nullable;
import is.codion.swing.common.ui.textfield.DecimalField;

final class DecimalFieldValue extends AbstractTextComponentValue<Double, DecimalField> {

  DecimalFieldValue(final DecimalField decimalField, final Nullable nullable, final UpdateOn updateOn) {
    super(decimalField, nullable, updateOn);
  }

  @Override
  protected Double getComponentValue(final DecimalField component) {
    final Number number = component.getNumber();
    if (number == null) {
      return isNullable() ? null : 0d;
    }

    return number.doubleValue();
  }

  @Override
  protected void setComponentValue(final DecimalField component, final Double value) {
    component.setNumber(value);
  }
}
