/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.NumberField;

import java.text.NumberFormat;

abstract class AbstractNumberFieldBuilder<T extends Number, C extends NumberField<T>, B extends NumberFieldBuilder<T, C, B>>
        extends DefaultTextFieldBuilder<T, C, B> implements NumberFieldBuilder<T, C, B> {

  private Double maximumValue;
  private Double minimumValue;
  protected char groupingSeparator = 0;
  private boolean groupingUsed;

  AbstractNumberFieldBuilder(final Class<T> type) {
    super(type);
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
    final C field = createNumberField(getFormat() == null ? null : cloneFormat((NumberFormat) getFormat()));
    if (minimumValue != null && maximumValue != null) {
      field.setRange(Math.min(minimumValue, 0), maximumValue);
    }
    if (groupingSeparator != 0) {
      field.setSeparators((char) 0, groupingSeparator);
    }
    field.setGroupingUsed(groupingUsed);

    return field;
  }

  protected abstract C createNumberField(final NumberFormat format);

  private static NumberFormat cloneFormat(final NumberFormat format) {
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
