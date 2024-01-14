/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.GridLayout;

final class DefaultButtonPanelBuilder extends AbstractControlPanelBuilder<JPanel, ButtonPanelBuilder>
        implements ButtonPanelBuilder {

  private int buttonGap = Layouts.GAP.get();

  DefaultButtonPanelBuilder(Action... actions) {
    this(Controls.controls(actions));
  }

  DefaultButtonPanelBuilder(Controls controls) {
    super(controls);
  }

  @Override
  public ButtonPanelBuilder buttonGap(int buttonGap) {
    this.buttonGap = buttonGap;
    return this;
  }

  @Override
  protected JPanel createComponent() {
    JPanel panel = createPanel();
    new ButtonControlHandler(panel, controls(), buttonBuilder(), toggleButtonBuilder());

    return panel;
  }

  private JPanel createPanel() {
    return new JPanel(orientation() == SwingConstants.HORIZONTAL ?
            new GridLayout(1, 0, buttonGap, 0) :
            new GridLayout(0, 1, 0, buttonGap));
  }

  private final class ButtonControlHandler extends ControlHandler {

    private final JPanel panel;
    private final ButtonBuilder<?, ?, ?> buttonBuilder;
    private final ToggleButtonBuilder<?, ?> toggleButtonBuilder;

    private ButtonControlHandler(JPanel panel, Controls controls,
                                 ButtonBuilder<?, ?, ?> buttonBuilder,
                                 ToggleButtonBuilder<?, ?> toggleButtonBuilder) {
      this.panel = panel;
      this.buttonBuilder = buttonBuilder.clear();
      this.toggleButtonBuilder = toggleButtonBuilder.clear();
      controls.actions().forEach(this);
    }

    @Override
    void onSeparator() {
      panel.add(new JLabel());
    }

    @Override
    void onControl(Control control) {
      onAction(control);
    }

    @Override
    void onToggleControl(ToggleControl toggleControl) {
      panel.add(toggleButtonBuilder.toggleControl(toggleControl).build());
      toggleButtonBuilder.clear();
    }

    @Override
    void onControls(Controls controls) {
      JPanel controlPanel = createPanel();
      new ButtonControlHandler(controlPanel, controls, buttonBuilder, toggleButtonBuilder);
      panel.add(controlPanel);
    }

    @Override
    void onAction(Action action) {
      panel.add(buttonBuilder.action(action).build());
      buttonBuilder.clear();
    }
  }
}
