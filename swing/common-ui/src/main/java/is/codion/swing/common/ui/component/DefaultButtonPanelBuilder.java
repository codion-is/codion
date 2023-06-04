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

final class DefaultButtonPanelBuilder extends AbstractControlPanelBuilder<JPanel, ButtonPanelBuilder>
        implements ButtonPanelBuilder {

  DefaultButtonPanelBuilder(Controls controls) {
    super(controls);
  }

  @Override
  protected JPanel createComponent() {
    JPanel panel = createPanel();
    controls().actions().forEach(new ButtonControlHandler(panel));

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

    private ButtonControlHandler(JPanel panel) {
      this.panel = panel;
    }

    @Override
    public void onSeparator() {
      panel.add(new JLabel());
    }

    @Override
    public void onControl(Control control) {
      if (control instanceof ToggleControl) {
        ToggleControl toggleControl = (ToggleControl) control;
        panel.add(toggleButtonType() == ToggleButtonType.CHECKBOX ?
                toggleControl.createCheckBox() :
                toggleControl.createToggleButton());
      }
      else {
        onAction(control);
      }
    }

    @Override
    public void onControls(Controls controls) {
      JPanel controlPanel = createPanel();
      controls.actions().forEach(new ButtonControlHandler(controlPanel));
      panel.add(controlPanel);
    }

    @Override
    public void onAction(Action action) {
      panel.add(new JButton(action));
    }
  }
}
