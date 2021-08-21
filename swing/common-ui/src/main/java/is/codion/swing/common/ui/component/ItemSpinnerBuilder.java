/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JSpinner;

/**
 * A builder for JSpinner based on a list of {@link Item}s.
 */
public interface ItemSpinnerBuilder<T> extends ComponentBuilder<T, JSpinner, ItemSpinnerBuilder<T>> {

  /**
   * @param columns the number of colums in the spinner text component
   * @return this builder instance
   */
  ItemSpinnerBuilder<T> columns(int columns);
}
