/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.common.ui.component.AbstractComponentValue;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.textfield.IntegerField;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ResourceBundle;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

final class MinutesSecondsPanelValue extends AbstractComponentValue<Integer, MinutesSecondsPanelValue.MinutesSecondsPanel> {

  MinutesSecondsPanelValue() {
    super(new MinutesSecondsPanel());
    getComponent().minutesField.addValueListener(minutes -> notifyValueChange());
    getComponent().secondsField.addValueListener(seconds -> notifyValueChange());
  }

  @Override
  protected Integer getComponentValue(final MinutesSecondsPanel component) {
    return getMilliseconds(component.minutesField.getInteger(), component.secondsField.getInteger());
  }

  @Override
  protected void setComponentValue(final MinutesSecondsPanel component, final Integer milliseconds) {
    component.minutesField.setInteger(getMinutes(milliseconds));
    component.secondsField.setInteger(getSeconds(milliseconds));
  }

  static final class MinutesSecondsPanel extends JPanel {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(MinutesSecondsPanel.class.getName());

    private final IntegerField minutesField = Components.integerField()
            .transferFocusOnEnter(true)
            .selectAllOnFocusGained(true)
            .columns(2)
            .build();
    private final IntegerField secondsField = Components.integerField()
            .range(0, 59)
            .transferFocusOnEnter(true)
            .selectAllOnFocusGained(true)
            .columns(2)
            .build();

    private MinutesSecondsPanel() {
      super(borderLayout());
      final JPanel northPanel = new JPanel(gridLayout(1, 2));
      northPanel.add(new JLabel(BUNDLE.getString("min")));
      northPanel.add(new JLabel(BUNDLE.getString("sec")));
      final JPanel centerPanel = new JPanel(gridLayout(1, 2));
      centerPanel.add(minutesField);
      centerPanel.add(secondsField);
      add(northPanel, BorderLayout.NORTH);
      add(centerPanel, BorderLayout.CENTER);
    }
  }
}
