/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

final class DefaultBigDecimalFieldBuilder extends AbstractNumberFieldBuilder<BigDecimal, BigDecimalField, BigDecimalFieldBuilder> implements BigDecimalFieldBuilder {

  private int maximumFractionDigits = -1;

  DefaultBigDecimalFieldBuilder() {
    super(BigDecimal.class);
  }

  @Override
  public BigDecimalFieldBuilder maximumFractionDigits(final int maximumFractionDigits) {
    this.maximumFractionDigits = maximumFractionDigits;
    return this;
  }

  @Override
  protected BigDecimalField createTextField() {
    final BigDecimalField field = format == null ? new BigDecimalField() : new BigDecimalField((DecimalFormat) cloneFormat((NumberFormat) format));
    if (minimumValue != null && maximumValue != null) {
      field.setRange(Math.min(minimumValue, 0), maximumValue);
    }
    if (maximumFractionDigits > 0) {
      field.setMaximumFractionDigits(maximumFractionDigits);
    }

    return field;
  }

  @Override
  protected ComponentValue<BigDecimal, BigDecimalField> buildComponentValue(final BigDecimalField component) {
    return ComponentValues.bigDecimalField(component, true, updateOn);
  }
}
