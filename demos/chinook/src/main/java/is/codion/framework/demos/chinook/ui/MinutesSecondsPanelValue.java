/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
    this(false);
  }

  MinutesSecondsPanelValue(boolean horizontal) {
    super(new MinutesSecondsPanel(horizontal));
    getComponent().minutesField.addValueListener(minutes -> notifyValueChange());
    getComponent().secondsField.addValueListener(seconds -> notifyValueChange());
  }

  @Override
  protected Integer getComponentValue(MinutesSecondsPanel component) {
    return getMilliseconds(component.minutesField.getInteger(), component.secondsField.getInteger());
  }

  @Override
  protected void setComponentValue(MinutesSecondsPanel component, Integer milliseconds) {
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

    private MinutesSecondsPanel(boolean horizontal) {
      super(borderLayout());
      if (horizontal) {
        Components.panel(gridLayout(1, 4))
                .add(new JLabel(BUNDLE.getString("min")))
                .add(minutesField)
                .add(new JLabel(BUNDLE.getString("sec")))
                .add(secondsField)
                .build(panel -> add(panel, BorderLayout.CENTER));
      }
      else {
        Components.panel(gridLayout(1, 2))
                .add(new JLabel(BUNDLE.getString("min")))
                .add(new JLabel(BUNDLE.getString("sec")))
                .build(panel -> add(panel, BorderLayout.NORTH));
        Components.panel(gridLayout(1, 2))
                .add(minutesField)
                .add(secondsField)
                .build(panel -> add(panel, BorderLayout.CENTER));
      }
    }
  }
}
