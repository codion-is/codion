/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.textfield.NumberField;

import java.text.NumberFormat;

abstract class AbstractNumberFieldBuilder<T extends Number, C extends NumberField<T>, B extends NumberFieldBuilder<T, C, B>>
        extends DefaultTextFieldBuilder<T, C, B> implements NumberFieldBuilder<T, C, B> {

  private Number maximumValue;
  private Number minimumValue;
  protected char groupingSeparator = 0;
  private boolean groupingUsed;

  protected AbstractNumberFieldBuilder(Class<T> type, Value<T> linkedValue) {
    super(type, linkedValue);
  }

  @Override
  public final B range(Number minimumValue, Number maximumValue) {
    minimumValue(minimumValue);
    maximumValue(maximumValue);
    return (B) this;
  }

  @Override
  public final B minimumValue(Number minimumValue) {
    this.minimumValue = minimumValue;
    return (B) this;
  }

  @Override
  public final B maximumValue(Number maximumValue) {
    this.maximumValue = maximumValue;
    return (B) this;
  }

  @Override
  public final B groupingSeparator(char groupingSeparator) {
    this.groupingSeparator = groupingSeparator;
    return (B) this;
  }

  @Override
  public final B groupingUsed(boolean groupingUsed) {
    this.groupingUsed = groupingUsed;
    return (B) this;
  }

  @Override
  protected final C createTextField() {
    NumberFormat format = cloneFormat((NumberFormat) getFormat());
    C numberField = createNumberField(format);
    numberField.setMinimumValue(minimumValue);
    numberField.setMaximumValue(maximumValue);
    if (groupingSeparator != 0) {
      numberField.setGroupingSeparator(groupingSeparator);
    }
    if (format == null) {
      numberField.setGroupingUsed(groupingUsed);
    }

    return numberField;
  }

  protected abstract C createNumberField(NumberFormat format);

  @Override
  protected final void setInitialValue(C component, T initialValue) {
    component.setNumber(initialValue);
  }

  private static NumberFormat cloneFormat(NumberFormat format) {
    if (format == null) {
      return null;
    }
    NumberFormat cloned = (NumberFormat) format.clone();
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
