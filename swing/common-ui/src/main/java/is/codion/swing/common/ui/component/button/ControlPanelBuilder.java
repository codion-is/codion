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

import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.Action;
import javax.swing.JComponent;

/**
 * Builds panels with controls.
 */
public interface ControlPanelBuilder<C extends JComponent, B extends ControlPanelBuilder<C, B>>
        extends ComponentBuilder<Void, C, B> {

  /**
   * @param orientation the panel orientation, default {@link javax.swing.SwingConstants#HORIZONTAL}
   * @return this builder instance
   */
  B orientation(int orientation);

  /**
   * @param action the action to add
   * @return this builder instance
   */
  B action(Action action);

  /**
   * Adds all actions from the given {@link Controls} instance
   * @param controls the Controls instance
   * @return this builder instance
   */
  B controls(Controls controls);

  /**
   * Adds a separator
   * @return this builder instance
   */
  B separator();

  /**
   * Specifies how toggle controls are presented on this control panel.
   * The default is {@link ToggleButtonType#BUTTON}.
   * @param toggleButtonType the toggle button type
   * @return this builder instance
   */
  B toggleButtonType(ToggleButtonType toggleButtonType);

  /**
   * @param buttonBuilder the button builder to use when creating buttons
   * @return this builder instance
   */
  B buttonBuilder(ButtonBuilder<?, ?, ?> buttonBuilder);

  /**
   * Overrides {@link #toggleButtonType(ToggleButtonType)}.
   * @param toggleButtonBuilder the toggle button builder to use when creating toggle buttons
   * @return this builder instance
   */
  B toggleButtonBuilder(ToggleButtonBuilder<?, ?> toggleButtonBuilder);
}
