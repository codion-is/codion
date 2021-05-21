/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.item.Item;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.combobox.SteppedComboBox;

import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * Builds a value list combo box.
 * @param <T> the value type
 */
public interface ValueListComboBoxBuilder<T> extends ComponentBuilder<T, SteppedComboBox<Item<T>>> {

  @Override
  ValueListComboBoxBuilder<T> preferredHeight(int preferredHeight);

  @Override
  ValueListComboBoxBuilder<T> preferredWidth(int preferredWidth);

  @Override
  ValueListComboBoxBuilder<T> preferredSize(Dimension preferredSize);

  @Override
  ValueListComboBoxBuilder<T> transferFocusOnEnter(boolean transferFocusOnEnter);

  @Override
  ValueListComboBoxBuilder<T> enabledState(StateObserver enabledState);

  @Override
  ValueListComboBoxBuilder<T> onBuild(Consumer<SteppedComboBox<Item<T>>> onBuild);

  /**
   * @return this builder instance
   */
  ValueListComboBoxBuilder<T> sorted(boolean sorted);
}
