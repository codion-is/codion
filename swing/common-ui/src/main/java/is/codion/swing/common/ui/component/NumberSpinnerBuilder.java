/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JSpinner;

/**
 * A builder for number based JSpinner
 */
public interface NumberSpinnerBuilder<T extends Number> extends ComponentBuilder<T, JSpinner, NumberSpinnerBuilder<T>> {

  /**
   * @param columns the number of colums in the spinner text component
   * @return this builder instance
   */
  NumberSpinnerBuilder<T> columns(int columns);

  /**
   * @param editable false if the spinner field should not be editable
   * @return this builder instance
   */
  NumberSpinnerBuilder<T> editable(boolean editable);

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
}
