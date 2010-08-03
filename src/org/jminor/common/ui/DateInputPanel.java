/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Util;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A panel for displaying a formatted text field and a button activating a calendar for date input.
 */
public final class DateInputPanel extends JPanel {

  private final JFormattedTextField inputField;
  private final SimpleDateFormat dateFormat;
  private JButton button;

  public DateInputPanel(final Date initialValue, final SimpleDateFormat maskFormat) {
    this(UiUtil.createFormattedField(maskFormat, initialValue), maskFormat, true, null);
  }

  public DateInputPanel(final JFormattedTextField inputField, final SimpleDateFormat dateFormat,
                        final boolean includeButton, final StateObserver enabledState) {
    super(new BorderLayout());
    Util.rejectNullValue(inputField, "inputField");
    Util.rejectNullValue(dateFormat, "dateFormat");
    this.inputField = inputField;
    this.dateFormat = dateFormat;
    add(inputField, BorderLayout.CENTER);
    if (includeButton) {
      final AbstractAction buttonAction = new AbstractAction("...") {
        public void actionPerformed(final ActionEvent e) {
          Date currentValue = null;
          try {
            currentValue = getDate();
          }
          catch (ParseException ex) {/**/}
          final Date newValue = UiUtil.getDateFromUser(currentValue, Messages.get(Messages.SELECT_DATE), inputField);
          inputField.setText(dateFormat.format(newValue));
        }
      };
      this.button = new JButton(buttonAction);
      this.button.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);
      if (enabledState != null) {
        UiUtil.linkToEnabledState(enabledState, this.button);
      }
      addFocusListener(new InputFocusAdapter(inputField));
      add(this.button, BorderLayout.EAST);
    }
  }

  public JFormattedTextField getInputField() {
    return inputField;
  }

  public JButton getButton() {
    return button;
  }

  public Date getDate() throws ParseException {
    return dateFormat.parse(inputField.getText());
  }

  public String getFormatPattern() {
    return dateFormat.toPattern();
  }

  private static final class InputFocusAdapter extends FocusAdapter {
    private final JFormattedTextField inputField;

    private InputFocusAdapter(final JFormattedTextField inputField) {
      this.inputField = inputField;
    }

    @Override
    public void focusGained(final FocusEvent e) {
      inputField.requestFocusInWindow();
    }
  }
}
