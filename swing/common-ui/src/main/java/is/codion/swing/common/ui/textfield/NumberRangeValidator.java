/*
 * Copyright (c) 2021 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.value.Value;

import java.util.ResourceBundle;

final class NumberRangeValidator<T extends Number> implements Value.Validator<T> {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(NumberRangeValidator.class.getName());

  private double minimumValue = Double.NEGATIVE_INFINITY;
  private double maximumValue = Double.POSITIVE_INFINITY;

  @Override
  public void validate(final T value) {
    if (!isWithinRange(value.doubleValue())) {
      throw new IllegalArgumentException(MESSAGES.getString("value_outside_range") + " " + minimumValue + " - " + maximumValue);
    }
  }

  /**
   * Sets the range of values this document filter should allow
   * @param min the minimum value
   * @param max the maximum value
   */
  void setRange(final double min, final double max) {
    this.minimumValue = min;
    this.maximumValue = max;
  }

  /**
   * @return the minimum value this field should accept
   */
  double getMinimumValue() {
    return minimumValue;
  }

  /**
   * @return the maximum value this field should accept
   */
  double getMaximumValue() {
    return maximumValue;
  }

  /**
   * @param value the value to check
   * @return true if this value falls within the allowed range for this document
   */
  boolean isWithinRange(final double value) {
    return value >= minimumValue && value <= maximumValue;
  }
}
