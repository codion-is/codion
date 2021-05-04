/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.textfield.DoubleField;

import java.text.DecimalFormat;

final class DoubleFieldValueBuilder extends AbstractNumberFieldValueBuilder<DecimalFormat, Double, DoubleField> {

  DoubleFieldValueBuilder() {
    format(new DecimalFormat());
  }

  @Override
  public ComponentValue<Double, DoubleField> build() {
    if (component == null) {
      component = new DoubleField(format, columns);
      component.setDouble(initialValue);
    }

    return new DoubleFieldValue(component, nullable, updateOn);
  }
}
