/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.swing.common.ui.textfield.LongField;

final class LongFieldValue extends NumberFieldValue<LongField, Long> {

  LongFieldValue(final LongField longField, final boolean nullable, final boolean updateOnKeystroke) {
    super(longField, nullable, updateOnKeystroke);
  }

  @Override
  public Long get() {
    final Number number = getComponent().getNumber();
    if (number == null) {
      return isNullable() ? null : 0L;
    }

    return number.longValue();
  }

  @Override
  protected void setComponentValue(final Long value) {
    getComponent().setNumber(value);
  }
}
