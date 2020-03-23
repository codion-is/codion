/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.swing.common.ui.textfield.IntegerField;

final class IntegerFieldValue extends AbstractTextComponentValue<Integer, IntegerField> {

  IntegerFieldValue(final IntegerField integerField, final boolean nullable, final UpdateOn updateOn) {
    super(integerField, nullable, updateOn);
  }

  @Override
  protected Integer getComponentValue(final IntegerField component) {
    final Number number = component.getNumber();
    if (number == null) {
      return isNullable() ? null : 0;
    }

    return number.intValue();
  }

  @Override
  protected void setComponentValue(final IntegerField component, final Integer value) {
    component.setNumber(value);
  }
}
