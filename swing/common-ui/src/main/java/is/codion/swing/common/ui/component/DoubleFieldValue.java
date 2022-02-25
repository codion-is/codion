/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.DoubleField;

final class DoubleFieldValue extends AbstractTextComponentValue<Double, DoubleField> {

  DoubleFieldValue(DoubleField doubleField, boolean nullable, UpdateOn updateOn) {
    super(doubleField, nullable ? null : 0d, updateOn);
    if (!isNullable() && doubleField.getDouble() == null) {
      doubleField.setDouble(0d);
    }
  }

  @Override
  protected Double getComponentValue(DoubleField component) {
    Number number = component.getNumber();
    if (number == null) {
      return isNullable() ? null : 0d;
    }

    return number.doubleValue();
  }

  @Override
  protected void setComponentValue(DoubleField component, Double value) {
    component.setNumber(value);
  }
}
