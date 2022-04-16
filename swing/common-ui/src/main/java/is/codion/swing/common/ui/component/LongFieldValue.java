/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.textfield.NumberField;

final class LongFieldValue extends AbstractTextComponentValue<Long, NumberField<Long>> {

  LongFieldValue(NumberField<Long> longField, boolean nullable, UpdateOn updateOn) {
    super(longField, nullable ? null : 0L, updateOn);
    if (!isNullable() && longField.getNumber() == null) {
      longField.setNumber(0L);
    }
  }

  @Override
  protected Long getComponentValue(NumberField<Long> component) {
    Number number = component.getNumber();
    if (number == null) {
      return isNullable() ? null : 0L;
    }

    return number.longValue();
  }

  @Override
  protected void setComponentValue(NumberField<Long> component, Long value) {
    component.setNumber(value);
  }
}
