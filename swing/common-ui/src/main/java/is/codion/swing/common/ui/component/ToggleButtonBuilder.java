/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JToggleButton;

/**
 * Builds a JToggleButton.
 */
public interface ToggleButtonBuilder extends ComponentBuilder<Boolean, JToggleButton, ToggleButtonBuilder> {

  /**
   * @param caption the caption
   * @return this builder instance
   */
  ToggleButtonBuilder caption(String caption);

  /**
   * @param includeCaption specifies whether a caption should be included
   * @return this builder instance
   */
  ToggleButtonBuilder includeCaption(boolean includeCaption);

  /**
   * @return a new toggle button
   */
  JToggleButton build();
}
