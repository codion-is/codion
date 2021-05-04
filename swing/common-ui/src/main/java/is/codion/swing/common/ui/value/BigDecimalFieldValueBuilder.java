/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.textfield.BigDecimalField;

import java.math.BigDecimal;
import java.text.DecimalFormat;

final class BigDecimalFieldValueBuilder extends AbstractNumberFieldValueBuilder<DecimalFormat, BigDecimal, BigDecimalField> {

  BigDecimalFieldValueBuilder() {
    format(new DecimalFormat());
  }

  @Override
  public ComponentValue<BigDecimal, BigDecimalField> build() {
    if (component == null) {
      component = new BigDecimalField(format, columns);
      component.setBigDecimal(initialValue);
    }

    return new BigDecimalFieldValue(component, nullable, updateOn);
  }
}
