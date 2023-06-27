/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import static java.util.Objects.requireNonNull;

final class DefaultButtonPanelBuilder extends AbstractControlPanelBuilder<JPanel, ButtonPanelBuilder>
        implements ButtonPanelBuilder {

  private boolean buttonsFocusable = true;
  private Dimension preferredButtonSize;
  private int buttonGap = Layouts.HORIZONTAL_VERTICAL_GAP.get();

  DefaultButtonPanelBuilder(Action... actions) {
    this(Controls.controls(actions));
  }

  DefaultButtonPanelBuilder(Controls controls) {
    super(controls);
  }

  @Override
  public ButtonPanelBuilder buttonsFocusable(boolean buttonsFocusable) {
    this.buttonsFocusable = buttonsFocusable;
    return this;
  }

  @Override
  public ButtonPanelBuilder preferredButtonSize(Dimension preferredButtonSize) {
    this.preferredButtonSize = requireNonNull(preferredButtonSize);
    return this;
  }

  @Override
  public ButtonPanelBuilder buttonGap(int buttonGap) {
    this.buttonGap = buttonGap;
    return this;
  }

  @Override
  protected JPanel createComponent() {
    JPanel panel = createPanel();
    new ButtonControlHandler(panel, controls(), getButtonBuilder(), getToggleButtonBuilder());

    return panel;
  }

  private JPanel createPanel() {
    return new JPanel(orientation() == SwingConstants.HORIZONTAL ?
            new GridLayout(1, 0, buttonGap, 0) :
            new GridLayout(0, 1, 0, buttonGap));
  }

  private ToggleButtonBuilder<?, ?> getToggleButtonBuilder() {
    return toggleButtonBuilder().orElse(createToggleButtonBuilder()
            .focusable(buttonsFocusable)
            .preferredSize(preferredButtonSize));
  }

  private ButtonBuilder<?, ?, ?> getButtonBuilder() {
    return buttonBuilder().orElse(ButtonBuilder.builder()
            .focusable(buttonsFocusable)
            .preferredSize(preferredButtonSize));
  }

  static JPanel createEastButtonPanel(JComponent centerComponent, boolean buttonFocusable, Action... buttonActions) {
    requireNonNull(centerComponent, "centerComponent");
    requireNonNull(buttonActions, "buttonActions");

    ButtonPanelBuilder buttonPanelBuilder = new DefaultButtonPanelBuilder(buttonActions)
            .buttonsFocusable(buttonFocusable)
            .preferredButtonSize(new Dimension(centerComponent.getPreferredSize().height, centerComponent.getPreferredSize().height))
            .buttonGap(0);

    return Components.panel(new BorderLayout())
            .add(centerComponent, BorderLayout.CENTER)
            .add(buttonPanelBuilder.build(), BorderLayout.EAST)
            .build();
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
