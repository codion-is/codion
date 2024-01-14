/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.Action;
import javax.swing.JComponent;
import java.awt.Dimension;
import java.util.function.Consumer;

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
   * @param includeButtonText true if buttons should include text
   * @return this builder instance
   */
  B includeButtonText(boolean includeButtonText);

  /**
   * @param preferredButtonSize the preferred button size
   * @return this builder instance
   */
  B preferredButtonSize(Dimension preferredButtonSize);

  /**
   * @param buttonsFocusable whether the buttons should be focusable, default is {@code true}
   * @return this builder instance
   */
  B buttonsFocusable(boolean buttonsFocusable);

  /**
   * Specifies how toggle controls are presented on this control panel.
   * The default is {@link ToggleButtonType#BUTTON}.
   * @param toggleButtonType the toggle button type
   * @return this builder instance
   */
  B toggleButtonType(ToggleButtonType toggleButtonType);

  /**
   * Provides a way to configure the {@link ButtonBuilder} used by this {@link ControlPanelBuilder}.
   * @param buttonBuilder provides the button builder used to create buttons
   * @return this builder instance
   */
  B buttonBuilder(Consumer<ButtonBuilder<?, ?, ?>> buttonBuilder);

  /**
   * Provides a way to configure the {@link ToggleButtonBuilder} used by this {@link ControlPanelBuilder}.
   * @param toggleButtonBuilder provides the toggle button builder used to create toggle buttons
   * @return this builder instance
   */
  B toggleButtonBuilder(Consumer<ToggleButtonBuilder<?, ?>> toggleButtonBuilder);

  /**
   * Provides a way to configure the {@link CheckBoxBuilder} used by this {@link ControlPanelBuilder}.
   * @param checkBoxBuilder provides the toggle button builder used to create check boxes
   * @return this builder instance
   */
  B checkBoxBuilder(Consumer<CheckBoxBuilder> checkBoxBuilder);

  /**
   * Provides a way to configure the {@link RadioButtonBuilder} used by this {@link ControlPanelBuilder}.
   * @param radioButtonBuilder provides the toggle button builder used to create radio buttons
   * @return this builder instance
   */
  B radioButtonBuilder(Consumer<RadioButtonBuilder> radioButtonBuilder);
}
