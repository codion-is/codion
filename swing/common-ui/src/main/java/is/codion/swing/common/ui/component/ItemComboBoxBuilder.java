/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.item.Item;
import is.codion.swing.common.ui.combobox.Completion;

import javax.swing.JComboBox;
import java.util.Comparator;

/**
 * Builds a item combo box.
 * @param <T> the value type
 */
public interface ItemComboBoxBuilder<T> extends ComponentBuilder<T, JComboBox<Item<T>>, ItemComboBoxBuilder<T>> {

  /**
   * @param nullable true if a null value should be added to the model if missing
   * @return this builder instance
   */
  ItemComboBoxBuilder<T> nullable(boolean nullable);

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

  /**
   * Enable mouse wheel scrolling on the combo box
   * @param mouseWheelScrolling true if mouse wheel scrolling should be enabled
   * @return this builder instance
   */
  ItemComboBoxBuilder<T> mouseWheelScrolling(boolean mouseWheelScrolling);

  /**
   * Enable mouse wheel scrolling on the combo box, with wrap around
   * @param mouseWheelScrollingWithWrapAround true if mouse wheel scrolling with wrap around should be enabled
   * @return this builder instance
   */
  ItemComboBoxBuilder<T> mouseWheelScrollingWithWrapAround(boolean mouseWheelScrollingWithWrapAround);

  /**
   * @param maximumRowCount the maximum row count
   * @return this builder instance
   */
  ItemComboBoxBuilder<T> maximumRowCount(int maximumRowCount);
}
