/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JSpinner;

/**
 * A builder for JSpinner based on a list of {@link is.codion.common.item.Item}s.
 * @param <T> the value type
 */
public interface ItemSpinnerBuilder<T> extends ComponentBuilder<T, JSpinner, ItemSpinnerBuilder<T>> {

  /**
   * @param columns the number of colums in the spinner text component
   * @return this builder instance
   */
  ItemSpinnerBuilder<T> columns(int columns);
}
