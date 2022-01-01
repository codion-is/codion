/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JToggleButton;

/**
 * Builds a ToggleButton.
 */
public interface ToggleButtonBuilder<C extends JToggleButton, B extends ToggleButtonBuilder<C, B>> extends ButtonBuilder<Boolean, C, B> {

  /**
   * @return a new toggle button
   */
  C build();
}
