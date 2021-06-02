/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JSpinner;

/**
 * A builder for JSpinner
 */
public interface SpinnerBuilder<T extends Number> extends ComponentBuilder<T, JSpinner, SpinnerBuilder<T>> {

  /**
   * @param columns the number of colums in the spinner text component
   * @return this builder instance
   */
  SpinnerBuilder<T> columns(int columns);

  /**
   * @param editable false if the spinner field should not be editable
   * @return this builder instance
   */
  SpinnerBuilder<T> editable(boolean editable);

  /**
   * @param minimum the minimum value
   * @return this builder instance
   */
  SpinnerBuilder<T> minimum(T minimum);

  /**
   * @param maximum the maximum value
   * @return this builder instance
   */
  SpinnerBuilder<T> maximum(T maximum);

  /**
   * @param stepSize the step size
   * @return this builder instance
   */
  SpinnerBuilder<T> stepSize(T stepSize);
}
