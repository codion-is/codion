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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.Action;
import javax.swing.JToolBar;

final class DefaultToolBarBuilder extends AbstractControlPanelBuilder<JToolBar, ToolBarBuilder> implements ToolBarBuilder {

  private boolean floatable = true;
  private boolean rollover = false;
  private boolean borderPainted = true;

  DefaultToolBarBuilder(Controls controls) {
    super(controls);
    includeButtonText(false);
  }

  @Override
  public ToolBarBuilder floatable(boolean floatable) {
    this.floatable = floatable;
    return this;
  }

  @Override
  public ToolBarBuilder rollover(boolean rollover) {
    this.rollover = rollover;
    return this;
  }

  @Override
  public ToolBarBuilder borderPainted(boolean borderPainted) {
    this.borderPainted = borderPainted;
    return this;
  }

  @Override
  protected JToolBar createComponent() {
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(floatable);
    toolBar.setOrientation(orientation());
    toolBar.setRollover(rollover);
    toolBar.setBorderPainted(borderPainted);

    new ToolBarControlHandler(toolBar, controls(), buttonBuilder(), toggleButtonBuilder());

    return toolBar;
  }

  private static final class ToolBarControlHandler extends ControlHandler {

    private final JToolBar toolBar;
    private final ButtonBuilder<?, ?, ?> buttonBuilder;
    private final ToggleButtonBuilder<?, ?> toggleButtonBuilder;

    private ToolBarControlHandler(JToolBar toolBar, Controls controls,
                                  ButtonBuilder<?, ?, ?> buttonBuilder,
                                  ToggleButtonBuilder<?, ?> toggleButtonBuilder) {
      this.toolBar = toolBar;
      this.buttonBuilder = buttonBuilder.clear();
      this.toggleButtonBuilder = toggleButtonBuilder.clear();
      controls.actions().forEach(this);
    }

    @Override
    void onSeparator() {
      toolBar.addSeparator();
    }

    @Override
    void onControl(Control control) {
      onAction(control);
    }

    @Override
    void onToggleControl(ToggleControl toggleControl) {
      toolBar.add(toggleButtonBuilder
              .toggleControl(toggleControl)
              .build());
      toggleButtonBuilder.clear();
    }

    @Override
    void onControls(Controls controls) {
      new ToolBarControlHandler(toolBar, controls, buttonBuilder, toggleButtonBuilder);
    }

    @Override
    void onAction(Action action) {
      toolBar.add(buttonBuilder
              .action(action)
              .build());
      buttonBuilder.clear();
    }
  }
}
