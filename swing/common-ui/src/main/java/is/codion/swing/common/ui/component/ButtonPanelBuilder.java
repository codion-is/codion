/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.control.Controls;

import javax.swing.JPanel;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JPanel with buttons.
 */
public interface ButtonPanelBuilder extends ControlPanelBuilder<JPanel, ButtonPanelBuilder> {

  /**
   * @return a new button panel builder
   */
  static ButtonPanelBuilder builder() {
    return new DefaultButtonPanelBuilder(null);
  }

  /**
   * @param controls the controls
   * @return a new button panel builder
   */
  static ButtonPanelBuilder builder(Controls controls) {
    return new DefaultButtonPanelBuilder(requireNonNull(controls));
  }
}
