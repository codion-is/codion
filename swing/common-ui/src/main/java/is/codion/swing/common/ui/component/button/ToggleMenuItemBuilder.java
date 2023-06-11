/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JMenuItem;

/**
 * Builds a toggle menu item.
 */
public interface ToggleMenuItemBuilder<C extends JMenuItem, B extends ToggleMenuItemBuilder<C, B>> extends ButtonBuilder<Boolean, C, B> {

  /**
   * @param toggleControl the toggle control to base this toggle menu item on
   * @return this builder instance
   */
  B toggleControl(ToggleControl toggleControl);

  /**
   * @param toggleControlBuilder the builder for the toggle control to base this toggle menu on
   * @return this builder instance
   */
  B toggleControl(ToggleControl.Builder toggleControlBuilder);
}
