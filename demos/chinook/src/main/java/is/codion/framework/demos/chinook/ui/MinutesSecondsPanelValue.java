/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ResourceBundle;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.integerField;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

final class MinutesSecondsPanelValue extends AbstractComponentValue<Integer, MinutesSecondsPanelValue.MinutesSecondsPanel> {

  MinutesSecondsPanelValue() {
    this(false);
  }

  MinutesSecondsPanelValue(boolean horizontal) {
    super(new MinutesSecondsPanel(horizontal));
    component().minutesField.addListener(minutes -> notifyListeners());
    component().secondsField.addListener(seconds -> notifyListeners());
  }

  @Override
  protected Integer getComponentValue() {
    return milliseconds(component().minutesField.getNumber(), component().secondsField.getNumber());
  }

  @Override
  protected void setComponentValue(Integer milliseconds) {
    component().minutesField.setNumber(minutes(milliseconds));
    component().secondsField.setNumber(seconds(milliseconds));
  }

  static final class MinutesSecondsPanel extends JPanel {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(MinutesSecondsPanel.class.getName());

    private final NumberField<Integer> minutesField = integerField()
            .transferFocusOnEnter(true)
            .selectAllOnFocusGained(true)
            .columns(2)
            .build();
    private final NumberField<Integer> secondsField = integerField()
            .valueRange(0, 59)
            .transferFocusOnEnter(true)
            .selectAllOnFocusGained(true)
            .columns(2)
            .build();

    private MinutesSecondsPanel(boolean horizontal) {
      super(borderLayout());
      if (horizontal) {
        gridLayoutPanel(1, 4)
                .add(new JLabel(BUNDLE.getString("min")))
                .add(minutesField)
                .add(new JLabel(BUNDLE.getString("sec")))
                .add(secondsField)
                .build(panel -> add(panel, BorderLayout.CENTER));
      }
      else {
        gridLayoutPanel(1, 2)
                .add(new JLabel(BUNDLE.getString("min")))
                .add(new JLabel(BUNDLE.getString("sec")))
                .build(panel -> add(panel, BorderLayout.NORTH));
        gridLayoutPanel(1, 2)
                .add(minutesField)
                .add(secondsField)
                .build(panel -> add(panel, BorderLayout.CENTER));
      }
    }
  }
}
