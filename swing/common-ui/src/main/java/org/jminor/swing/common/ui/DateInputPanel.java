/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.StateObserver;
import org.jminor.common.Util;
import org.jminor.common.i18n.Messages;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.Controls;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
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
      final Control buttonControl = Controls.control(() -> {
        Date currentValue = null;
        try {
          currentValue = getDate();
        }
        catch (final ParseException ignored) {/*ignored*/}
        final Date newValue = UiUtil.getDateFromUser(currentValue, Messages.get(Messages.SELECT_DATE), inputField);
        if (newValue != null) {
          inputField.setText(dateFormat.format(newValue));
        }
      }, "...");
      this.button = new JButton(buttonControl);
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
   * @return the Date currently being displayed, null in case of an incomplete date
   * @throws ParseException in case the date can not be parsed
   */
  public Date getDate() throws ParseException {
    final String text = inputField.getText();
    if (!text.contains("_")) {
      return dateFormat.parse(text);
    }

    return null;
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

  /**
   * A panel containing two DateInputPanels for selecting a date interval
   */
  public static final class IntervalInputPanel extends JPanel {

    private final DateInputPanel fromInputPanel;
    private final DateInputPanel toInputPanel;

    /**
     * @param dateFormat the date format to use
     * @param initialValues the initial values, if null then no date values are selected
     */
    public IntervalInputPanel(final SimpleDateFormat dateFormat, final DateInterval initialValues) {
      fromInputPanel = new DateInputPanel(initialValues != null ? initialValues.getFrom() : null, dateFormat);
      toInputPanel = new DateInputPanel(initialValues != null ? initialValues.getTo() : null, dateFormat);
      fromInputPanel.getButton().setFocusable(false);
      toInputPanel.getButton().setFocusable(false);
      setLayout(new GridLayout(4, 1, 5, 5));
      add(new JLabel(Messages.get(Messages.FROM)));
      add(fromInputPanel);
      add(new JLabel(Messages.get(Messages.TO)));
      add(toInputPanel);
    }

    /**
     * @return the input panel for the 'from' value
     */
    public DateInputPanel getFromInputPanel() {
      return fromInputPanel;
    }

    /**
     * @return the input panel for the 'to' value
     */
    public DateInputPanel getToInputPanel() {
      return toInputPanel;
    }

    /**
     * @return the date interval represented by the input fields, null if either is not specified
     */
    public DateInterval getDateInterval() {
      try {
        final Date from = fromInputPanel.getDate();
        final Date to = toInputPanel.getDate();
        if (Util.notNull(from, to)) {
          return new DateInterval(from, to);
        }

        return null;
      }
      catch (final ParseException e) {
        return null;
      }
    }
  }

  /**
   * A Date interval with from and to fields
   */
  public static final class DateInterval {

    private final Date from;
    private final Date to;

    /**
     * @param from the from date
     * @param to the to date
     */
    public DateInterval(final Date from, final Date to) {
      this.from = from;
      this.to = to;
    }

    /**
     * @return the from part of this interval
     */
    public Date getFrom() {
      return from;
    }

    /**
     * @return the to part of this interval
     */
    public Date getTo() {
      return to;
    }
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
