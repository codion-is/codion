/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.common.value.Value;

import javax.swing.SpinnerNumberModel;

import static java.util.Objects.requireNonNull;

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

  /**
   * @param commitOnValidEdit true if the spinner should commit on a valid edit
   * @return this builder instance
   * @see javax.swing.text.DefaultFormatter#setCommitsOnValidEdit(boolean)
   */
  NumberSpinnerBuilder<T> commitOnValidEdit(boolean commitOnValidEdit);

  /**
   * @param spinnerNumberModel the spinner model
   * @param valueClass the value class
   * @return a new {@link NumberSpinnerBuilder} instance
   * @param <T> the number type
   */
  static <T extends Number> NumberSpinnerBuilder<T> builder(SpinnerNumberModel spinnerNumberModel,
                                                            Class<T> valueClass) {
    return new DefaultNumberSpinnerBuilder<>(spinnerNumberModel, valueClass, null);
  }

  /**
   * @param spinnerNumberModel the spinner model
   * @param valueClass the value class
   * @param linkedValue the value to link to
   * @return a new {@link NumberSpinnerBuilder} instance
   * @param <T> the number type
   */
  static <T extends Number> NumberSpinnerBuilder<T> builder(SpinnerNumberModel spinnerNumberModel,
                                                            Class<T> valueClass, Value<T> linkedValue) {
    return new DefaultNumberSpinnerBuilder<>(spinnerNumberModel, valueClass, requireNonNull(linkedValue));
  }
}
