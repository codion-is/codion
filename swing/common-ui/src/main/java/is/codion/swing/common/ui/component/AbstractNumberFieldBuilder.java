/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.NumberField;

import java.text.NumberFormat;

abstract class AbstractNumberFieldBuilder<T extends Number, C extends NumberField<T>, B extends NumberFieldBuilder<T, C, B>>
        extends DefaultTextFieldBuilder<T, C, B> implements NumberFieldBuilder<T, C, B> {

  private Double maximumValue;
  private Double minimumValue;
  protected char groupingSeparator = 0;
  private boolean groupingUsed;

  protected AbstractNumberFieldBuilder(final Class<T> type, final Value<T> linkedValue) {
    super(type, linkedValue);
  }

  @Override
  public final B minimumValue(final Double minimumValue) {
    this.minimumValue = minimumValue;
    return (B) this;
  }

  @Override
  public final B maximumValue(final Double maximumValue) {
    this.maximumValue = maximumValue;
    return (B) this;
  }

  @Override
  public final B groupingSeparator(final char groupingSeparator) {
    this.groupingSeparator = groupingSeparator;
    return (B) this;
  }

  @Override
  public final B groupingUsed(final boolean groupingUsed) {
    this.groupingUsed = groupingUsed;
    return (B) this;
  }

  @Override
  protected final C createTextField() {
    final NumberFormat format = cloneFormat((NumberFormat) getFormat());
    final C numberField = createNumberField(format);
    if (minimumValue != null && maximumValue != null) {
      numberField.setRange(Math.min(minimumValue, 0), maximumValue);
    }
    if (groupingSeparator != 0) {
      numberField.setGroupingSeparator(groupingSeparator);
    }
    if (format == null) {
      numberField.setGroupingUsed(groupingUsed);
    }

    return numberField;
  }

  protected abstract C createNumberField(final NumberFormat format);

  @Override
  protected final void setInitialValue(final C component, final T initialValue) {
    component.setNumber(initialValue);
  }

  private static NumberFormat cloneFormat(final NumberFormat format) {
    if (format == null) {
      return null;
    }
    final NumberFormat cloned = (NumberFormat) format.clone();
    cloned.setGroupingUsed(format.isGroupingUsed());
    cloned.setMaximumIntegerDigits(format.getMaximumIntegerDigits());
    cloned.setMaximumFractionDigits(format.getMaximumFractionDigits());
    cloned.setMinimumFractionDigits(format.getMinimumFractionDigits());
    cloned.setRoundingMode(format.getRoundingMode());
    cloned.setCurrency(format.getCurrency());
    cloned.setParseIntegerOnly(format.isParseIntegerOnly());

    return cloned;
  }
}
