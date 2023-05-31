/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.button.ToggleButtonType;
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
   * Specifies how toggle controls are presented on this tool bar.
   * The default is {@link ToggleButtonType#BUTTON}.
   * @param toggleButtonType the toggle button type
   * @return this builder instance
   */
  B toggleButtonType(ToggleButtonType toggleButtonType);
}
