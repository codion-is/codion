/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.JCheckBoxMenuItem;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JCheckBoxMenuItem.
 */
public interface CheckBoxMenuItemBuilder<B extends CheckBoxMenuItemBuilder<B>> extends ToggleMenuItemBuilder<JCheckBoxMenuItem, B> {

  /**
   * @param <B> the builder type
   * @param toggleControl the button action
   * @return a builder for a JButton
   */
  static <B extends CheckBoxMenuItemBuilder<B>> CheckBoxMenuItemBuilder<B> builder(ToggleControl toggleControl) {
    return new DefaultCheckBoxMenuItemBuilder<>(requireNonNull(toggleControl), null);
  }

  /**
   * @param <B> the builder type
   * @param toggleControl the button action
   * @param linkedValue the value to link to the menu item
   * @return a builder for a JButton
   */
  static <B extends CheckBoxMenuItemBuilder<B>> CheckBoxMenuItemBuilder<B> builder(ToggleControl toggleControl, Value<Boolean> linkedValue) {
    return new DefaultCheckBoxMenuItemBuilder<>(requireNonNull(toggleControl), requireNonNull(linkedValue));
  }
}
