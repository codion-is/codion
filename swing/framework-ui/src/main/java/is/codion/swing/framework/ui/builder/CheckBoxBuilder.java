/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.checkbox.NullableCheckBox;

import javax.swing.JCheckBox;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * Builds a JCheckBox.
 */
public interface CheckBoxBuilder extends ComponentBuilder<Boolean, JCheckBox> {

  @Override
  CheckBoxBuilder preferredHeight(int preferredHeight);

  @Override
  CheckBoxBuilder preferredWidth(int preferredWidth);

  @Override
  CheckBoxBuilder preferredSize(Dimension preferredSize);

  @Override
  CheckBoxBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

  @Override
  CheckBoxBuilder enabledState(StateObserver enabledState);

  @Override
  CheckBoxBuilder onBuild(Consumer<JCheckBox> onBuild);

  /**
   * @return this builder instance
   */
  CheckBoxBuilder includeCaption(boolean includeCaption);

  /**
   * @param nullable if true then a {@link NullableCheckBox} is built.
   * @return this builder instance
   */
  CheckBoxBuilder nullable(boolean nullable);
}
