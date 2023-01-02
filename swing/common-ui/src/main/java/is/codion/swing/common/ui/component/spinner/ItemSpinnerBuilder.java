/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.common.value.Value;

import javax.swing.SpinnerListModel;

import static java.util.Objects.requireNonNull;

/**
 * A builder for JSpinner based on a list of {@link is.codion.common.item.Item}s.
 */
public interface ItemSpinnerBuilder<T> extends SpinnerBuilder<T, ItemSpinnerBuilder<T>> {

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @return a builder for a JSpinner
   */
  static <T> ItemSpinnerBuilder<T> builder(SpinnerListModel spinnerModel) {
    return new DefaultItemSpinnerBuilder<>(spinnerModel, null);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a builder for a JSpinner
   */
  static <T> ItemSpinnerBuilder<T> builder(SpinnerListModel spinnerModel, Value<T> linkedValue) {
    return new DefaultItemSpinnerBuilder<>(spinnerModel, requireNonNull(linkedValue));
  }
}
