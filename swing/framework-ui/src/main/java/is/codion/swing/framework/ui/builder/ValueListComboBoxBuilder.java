/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.item.Item;
import is.codion.swing.common.ui.combobox.SteppedComboBox;

/**
 * Builds a value list combo box.
 * @param <T> the value type
 */
public interface ValueListComboBoxBuilder<T> extends ComponentBuilder<T, SteppedComboBox<Item<T>>, ValueListComboBoxBuilder<T>> {

  /**
   * @return this builder instance
   */
  ValueListComboBoxBuilder<T> sorted(boolean sorted);
}
