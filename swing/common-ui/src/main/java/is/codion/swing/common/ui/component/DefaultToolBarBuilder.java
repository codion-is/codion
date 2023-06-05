/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.button.ToggleButtonType;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.Action;
import javax.swing.JToolBar;

import static is.codion.swing.common.ui.component.Components.checkBox;
import static is.codion.swing.common.ui.component.Components.toggleButton;

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
    controls().actions().forEach(new ToolBarControlHandler(toolBar));

    return toolBar;
  }

  private final class ToolBarControlHandler extends ControlHandler {

    private final JToolBar toolBar;

    private ToolBarControlHandler(JToolBar toolBar) {
      this.toolBar = toolBar;
    }

    @Override
    public void onSeparator() {
      toolBar.addSeparator();
    }

    @Override
    public void onControl(Control control) {
      if (control instanceof ToggleControl) {
        ToggleControl toggleControl = (ToggleControl) control;
        toolBar.add(toggleButtonType() == ToggleButtonType.CHECKBOX ?
                checkBox(toggleControl).build() :
                toggleButton(toggleControl).build());
      }
      else {
        toolBar.add(control);
      }
    }

    @Override
    public void onControls(Controls controls) {
      controls.actions().forEach(new ToolBarControlHandler(toolBar));
    }

    @Override
    public void onAction(Action action) {
      toolBar.add(action);
    }
  }
}
