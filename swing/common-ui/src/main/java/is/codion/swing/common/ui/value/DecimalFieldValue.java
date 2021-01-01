/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.value.Nullable;
import is.codion.swing.common.ui.textfield.DoubleField;

final class DecimalFieldValue extends AbstractTextComponentValue<Double, DoubleField> {

  DecimalFieldValue(final DoubleField doubleField, final Nullable nullable, final UpdateOn updateOn) {
    super(doubleField, nullable == Nullable.YES ? null : 0d, updateOn);
    if (!isNullable()) {
      doubleField.setDouble(0d);
    }
  }

  @Override
  protected Double getComponentValue(final DoubleField component) {
    final Number number = component.getNumber();
    if (number == null) {
      return isNullable() ? null : 0d;
    }

    return number.doubleValue();
  }

  @Override
  protected void setComponentValue(final DoubleField component, final Double value) {
    component.setNumber(value);
  }
}
