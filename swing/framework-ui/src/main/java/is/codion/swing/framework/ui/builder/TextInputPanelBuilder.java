/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.value.UpdateOn;

import java.awt.Dimension;
import java.util.function.Consumer;

/**
 * Builds a TextInputPanel.
 */
public interface TextInputPanelBuilder extends ComponentBuilder<String, TextInputPanel> {

  @Override
  TextInputPanelBuilder preferredHeight(int preferredHeight);

  @Override
  TextInputPanelBuilder preferredWidth(int preferredWidth);

  @Override
  TextInputPanelBuilder preferredSize(Dimension preferredSize);

  @Override
  TextInputPanelBuilder transferFocusOnEnter(boolean transferFocusOnEnter);

  @Override
  TextInputPanelBuilder enabledState(StateObserver enabledState);

  @Override
  TextInputPanelBuilder onBuild(Consumer<TextInputPanel> onBuild);

  /**
   * @param updateOn specifies when the underlying value should be updated
   * @return this builder instance
   */
  TextInputPanelBuilder updateOn(UpdateOn updateOn);

  /**
   * @param columns the number of colums in the text field
   * @return this builder instance
   */
  TextInputPanelBuilder columns(int columns);

  /**
   * @return this builder instance
   */
  TextInputPanelBuilder buttonFocusable(boolean buttonFocusable);

  /**
   * @param textAreaSize the input text area size
   * @return this builder instance
   */
  TextInputPanelBuilder textAreaSize(Dimension textAreaSize);
}
