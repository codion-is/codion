/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.value.UpdateOn;

import javax.swing.JTextArea;
import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * Builds a JTextArea.
 */
public interface TextAreaBuilder extends ComponentBuilder<String, JTextArea> {

  @Override
  TextAreaBuilder preferredHeight(int preferredHeight);

  @Override
  TextAreaBuilder preferredWidth(int preferredWidth);

  @Override
  TextAreaBuilder preferredSize(Dimension preferredSize);

  @Override
  TextAreaBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

  @Override
  TextAreaBuilder enabledState(StateObserver enabledState);

  @Override
  TextAreaBuilder onBuild(Consumer<JTextArea> onBuild);

  /**
   * @param updateOn specifies when the underlying value should be updated
   * @return this builder instance
   */
  TextAreaBuilder updateOn(UpdateOn updateOn);

  /**
   * @param rows the number of rows in the text area
   * @return this builder instance
   */
  TextAreaBuilder rows(int rows);

  /**
   * @param columns the number of colums in the text area
   * @return this builder instance
   */
  TextAreaBuilder columns(int columns);
}
