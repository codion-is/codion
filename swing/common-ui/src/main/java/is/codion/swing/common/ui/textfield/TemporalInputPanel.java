/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.component.AbstractComponentValue;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.control.Control;

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
  private final CalendarProvider calendarProvider;
  private final JButton calendarButton;

  /**
   * Instantiates a new TemporalInputPanel.
   * @param temporalField the temporal input field
   * @param calendarProvider a calendar UI provider
   */
  public TemporalInputPanel(final TemporalField<T> temporalField, final CalendarProvider calendarProvider) {
    super(new BorderLayout());
    this.inputField = requireNonNull(temporalField, "temporalField");
    this.calendarProvider = requireNonNull(calendarProvider, "calendarProvider");
    this.calendarButton = new JButton(Control.builder(this::displayCalendar)
            .caption("...")
            .build());
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
      TransferFocusOnEnter.enable(inputField);
      TransferFocusOnEnter.enable(calendarButton);
    }
    else {
      TransferFocusOnEnter.disable(inputField);
      TransferFocusOnEnter.disable(calendarButton);
    }
  }

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    inputField.setEnabled(enabled);
    calendarButton.setEnabled(enabled);
  }

  /**
   * Instantiates a new {@link ComponentValue} based on this input panel.
   * @return a component value bound to this input panel
   */
  public ComponentValue<T, TemporalInputPanel<T>> componentValue() {
    return new TemporalInputPanelValue<>(this);
  }

  /**
   * Provides a calendar UI for retrieving a date from the user.
   */
  public interface CalendarProvider {

    /**
     * Retrieves a LocalDateTime from the user by displaying a calendar.
     * @param dialogTitle the dialog title
     * @param dialogOwner the dialog owner
     * @param startDate the start date
     * @return a LocalDateTime from the user, {@link Optional#empty()} in case the user cancels
     */
    Optional<LocalDate> getLocalDate(String dialogTitle, JComponent dialogOwner, LocalDate startDate);

    /**
     * Retrieves a LocalDateTime from the user by displaying a calendar.
     * @param dialogTitle the dialog title
     * @param dialogOwner the dialog owner
     * @param startDateTime the starting date, if null the current date is used
     * @return a LocalDateTime from the user, {@link Optional#empty()} in case the user cancels
     */
    Optional<LocalDateTime> getLocalDateTime(String dialogTitle, JComponent dialogOwner, LocalDateTime startDateTime);
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
    calendarProvider.getLocalDate(MESSAGES.getString("select_date"), inputField, (LocalDate) getTemporal())
            .ifPresent(localDate -> {
              inputField.setText(getInputField().getDateTimeFormatter().format(localDate));
              inputField.requestFocusInWindow();
            });
  }

  private void displayCalendarForLocalDateTime() {
    calendarProvider.getLocalDateTime(MESSAGES.getString("select_date_time"), inputField, (LocalDateTime) getTemporal())
            .ifPresent(localDateTime -> {
              inputField.setText(getInputField().getDateTimeFormatter().format(localDateTime));
              inputField.requestFocusInWindow();
            });
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

  private static final class TemporalInputPanelValue<T extends Temporal> extends AbstractComponentValue<T, TemporalInputPanel<T>> {

    private TemporalInputPanelValue(final TemporalInputPanel<T> inputPanel) {
      super(inputPanel);
      inputPanel.getInputField().addTemporalListener(temporal -> notifyValueChange());
    }

    @Override
    protected T getComponentValue(final TemporalInputPanel<T> component) {
      return component.getTemporal();
    }

    @Override
    protected void setComponentValue(final TemporalInputPanel<T> component, final T value) {
      component.setTemporal(value);
    }
  }
}
