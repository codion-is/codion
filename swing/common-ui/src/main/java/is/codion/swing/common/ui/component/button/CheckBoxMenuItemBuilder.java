/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;

import javax.swing.JCheckBoxMenuItem;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JCheckBoxMenuItem.
 */
public interface CheckBoxMenuItemBuilder<B extends CheckBoxMenuItemBuilder<B>> extends ToggleMenuItemBuilder<JCheckBoxMenuItem, B> {

  /**
   * @param <B> the builder type
   * @return a builder for a JCheckBoxMenuItem
   */
  static <B extends CheckBoxMenuItemBuilder<B>> CheckBoxMenuItemBuilder<B> builder() {
    return new DefaultCheckBoxMenuItemBuilder<>(null);
  }

  /**
   * @param <B> the builder type
   * @param linkedValue the value to link to the menu item
   * @return a builder for a JCheckBoxMenuItem
   */
  static <B extends CheckBoxMenuItemBuilder<B>> CheckBoxMenuItemBuilder<B> builder(Value<Boolean> linkedValue) {
    return new DefaultCheckBoxMenuItemBuilder<>(requireNonNull(linkedValue));
  }
}
