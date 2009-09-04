/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.State;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateInputPanel extends JPanel {

  private final JFormattedTextField inputField;
  private final SimpleDateFormat dateFormat;

  public DateInputPanel(final JFormattedTextField inputField, final SimpleDateFormat dateFormat,
                        final boolean includeButton, final State enabledState) {
    super(new BorderLayout());
    this.inputField = inputField;
    this.dateFormat = dateFormat;
    add(inputField, BorderLayout.CENTER);
    if (includeButton) {
      final AbstractAction buttonAction = new AbstractAction("...") {
        public void actionPerformed(ActionEvent e) {
          Date currentValue = null;
          try {
            currentValue = dateFormat.parse(inputField.getText());
          }
          catch (ParseException ex) {/**/}
          final Date newValue = UiUtil.getDateFromUser(currentValue,
                  Messages.get(Messages.SELECT_DATE), inputField);
          inputField.setText(dateFormat.format(newValue));
        }
      };
      final JButton btnChooser = new JButton(buttonAction);
      btnChooser.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);
      if (enabledState != null)
        UiUtil.linkToEnabledState(enabledState, btnChooser);
      add(btnChooser, BorderLayout.EAST);
    }
  }

  public JFormattedTextField getInputField() {
    return inputField;
  }

  public SimpleDateFormat getDateFormat() {
    return dateFormat;
  }
}
