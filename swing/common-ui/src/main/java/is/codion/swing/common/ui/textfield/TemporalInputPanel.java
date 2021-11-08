/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.state.State;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.calendar.CalendarPanel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.Optional;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

/**
 * A panel for Temporal input
 * @param <T> the Temporal type supplied by this panel
 */
public final class TemporalInputPanel<T extends Temporal> extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(TemporalInputPanel.class.getName());

  private final TemporalField<T> inputField;
  private final JButton calendarButton;

  /**
   * Instantiates a new TemporalInputPanel.
   * @param temporalField the temporal input field
   */
  public TemporalInputPanel(final TemporalField<T> temporalField) {
    super(new BorderLayout());
    this.inputField = requireNonNull(temporalField, "temporalField");
    this.calendarButton = Control.builder(this::displayCalendar)
            .caption("...")
            .build()
            .createButton();
    this.calendarButton.setPreferredSize(TextFields.DIMENSION_TEXT_FIELD_SQUARE);
    add(temporalField, BorderLayout.CENTER);
    add(calendarButton, BorderLayout.EAST);
    addFocusListener(new InputFocusAdapter(temporalField));
  }

  /**
   * @return the input field
   */
  public TemporalField<T> getInputField() {
    return inputField;
  }

  /**
   * @return the calendar button
   */
  public JButton getCalendarButton() {
    return calendarButton;
  }

  /**
   * @return the Temporal value currently being displayed, an empty Optional in case of an incomplete/unparseable date
   */
  public Optional<T> getOptional() {
    return inputField.getOptional();
  }

  /**
   * @return the Temporal value currently being displayed, null in case of an incomplete/unparseable date
   */
  public T getTemporal() {
    return inputField.getTemporal();
  }

  /**
   * Sets the date in the input field, clears the field if {@code date} is null.
   * @param temporal the temporal value to set
   */
  public void setTemporal(final Temporal temporal) {
    inputField.setTemporal(temporal);
  }

  /**
   * @return the format pattern
   */
  public String getDateTimePattern() {
    return inputField.getDateTimePattern();
  }

  /**
   * @param transferFocusOnEnter specifies whether focus should be transferred on Enter
   */
  public void setTransferFocusOnEnter(final boolean transferFocusOnEnter) {
    if (transferFocusOnEnter) {
      Components.transferFocusOnEnter(inputField);
      Components.transferFocusOnEnter(calendarButton);
    }
    else {
      Components.removeTransferFocusOnEnter(inputField);
      Components.removeTransferFocusOnEnter(calendarButton);
    }
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    inputField.setEnabled(enabled);
    calendarButton.setEnabled(enabled);
  }

  private void displayCalendar() {
    if (inputField.getTemporalClass().equals(LocalDate.class)) {
      displayCalendarForLocalDate();
    }
    else if (inputField.getTemporalClass().equals(LocalDateTime.class)) {
      displayCalendarForLocalDateTime();
    }
    else {
      throw new IllegalStateException("Unsupported Temporal type: " + inputField.getTemporalClass());
    }
  }

  private void displayCalendarForLocalDate() {
    getLocalDateWithCalendar((LocalDate) getTemporal(), MESSAGES.getString("select_date"), inputField)
            .ifPresent(localDate -> {
              inputField.setText(getInputField().getDateTimeFormatter().format(localDate));
              inputField.requestFocusInWindow();
            });
  }

  private void displayCalendarForLocalDateTime() {
    getLocalDateTimeWithCalendar((LocalDateTime) getTemporal(), MESSAGES.getString("select_date_time"), inputField)
            .ifPresent(localDateTime -> {
              inputField.setText(getInputField().getDateTimeFormatter().format(localDateTime));
              inputField.requestFocusInWindow();
            });
  }

  /**
   * Retrieves a LocalDate from the user.
   * @param startDate the starting date, if null the current date is used
   * @param message the message to display as dialog title
   * @param parent the dialog parent
   * @return a LocalDate from the user, {@link Optional#empty()} in case the user cancels
   */
  public static Optional<LocalDate> getLocalDateWithCalendar(final LocalDate startDate, final String message, final JComponent parent) {
    final CalendarPanel calendarPanel = CalendarPanel.dateCalendarPanel();
    if (startDate != null) {
      calendarPanel.setDate(startDate);
    }
    final State okPressed = State.state();
    Dialogs.okCancelDialogBuilder(calendarPanel)
            .owner(parent)
            .title(message)
            .onOk(() -> okPressed.set(true))
            .show();

    return okPressed.get() ? Optional.of(calendarPanel.getDate()) : Optional.empty();
  }

  /**
   * Retrieves a LocalDateTime from the user.
   * @param startDateTime the starting date, if null the current date is used
   * @param dialogTitle the dialog title
   * @param parent the dialog parent
   * @return a LocalDateTime from the user, {@link Optional#empty()} in case the user cancels
   */
  public static Optional<LocalDateTime> getLocalDateTimeWithCalendar(final LocalDateTime startDateTime, final String dialogTitle,
                                                                     final JComponent parent) {
    final CalendarPanel calendarPanel = CalendarPanel.dateTimeCalendarPanel();
    if (startDateTime != null) {
      calendarPanel.setDateTime(startDateTime);
    }
    final State okPressed = State.state();
    Dialogs.okCancelDialogBuilder(calendarPanel)
            .owner(parent)
            .title(dialogTitle)
            .onOk(() -> okPressed.set(true))
            .show();

    return okPressed.get() ? Optional.of(calendarPanel.getDateTime()) : Optional.empty();
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
