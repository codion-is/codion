/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.Controls;

import javax.swing.Action;
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
}
