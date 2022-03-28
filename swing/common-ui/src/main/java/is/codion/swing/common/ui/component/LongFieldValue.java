/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.textfield.LongField;

final class LongFieldValue extends AbstractTextComponentValue<Long, LongField> {

  LongFieldValue(LongField longField, boolean nullable, UpdateOn updateOn) {
    super(longField, nullable ? null : 0L, updateOn);
    if (!isNullable() && longField.getLong() == null) {
      longField.setLong(0L);
    }
  }

  @Override
  protected Long getComponentValue(LongField component) {
    Number number = component.getNumber();
    if (number == null) {
      return isNullable() ? null : 0L;
    }

    return number.longValue();
  }

  @Override
  protected void setComponentValue(LongField component, Long value) {
    component.setNumber(value);
  }
}
