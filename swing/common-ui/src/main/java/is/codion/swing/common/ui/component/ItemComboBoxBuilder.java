/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.swing.common.ui.combobox.SteppedComboBox;

/**
 * Builds a item combo box.
 * @param <T> the value type
 */
public interface ItemComboBoxBuilder<T> extends ComponentBuilder<T, SteppedComboBox<Item<T>>, ItemComboBoxBuilder<T>> {

  /**
   * @param nullable true if a null value should be added to the model if missing
   * @return this builder instance
   */
  ItemComboBoxBuilder<T> nullable(boolean nullable);

  /**
   * @param popupWidth the required popup with
   * @return this builder instance
   */
  ItemComboBoxBuilder<T> popupWidth(final int popupWidth);

  /**
   * @param sorted true if the combo box content should be sorted
   * @return this builder instance
   */
  ItemComboBoxBuilder<T> sorted(boolean sorted);
}
