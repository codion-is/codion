/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.DoubleField;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;

import java.text.DecimalFormat;
import java.text.NumberFormat;

final class DefaultDoubleFieldBuilder extends AbstractNumberFieldBuilder<Double, DoubleField, DoubleFieldBuilder> implements DoubleFieldBuilder {

  private int maximumFractionDigits = -1;

  DefaultDoubleFieldBuilder() {
    super(Double.class);
  }

  @Override
  public DoubleFieldBuilder maximumFractionDigits(final int maximumFractionDigits) {
    this.maximumFractionDigits = maximumFractionDigits;
    return this;
  }

  @Override
  protected DoubleField createTextField() {
    final DoubleField field = format == null ? new DoubleField() : new DoubleField((DecimalFormat) cloneFormat((NumberFormat) format));
    if (minimumValue != null && maximumValue != null) {
      field.setRange(Math.min(minimumValue, 0), maximumValue);
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
