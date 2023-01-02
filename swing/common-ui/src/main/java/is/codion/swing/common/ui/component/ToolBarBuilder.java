/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.Action;
import javax.swing.JToolBar;

/**
 * A builder for a {@link JToolBar}.
 */
public interface ToolBarBuilder extends ComponentBuilder<Void, JToolBar, ToolBarBuilder> {

  /**
   * @param floatable true if the toolbar should be floatable
   * @return this builder instance
   */
  ToolBarBuilder floatable(boolean floatable);

  /**
   * @param orientation the orientation
   * @return this builder instance
   */
  ToolBarBuilder orientation(int orientation);

  /**
   * @param rollover true if rollover should be enabled
   * @return this builder instance
   */
  ToolBarBuilder rollover(boolean rollover);

  /**
   * @param borderPainted true if the border should be painted
   * @return this builder instance
   */
  ToolBarBuilder borderPainted(boolean borderPainted);

  /**
   * @param action the action to add
   * @return this builder instance
   */
  ToolBarBuilder action(Action action);

  /**
   * Adds a separator
   * @return this builder instance
   */
  ToolBarBuilder separator();
}
