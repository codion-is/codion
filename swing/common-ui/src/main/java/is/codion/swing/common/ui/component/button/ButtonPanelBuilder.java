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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.Controls;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Dimension;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JPanel with buttons.
 */
public interface ButtonPanelBuilder extends ControlPanelBuilder<JPanel, ButtonPanelBuilder> {

  /**
   * Overridden by {@link #buttonBuilder(ButtonBuilder)} and {@link #toggleButtonBuilder(ToggleButtonBuilder)}.
   * @param buttonsFocusable whether the buttons should be focusable, default is {@code false}
   * @return this builder instance
   */
  ButtonPanelBuilder buttonsFocusable(boolean buttonsFocusable);

  /**
   * Overridden by {@link #buttonBuilder(ButtonBuilder)} and {@link #toggleButtonBuilder(ToggleButtonBuilder)}.
   * @param preferredButtonSize the preferred button size
   * @return this builder instance
   */
  ButtonPanelBuilder preferredButtonSize(Dimension preferredButtonSize);

  /**
   * Default is {@link is.codion.swing.common.ui.layout.Layouts#GAP}.
   * @param buttonGap the gap between buttons in pixels
   * @return this builder instance
   */
  ButtonPanelBuilder buttonGap(int buttonGap);

  /**
   * @return a new button panel builder
   */
  static ButtonPanelBuilder builder() {
    return new DefaultButtonPanelBuilder((Controls) null);
  }

  /**
   * @param actions the actions
   * @return a new button panel builder
   */
  static ButtonPanelBuilder builder(Action... actions) {
    return new DefaultButtonPanelBuilder(requireNonNull(actions));
  }

  /**
   * @param controls the controls
   * @return a new button panel builder
   */
  static ButtonPanelBuilder builder(Controls controls) {
    return new DefaultButtonPanelBuilder(requireNonNull(controls));
  }

  /**
   * @param controlsBuilder the controls builder
   * @return a new button panel builder
   */
  static ButtonPanelBuilder builder(Controls.Builder controlsBuilder) {
    return builder(requireNonNull(controlsBuilder).build());
  }

  /**
   * Creates a panel with {@code centerComponent} in the BorderLayout.CENTER position and non-focusable buttons based on buttonActions
   * in the BorderLayout.EAST position, with the buttons preferred size based on the preferred height of {@code centerComponent}.
   * @param centerComponent the center component
   * @param buttonActions the button actions
   * @return a panel
   * @see #createEastFocusableButtonPanel(JComponent, Action...)
   */
  static JPanel createEastButtonPanel(JComponent centerComponent, Action... buttonActions) {
    return DefaultButtonPanelBuilder.createEastButtonPanel(centerComponent, false, buttonActions);
  }

  /**
   * Creates a panel with {@code centerComponent} in the BorderLayout.CENTER position and focusable buttons based on buttonActions
   * in the BorderLayout.EAST position, with the buttons preferred size based on the preferred height of {@code centerComponent}.
   * @param centerComponent the center component
   * @param buttonActions the button actions
   * @return a panel
   */
  static JPanel createEastFocusableButtonPanel(JComponent centerComponent, Action... buttonActions) {
    return DefaultButtonPanelBuilder.createEastButtonPanel(centerComponent, true, buttonActions);
  }
}
