/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

/**
 * A builder for number based JSpinner
 */
public interface NumberSpinnerBuilder<T extends Number> extends SpinnerBuilder<T, NumberSpinnerBuilder<T>> {

  /**
   * @param minimum the minimum value
   * @return this builder instance
   */
  NumberSpinnerBuilder<T> minimum(T minimum);

  /**
   * @param maximum the maximum value
   * @return this builder instance
   */
  NumberSpinnerBuilder<T> maximum(T maximum);

  /**
   * @param stepSize the step size
   * @return this builder instance
   */
  NumberSpinnerBuilder<T> stepSize(T stepSize);

  /**
   * @param groupingUsed true if number format grouping should be used
   * @return this builder instance
   */
  NumberSpinnerBuilder<T> groupingUsed(boolean groupingUsed);

  /**
   * @param decimalFormatPattern the decimal format pattern
   * @return this builder instance
   */
  NumberSpinnerBuilder<T> decimalFormatPattern(String decimalFormatPattern);
}
