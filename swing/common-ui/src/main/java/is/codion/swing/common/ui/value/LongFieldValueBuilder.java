/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.textfield.LongField;

import java.text.NumberFormat;

final class LongFieldValueBuilder extends AbstractNumberFieldValueBuilder<NumberFormat, Long, LongField> {

  LongFieldValueBuilder() {
    format(NumberFormat.getIntegerInstance());
  }

  @Override
  public ComponentValue<Long, LongField> build() {
    if (component == null) {
      component = new LongField(format, columns);
      component.setLong(initialValue);
    }

    return new LongFieldValue(component, nullable, updateOn);
  }
}
