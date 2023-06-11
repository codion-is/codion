/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.Control;

import javax.swing.Action;
import javax.swing.JMenuItem;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JMenuItem.
 */
public interface MenuItemBuilder<C extends JMenuItem, B extends MenuItemBuilder<C, B>> extends ButtonBuilder<Void, C, B>{

  /**
   * @param <B> the builder type
   * @param <C> the component type
   * @return a builder for a JMenuItem
   */
  static <C extends JMenuItem, B extends MenuItemBuilder<C, B>> MenuItemBuilder<C, B> builder() {
    return new DefaultMenuItemBuilder<>(null);
  }

  /**
   * @param <B> the builder type
   * @param <C> the component type
   * @param action the button action
   * @return a builder for a JButton
   */
  static <C extends JMenuItem, B extends MenuItemBuilder<C, B>> MenuItemBuilder<C, B> builder(Action action) {
    return new DefaultMenuItemBuilder<>(requireNonNull(action));
  }

  /**
   * @param <B> the builder type
   * @param <C> the component type
   * @param control the button control
   * @return a builder for a JButton
   */
  static <C extends JMenuItem, B extends MenuItemBuilder<C, B>> MenuItemBuilder<C, B> builder(Control control) {
    return new DefaultMenuItemBuilder<>(requireNonNull(control));
  }

  /**
   * @param <B> the builder type
   * @param <C> the component type
   * @param controlBuilder the button control builder
   * @return a builder for a JButton
   */
  static <C extends JMenuItem, B extends MenuItemBuilder<C, B>> MenuItemBuilder<C, B> builder(Control.Builder controlBuilder) {
    return new DefaultMenuItemBuilder<>(requireNonNull(controlBuilder).build());
  }
}
