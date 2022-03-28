/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.textfield.DoubleField;

/**
 * A builder {@link DoubleField}.
 */
public interface DoubleFieldBuilder extends NumberFieldBuilder<Double, DoubleField, DoubleFieldBuilder> {

  /**
   * @param maximumFractionDigits the maximum fraction digits
   * @return this builder instance
   */
  DoubleFieldBuilder maximumFractionDigits(int maximumFractionDigits);

  /**
   * Set the decimal separator for this field
   * @param decimalSeparator the decimal separator
   * @return this builder instance
   * @throws IllegalArgumentException in case the decimal separator is the same as the grouping separator
   */
  DoubleFieldBuilder decimalSeparator(char decimalSeparator);

  /**
   * Sets the allowed value range
   * @param from the from value
   * @param to the to value
   * @return this builder instance
   */
  DoubleFieldBuilder range(double from, double to);
}
