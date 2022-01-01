/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;

import java.util.Comparator;

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
  ItemComboBoxBuilder<T> popupWidth(int popupWidth);

  /**
   * Sorts the contents by caption
   * @param sorted if true then the items will be sorted by caption
   * @return this builder instance
   */
  ItemComboBoxBuilder<T> sorted(boolean sorted);

  /**
   * @param sortComparator if specified the combo box contents are sorted using this comparator
   * @return this builder instance
   */
  ItemComboBoxBuilder<T> sortComparator(Comparator<Item<T>> sortComparator);

  /**
   * @param completionMode the completion mode
   * @return this builder instance
   */
  ItemComboBoxBuilder<T> completionMode(Completion.Mode completionMode);
}
