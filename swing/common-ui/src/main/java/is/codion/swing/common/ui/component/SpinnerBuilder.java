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
}
