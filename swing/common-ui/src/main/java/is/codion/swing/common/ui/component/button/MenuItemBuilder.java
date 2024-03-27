/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.Control;

import javax.swing.Action;
import javax.swing.JMenuItem;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JMenuItem.
 */
public interface MenuItemBuilder<C extends JMenuItem, B extends MenuItemBuilder<C, B>> extends ButtonBuilder<Void, C, B> {

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
  static <C extends JMenuItem, B extends MenuItemBuilder<C, B>> MenuItemBuilder<C, B> builder(Control.Builder<?, ?> controlBuilder) {
    return new DefaultMenuItemBuilder<>(requireNonNull(controlBuilder).build());
  }
}
