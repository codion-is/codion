/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.swing.common.ui.textfield.LongField;

final class LongFieldValue extends AbstractTextComponentValue<Long, LongField> {

  LongFieldValue(final LongField longField, final Nullable nullable, final UpdateOn updateOn) {
    super(longField, nullable, updateOn);
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
