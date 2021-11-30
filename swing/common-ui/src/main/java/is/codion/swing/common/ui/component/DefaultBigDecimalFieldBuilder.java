/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.BigDecimalField;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

final class DefaultBigDecimalFieldBuilder extends AbstractNumberFieldBuilder<BigDecimal, BigDecimalField, BigDecimalFieldBuilder>
        implements BigDecimalFieldBuilder {

  private int maximumFractionDigits = -1;
  private char decimalSeparator = 0;

  DefaultBigDecimalFieldBuilder(final Value<BigDecimal> linkedValue) {
    super(BigDecimal.class, linkedValue);
  }

  @Override
  public BigDecimalFieldBuilder maximumFractionDigits(final int maximumFractionDigits) {
    this.maximumFractionDigits = maximumFractionDigits;
    return this;
  }

  @Override
  public BigDecimalFieldBuilder decimalSeparator(final char decimalSeparator) {
    if (decimalSeparator == groupingSeparator) {
      throw new IllegalArgumentException("Decimal separator must not be the same as grouping separator");
    }
    this.decimalSeparator = decimalSeparator;
    return this;
  }

  @Override
  protected BigDecimalField createNumberField(final NumberFormat format) {
    final BigDecimalField field = format == null ? new BigDecimalField() : new BigDecimalField((DecimalFormat) format);
    if (decimalSeparator != 0) {
      field.setDecimalSeparator(decimalSeparator);
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
