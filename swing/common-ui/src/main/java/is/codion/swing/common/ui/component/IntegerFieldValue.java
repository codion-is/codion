/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.textfield.NumberField;

final class IntegerFieldValue extends AbstractTextComponentValue<Integer, NumberField<Integer>> {

  IntegerFieldValue(NumberField<Integer> integerField, boolean nullable, UpdateOn updateOn) {
    super(integerField, nullable ? null : 0, updateOn);
    if (!isNullable() && integerField.getValue() == null) {
      integerField.setValue(0);
    }
  }

  @Override
  protected Integer getComponentValue(NumberField<Integer> component) {
    Number value = component.getValue();
    if (value == null) {
      return isNullable() ? null : 0;
    }

    return value.intValue();
  }

  @Override
  protected void setComponentValue(NumberField<Integer> component, Integer value) {
    component.setValue(value);
  }
}
