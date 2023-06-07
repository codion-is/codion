/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.button.ToggleButtonType;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.GridLayout;

import static is.codion.swing.common.ui.component.Components.checkBox;
import static is.codion.swing.common.ui.component.Components.toggleButton;

final class DefaultButtonPanelBuilder extends AbstractControlPanelBuilder<JPanel, ButtonPanelBuilder>
        implements ButtonPanelBuilder {

  DefaultButtonPanelBuilder(Controls controls) {
    super(controls);
  }

  @Override
  protected JPanel createComponent() {
    JPanel panel = createPanel();
    new ButtonControlHandler(panel, controls());

    return panel;
  }

  private JPanel createPanel() {
    return addEmptyBorder(new JPanel(orientation() == SwingConstants.HORIZONTAL ?
            new GridLayout(1, 0) : new GridLayout(0, 1)));
  }

  private static JPanel addEmptyBorder(JPanel panel) {
    Integer gap = Layouts.HORIZONTAL_VERTICAL_GAP.get();
    if (gap != null) {
      panel.setBorder(BorderFactory.createEmptyBorder(gap, gap, gap, gap));
    }

    return panel;
  }

  private final class ButtonControlHandler extends ControlHandler {

    private final JPanel panel;

    private ButtonControlHandler(JPanel panel, Controls controls) {
      this.panel = panel;
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
      panel.add(toggleButtonType() == ToggleButtonType.CHECKBOX ?
              checkBox(toggleControl).build() :
              toggleButton(toggleControl).build());
    }

    @Override
    void onControls(Controls controls) {
      if (!controls.isEmpty()) {
        JPanel controlPanel = createPanel();
        new ButtonControlHandler(controlPanel, controls);
        panel.add(controlPanel);
      }
    }

    @Override
    void onAction(Action action) {
      panel.add(new JButton(action));
    }
  }
}
