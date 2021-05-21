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
 * Builds a boolean combo box.
 */
public interface BooleanComboBoxBuilder extends ComponentBuilder<Boolean, SteppedComboBox<Item<Boolean>>> {

  @Override
  BooleanComboBoxBuilder preferredHeight(int preferredHeight);

  @Override
  BooleanComboBoxBuilder preferredWidth(int preferredWidth);

  @Override
  BooleanComboBoxBuilder preferredSize(Dimension preferredSize);

  @Override
  BooleanComboBoxBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

  @Override
  BooleanComboBoxBuilder enabledState(StateObserver enabledState);

  @Override
  BooleanComboBoxBuilder onBuild(Consumer<SteppedComboBox<Item<Boolean>>> onBuild);
}
