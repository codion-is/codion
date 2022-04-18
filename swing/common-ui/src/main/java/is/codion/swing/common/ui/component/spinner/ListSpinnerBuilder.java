/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.common.value.Value;

import javax.swing.SpinnerListModel;

import static java.util.Objects.requireNonNull;

/**
 * A builder for JSpinner based on a list of values.
 */
public interface ListSpinnerBuilder<T> extends SpinnerBuilder<T, ListSpinnerBuilder<T>> {

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @return a builder for a JSpinner
   */
  static <T> ListSpinnerBuilder<T> builder(SpinnerListModel spinnerModel) {
    return new DefaultListSpinnerBuilder<>(spinnerModel, null);
  }

  /**
   * @param <T> the value type
   * @param spinnerModel the spinner model
   * @param linkedValue the value to link to the component
   * @return a builder for a JSpinner
   */
  static <T> ListSpinnerBuilder<T> builder(SpinnerListModel spinnerModel, Value<T> linkedValue) {
    return new DefaultListSpinnerBuilder<>(spinnerModel, requireNonNull(linkedValue));
  }
}
