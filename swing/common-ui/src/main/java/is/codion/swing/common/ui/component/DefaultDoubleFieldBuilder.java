/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.textfield.NumberField;

import java.text.DecimalFormat;
import java.text.NumberFormat;

final class DefaultDoubleFieldBuilder<B extends DecimalFieldBuilder<Double, B>> extends AbstractNumberFieldBuilder<Double, B>
        implements DecimalFieldBuilder<Double, B> {

  private int maximumFractionDigits = -1;
  private char decimalSeparator = 0;

  DefaultDoubleFieldBuilder(Value<Double> linkedValue) {
    super(Double.class, linkedValue);
  }

  @Override
  public B maximumFractionDigits(int maximumFractionDigits) {
    this.maximumFractionDigits = maximumFractionDigits;
    return (B) this;
  }

  @Override
  public B decimalSeparator(char decimalSeparator) {
    if (decimalSeparator == groupingSeparator) {
      throw new IllegalArgumentException("Decimal separator must not be the same as grouping separator");
    }
    this.decimalSeparator = decimalSeparator;
    return (B) this;
  }

  @Override
  protected NumberField<Double> createNumberField(NumberFormat format) {
    NumberField<Double> field = format == null ? NumberField.doubleField() : NumberField.doubleField((DecimalFormat) format);
    if (decimalSeparator != 0) {
      field.setDecimalSeparator(decimalSeparator);
    }
    if (maximumFractionDigits > 0) {
      field.setMaximumFractionDigits(maximumFractionDigits);
    }

    return field;
  }

  @Override
  protected ComponentValue<Double, NumberField<Double>> createComponentValue(NumberField<Double> component) {
    return new DoubleFieldValue(component, true, updateOn);
  }
}
