/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.DoubleField;

final class DoubleFieldValue extends AbstractTextComponentValue<Double, DoubleField> {

  DoubleFieldValue(final DoubleField doubleField, final boolean nullable, final UpdateOn updateOn) {
    super(doubleField, nullable ? null : 0d, updateOn);
    if (!isNullable() && doubleField.getDouble() == null) {
      doubleField.setDouble(0d);
    }
  }

  @Override
  protected Double getComponentValue(final DoubleField component) {
    Number number = component.getNumber();
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
