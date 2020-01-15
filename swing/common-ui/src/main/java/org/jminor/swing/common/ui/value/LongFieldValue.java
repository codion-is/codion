/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.swing.common.ui.textfield.LongField;

final class LongFieldValue extends NumberFieldValue<Long, LongField> {

  LongFieldValue(final LongField longField, final boolean nullable, final boolean updateOnKeystroke) {
    super(longField, nullable, updateOnKeystroke);
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
