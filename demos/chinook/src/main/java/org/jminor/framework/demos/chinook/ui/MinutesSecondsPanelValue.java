/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.ui;

import org.jminor.swing.common.model.textfield.DocumentAdapter;
import org.jminor.swing.common.ui.textfield.IntegerField;
import org.jminor.swing.common.ui.value.AbstractComponentValue;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;
import static org.jminor.swing.common.ui.KeyEvents.transferFocusOnEnter;
import static org.jminor.swing.common.ui.layout.Layouts.borderLayout;
import static org.jminor.swing.common.ui.layout.Layouts.gridLayout;
import static org.jminor.swing.common.ui.textfield.TextFields.selectAllOnFocusGained;

final class MinutesSecondsPanelValue extends AbstractComponentValue<Integer, MinutesSecondsPanelValue.MinutesSecondsPanel> {

  MinutesSecondsPanelValue() {
    super(new MinutesSecondsPanel());
    getComponent().minutesField.getDocument().addDocumentListener((DocumentAdapter) e -> notifyValueChange());
    getComponent().secondsField.getDocument().addDocumentListener((DocumentAdapter) e -> notifyValueChange());
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

    private final IntegerField minutesField = new IntegerField(2);
    private final IntegerField secondsField = new IntegerField(2);

    private MinutesSecondsPanel() {
      super(borderLayout());
      transferFocusOnEnter(minutesField);
      transferFocusOnEnter(secondsField);
      selectAllOnFocusGained(minutesField);
      selectAllOnFocusGained(secondsField);
      final JPanel northPanel = new JPanel(gridLayout(1, 2));
      northPanel.add(new JLabel("Min."));
      northPanel.add(new JLabel("Sec."));
      final JPanel centerPanel = new JPanel(gridLayout(1, 2));
      centerPanel.add(minutesField);
      centerPanel.add(secondsField);
      add(northPanel, BorderLayout.NORTH);
      add(centerPanel, BorderLayout.CENTER);
    }
  }
}
