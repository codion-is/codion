/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.value.Nullable;
import is.codion.swing.common.ui.textfield.LongField;

final class LongFieldValue extends AbstractTextComponentValue<Long, LongField> {

  LongFieldValue(final LongField longField, final Nullable nullable, final UpdateOn updateOn) {
    super(longField, nullable == Nullable.YES ? null : 0L, updateOn);
    if (!isNullable() && longField.getLong() == null) {
      longField.setLong(0L);
    }
  }

  @Override
  protected Long getComponentValue(final LongField component) {
    final Number number = component.getNumber();
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
