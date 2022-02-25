/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.DoubleField;

import java.text.DecimalFormat;
import java.text.NumberFormat;

final class DefaultDoubleFieldBuilder extends AbstractNumberFieldBuilder<Double, DoubleField, DoubleFieldBuilder>
        implements DoubleFieldBuilder {

  private int maximumFractionDigits = -1;
  private char decimalSeparator = 0;

  DefaultDoubleFieldBuilder(final Value<Double> linkedValue) {
    super(Double.class, linkedValue);
  }

  @Override
  public DoubleFieldBuilder maximumFractionDigits(final int maximumFractionDigits) {
    this.maximumFractionDigits = maximumFractionDigits;
    return this;
  }

  @Override
  public DoubleFieldBuilder decimalSeparator(final char decimalSeparator) {
    if (decimalSeparator == groupingSeparator) {
      throw new IllegalArgumentException("Decimal separator must not be the same as grouping separator");
    }
    this.decimalSeparator = decimalSeparator;
    return this;
  }

  @Override
  public DoubleFieldBuilder range(final double from, final double to) {
    minimumValue(from);
    maximumValue(to);
    return this;
  }

  @Override
  protected DoubleField createNumberField(final NumberFormat format) {
    DoubleField field = format == null ? new DoubleField() : new DoubleField((DecimalFormat) format);
    if (decimalSeparator != 0) {
      field.setDecimalSeparator(decimalSeparator);
    }
    if (maximumFractionDigits > 0) {
      field.setMaximumFractionDigits(maximumFractionDigits);
    }

    return field;
  }

  @Override
  protected ComponentValue<Double, DoubleField> buildComponentValue(final DoubleField component) {
    return ComponentValues.doubleField(component, true, updateOn);
  }
}
