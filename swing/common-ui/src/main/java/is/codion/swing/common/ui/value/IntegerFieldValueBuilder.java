/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.textfield.IntegerField;

import java.text.NumberFormat;

final class IntegerFieldValueBuilder extends AbstractNumberFieldValueBuilder<NumberFormat, Integer, IntegerField> {

  IntegerFieldValueBuilder() {
    format(NumberFormat.getIntegerInstance());
  }

  @Override
  public ComponentValue<Integer, IntegerField> build() {
    if (component == null) {
      component = new IntegerField(format, columns);
    }
    component.setInteger(initialValue);

    return new IntegerFieldValue(component, nullable, updateOn);
  }
}
