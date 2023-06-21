/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
    boolean defaultButtonBuilder = buttonBuilder() == null;
    boolean defaultToggleButtonBuilder = toggleButtonBuilder() == null;

    new ToolBarControlHandler(toolBar, controls(),
            defaultButtonBuilder ? ButtonBuilder.builder() : buttonBuilder(), defaultButtonBuilder,
            defaultToggleButtonBuilder ? createToggleButtonBuilder() : toggleButtonBuilder(), defaultToggleButtonBuilder);

    return toolBar;
  }

  private static final class ToolBarControlHandler extends ControlHandler {

    private final JToolBar toolBar;
    private final ButtonBuilder<?, ?, ?> buttonBuilder;
    private final boolean defaultButtonBuilder;
    private final ToggleButtonBuilder<?, ?> toggleButtonBuilder;
    private final boolean defaultToggleButtonBuilder;

    private ToolBarControlHandler(JToolBar toolBar, Controls controls,
                                  ButtonBuilder<?, ?, ?> buttonBuilder,
                                  boolean defaultButtonBuilder,
                                  ToggleButtonBuilder<?, ?> toggleButtonBuilder,
                                  boolean defaultToggleButtonBuilder) {
      this.toolBar = toolBar;
      this.buttonBuilder = buttonBuilder.clear();
      this.defaultButtonBuilder = defaultButtonBuilder;
      this.toggleButtonBuilder = toggleButtonBuilder.clear();
      this.defaultToggleButtonBuilder = defaultToggleButtonBuilder;
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
      if (defaultToggleButtonBuilder) {
        buttonBuilder.includeText(includeButtonText(toggleControl));
      }
      toolBar.add(toggleButtonBuilder
              .toggleControl(toggleControl)
              .build());
      toggleButtonBuilder.clear();
    }

    @Override
    void onControls(Controls controls) {
      if (!controls.isEmpty()) {
        new ToolBarControlHandler(toolBar, controls, buttonBuilder, defaultButtonBuilder, toggleButtonBuilder, defaultToggleButtonBuilder);
      }
    }

    @Override
    void onAction(Action action) {
      if (defaultButtonBuilder) {
        buttonBuilder.includeText(includeButtonText(action));
      }
      toolBar.add(buttonBuilder
              .action(action)
              .build());
      buttonBuilder.clear();
    }

    private static boolean includeButtonText(Action action) {
      return action.getValue(Action.SMALL_ICON) == null && action.getValue(Action.LARGE_ICON_KEY) == null;
    }
  }
}