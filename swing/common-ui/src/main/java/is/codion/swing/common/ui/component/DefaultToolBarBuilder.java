/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.Action;
import javax.swing.JToolBar;

import static is.codion.swing.common.ui.component.DefaultButtonPanelBuilder.createToggleButton;

final class DefaultToolBarBuilder extends AbstractControlPanelBuilder<JToolBar, ToolBarBuilder> implements ToolBarBuilder {

  private boolean floatable = true;
  private boolean rollover = false;
  private boolean borderPainted = true;

  DefaultToolBarBuilder(Controls controls) {
    super(controls);
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
    new ToolBarControlHandler(toolBar, controls());

    return toolBar;
  }

  private final class ToolBarControlHandler extends ControlHandler {

    private final JToolBar toolBar;

    private ToolBarControlHandler(JToolBar toolBar, Controls controls) {
      this.toolBar = toolBar;
      controls.actions().forEach(this);
    }

    @Override
    void onSeparator() {
      toolBar.addSeparator();
    }

    @Override
    void onControl(Control control) {
      toolBar.add(control);
    }

    @Override
    void onToggleControl(ToggleControl toggleControl) {
      toolBar.add(createToggleButton(toggleControl, toggleButtonType()));
    }

    @Override
    void onControls(Controls controls) {
      if (!controls.isEmpty()) {
        new ToolBarControlHandler(toolBar, controls);
      }
    }

    @Override
    void onAction(Action action) {
      toolBar.add(action);
    }
  }
}
