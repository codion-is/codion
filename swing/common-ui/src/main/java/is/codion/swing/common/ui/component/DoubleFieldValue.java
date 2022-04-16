/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.textfield.NumberField;

final class DoubleFieldValue extends AbstractTextComponentValue<Double, NumberField<Double>> {

  DoubleFieldValue(NumberField<Double> doubleField, boolean nullable, UpdateOn updateOn) {
    super(doubleField, nullable ? null : 0d, updateOn);
    if (!isNullable() && doubleField.getValue() == null) {
      doubleField.setValue(0d);
    }
  }

  @Override
  protected Double getComponentValue(NumberField<Double> component) {
    Number value = component.getValue();
    if (value == null) {
      return isNullable() ? null : 0d;
    }

    return value.doubleValue();
  }

  @Override
  protected void setComponentValue(NumberField<Double> component, Double value) {
    component.setValue(value);
  }
}
