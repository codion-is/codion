/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.LongField;

final class LongFieldValue extends AbstractTextComponentValue<Long, LongField> {

  LongFieldValue(final LongField longField, final boolean nullable, final UpdateOn updateOn) {
    super(longField, nullable ? null : 0L, updateOn);
    if (!isNullable() && longField.getLong() == null) {
      longField.setLong(0L);
    }
  }

  @Override
  protected Long getComponentValue(final LongField component) {
    Number number = component.getNumber();
    if (number == null) {
      return isNullable() ? null : 0L;
    }

    return number.longValue();
  }

  @Override
  protected void setComponentValue(final LongField component, final Long value) {
    component.setNumber(value);
  }
}
