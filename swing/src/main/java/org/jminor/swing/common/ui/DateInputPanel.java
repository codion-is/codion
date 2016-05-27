/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.StateObserver;
import org.jminor.common.i18n.Messages;

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
import java.util.Objects;

/**
 * A panel for displaying a formatted text field and a button activating a calendar for date input.
 */
public final class DateInputPanel extends JPanel {

  private final JFormattedTextField inputField;
  private final SimpleDateFormat dateFormat;
  private JButton button;

  /**
   * Instantiates a new DateInputPanel.
   * @param initialValue the initial value to display
   * @param dateFormat the date format
   */
  public DateInputPanel(final Date initialValue, final SimpleDateFormat dateFormat) {
    this(UiUtil.createFormattedDateField(dateFormat, initialValue), dateFormat, true, null);
  }

  /**
   * Instantiates a new DateInputPanel.
   * @param inputField the input field
   * @param dateFormat the date format
   * @param includeButton if true a "..." button is included
   * @param enabledState a StateObserver controlling the enabled state of the input field and button
   */
  public DateInputPanel(final JFormattedTextField inputField, final SimpleDateFormat dateFormat,
                        final boolean includeButton, final StateObserver enabledState) {
    super(new BorderLayout());
    Objects.requireNonNull(inputField, "inputField");
    Objects.requireNonNull(dateFormat, "dateFormat");
    this.inputField = inputField;
    this.dateFormat = dateFormat;
    add(inputField, BorderLayout.CENTER);
    addFocusListener(new InputFocusAdapter(inputField));
    if (includeButton) {
      final AbstractAction buttonAction = new AbstractAction("...") {
        @Override
        public void actionPerformed(final ActionEvent e) {
          Date currentValue = null;
          try {
            currentValue = getDate();
          }
          catch (final ParseException ignored) {/*ignored*/}
          final Date newValue = UiUtil.getDateFromUser(currentValue, Messages.get(Messages.SELECT_DATE), inputField);
          if (newValue != null) {
            inputField.setText(dateFormat.format(newValue));
          }
        }
      };
      this.button = new JButton(buttonAction);
      this.button.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);
      if (enabledState != null) {
        UiUtil.linkToEnabledState(enabledState, this.inputField);
        UiUtil.linkToEnabledState(enabledState, this.button);
      }
      add(this.button, BorderLayout.EAST);
    }
  }

  /**
   * @return the input field
   */
  public JFormattedTextField getInputField() {
    return inputField;
  }

  /**
   * @return the button, if any
   */
  public JButton getButton() {
    return button;
  }

  /**
   * @return the Date currently being displayed
   * @throws ParseException in case the date can not be parsed
   */
  public Date getDate() throws ParseException {
    return dateFormat.parse(inputField.getText());
  }

  /**
   * Sets the date in the input field, clears the field if {@code date} is null.
   * @param date the date to set
   */
  public void setDate(final Date date) {
    inputField.setText(date == null ? "" : dateFormat.format(date));
  }

  /**
   * @return the format pattern
   */
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
