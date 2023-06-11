/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.control.Controls;

import javax.swing.JToolBar;

import static java.util.Objects.requireNonNull;

/**
 * A builder for a {@link JToolBar}.
 */
public interface ToolBarBuilder extends ControlPanelBuilder<JToolBar, ToolBarBuilder> {

  /**
   * @param floatable true if the toolbar should be floatable
   * @return this builder instance
   */
  ToolBarBuilder floatable(boolean floatable);

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
   * @return a new {@link ToolBarBuilder}
   */
  static ToolBarBuilder builder() {
    return new DefaultToolBarBuilder(null);
  }

  /**
   * @param controls the controls
   * @return a new {@link ToolBarBuilder}
   */
  static ToolBarBuilder builder(Controls controls) {
    return new DefaultToolBarBuilder(requireNonNull(controls));
  }

  /**
   * @param controlsBuilder the controls builder
   * @return a new {@link ToolBarBuilder}
   */
  static ToolBarBuilder builder(Controls.Builder controlsBuilder) {
    return new DefaultToolBarBuilder(requireNonNull(controlsBuilder).build());
  }
}
