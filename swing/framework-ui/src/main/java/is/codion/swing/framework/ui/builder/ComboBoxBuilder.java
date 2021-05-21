/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.combobox.SteppedComboBox;

import java.awt.Dimension;
import java.util.function.Consumer;

public interface ComboBoxBuilder<T> extends ComponentBuilder<T, SteppedComboBox<T>> {

  @Override
  ComboBoxBuilder<T> preferredHeight(int preferredHeight);

  @Override
  ComboBoxBuilder<T> preferredWidth(int preferredWidth);

  @Override
  ComboBoxBuilder<T> preferredSize(Dimension preferredSize);

  @Override
  ComboBoxBuilder<T> transferFocusOnEnter(boolean transferFocusOnEnter);

  @Override
  ComboBoxBuilder<T> enabledState(StateObserver enabledState);

  @Override
  ComboBoxBuilder<T> onBuild(Consumer<SteppedComboBox<T>> onBuild);

  /**
   * @return this builder instance
   */
  ComboBoxBuilder<T> editable(boolean editable);
}
