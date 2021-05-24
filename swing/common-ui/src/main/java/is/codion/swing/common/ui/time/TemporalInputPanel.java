/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.time;

import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.textfield.TemporalField;

import com.github.lgooddatepicker.components.CalendarPanel;
import com.github.lgooddatepicker.components.TimePicker;
import com.github.lgooddatepicker.components.TimePickerSettings;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

/**
 * A panel for Temporal input
 * @param <T> the Temporal type supplied by this panel
 */
public class TemporalInputPanel<T extends Temporal> extends JPanel {

  private final TemporalField<T> inputField;

  /**
   * Instantiates a new TemporalInputPanel.
   * @param temporalField the temporal input field
   */
  public TemporalInputPanel(final TemporalField<T> temporalField) {
    this(temporalField, null);
  }

  /**
   * Instantiates a new TemporalInputPanel.
   * @param temporalField the temporal input field
   * @param enabledState a StateObserver controlling the enabled state of the input field and button
   */
  public TemporalInputPanel(final TemporalField<T> temporalField, final StateObserver enabledState) {
    super(new BorderLayout());
    this.inputField = requireNonNull(temporalField, "temporalField");
    add(temporalField, BorderLayout.CENTER);
    addFocusListener(new InputFocusAdapter(temporalField));
    if (enabledState != null) {
      Components.linkToEnabledState(enabledState, temporalField);
    }
  }

  /**
   * @return the input field
   */
  public final TemporalField<T> getInputField() {
    return inputField;
  }

  /**
   * @return the Date currently being displayed, null in case of an incomplete date
   * @throws DateTimeParseException if unable to parse the text
   */
  public final T getTemporal() throws DateTimeParseException {
    return inputField.getTemporal();
  }

  /**
   * Sets the date in the input field, clears the field if {@code date} is null.
   * @param temporal the temporal value to set
   */
  public final void setTemporal(final Temporal temporal) {
    inputField.setTemporal(temporal);
  }

  /**
   * @return the format pattern
   */
  public final String getDateTimePattern() {
    return inputField.getDateTimePattern();
  }

  /**
   * Returns null by default.
   * @return the button, if any
   */
  public JButton getCalendarButton() {
    return null;
  }

  @Override
  public final void add(final Component component, final Object constraints) {
    //prevent override of method used in constructor
    super.add(component, constraints);
  }

  @Override
  public final synchronized void addFocusListener(final FocusListener listener) {
    //prevent override of method used in constructor
    super.addFocusListener(listener);
  }

  /**
   * A new Builder instance
   * @param <T> the Temporal type
   * @param temporalField the temporal field
   * @return a new builder
   */
  public static <T extends Temporal> Builder<T> builder(final TemporalField<T> temporalField) {
    return new TemporalPanelBuilder<>(temporalField);
  }

  /**
   * Retrieves a LocalDate from the user.
   * @param startDate the starting date, if null the current date is used
   * @param message the message to display as dialog title
   * @param parent the dialog parent
   * @return a LocalDate from the user
   * @throws is.codion.common.model.CancelException in case the user cancelled
   */
  public static LocalDate getLocalDateWithCalendar(final LocalDate startDate, final String message, final JComponent parent) {
    final CalendarPanel calendarPanel = new CalendarPanel();
    calendarPanel.setSelectedDate(startDate);
    final State okPressed = State.state();
    Dialogs.okCancelDialogBuilder()
            .owner(parent)
            .title(message)
            .component(calendarPanel)
            .okAction(Control.control(() -> {
              okPressed.set(true);
              Windows.getParentDialog(calendarPanel).dispose();
            }))
            .show();
    if (okPressed.get()) {
      return calendarPanel.getSelectedDate();
    }

    throw new CancelException();
  }

  /**
   * Retrieves a LocalDateTime from the user.
   * @param startDateTime the starting date, if null the current date is used
   * @param message the message to display as dialog title
   * @param parent the dialog parent
   * @return a LocalDateTime from the user
   * @throws is.codion.common.model.CancelException in case the user cancelled
   */
  public static LocalDateTime getLocalDateTimeWithCalendar(final LocalDateTime startDateTime, final String message,
                                                           final JComponent parent) {
    final TimePickerSettings timeSettings = new TimePickerSettings();
    timeSettings.use24HourClockFormat();
    timeSettings.setDisplaySpinnerButtons(true);
    timeSettings.setInitialTimeToNow();
    final TimePicker timePicker = new TimePicker(timeSettings);
    final CalendarPanel calendarPanel = new CalendarPanel();
    if (startDateTime != null) {
      calendarPanel.setSelectedDate(startDateTime.toLocalDate());
      timePicker.setTime(startDateTime.toLocalTime());
    }
    final JPanel dateTimePanel = new JPanel(Layouts.borderLayout());
    final JPanel timePanel = new JPanel(new GridBagLayout());
    timePanel.add(timePicker);
    dateTimePanel.add(calendarPanel, BorderLayout.CENTER);
    dateTimePanel.add(timePanel, BorderLayout.EAST);
    final State okPressed = State.state();
    Dialogs.okCancelDialogBuilder()
            .owner(parent)
            .title(message)
            .component(dateTimePanel)
            .okAction(Control.control(() -> {
              okPressed.set(true);
              Windows.getParentDialog(dateTimePanel).dispose();
            }))
            .show();
    if (okPressed.get()) {
      return LocalDateTime.of(calendarPanel.getSelectedDate(), timePicker.getTime());
    }

    throw new CancelException();
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

  /**
   * A builder for {@link TemporalInputPanel}s
   * @param <T> the Temporal type
   */
  public interface Builder<T extends Temporal> {

    /**
     * @param initialValue the initial value to present
     * @return this builder instance
     */
    Builder<T> initialValue(T initialValue);

    /**
     * @param enabledState the enabled state
     * @return this builder instance
     */
    Builder<T> enabledState(StateObserver enabledState);

    /**
     * @param calendarButton true if a calendar button should be included, has no effect if not supported
     * @return this builder instance
     */
    Builder<T> calendarButton(boolean calendarButton);

    /**
     * @return a new {@link TemporalInputPanel} instance
     */
    TemporalInputPanel<T> build();
  }

  private static final class TemporalPanelBuilder<T extends Temporal> implements Builder<T> {

    private final TemporalField<T> temporalField;

    private T initialValue;
    private StateObserver enabledState;
    private boolean calendarButton;

    private TemporalPanelBuilder(final TemporalField<T> temporalField) {
      this.temporalField = requireNonNull(temporalField);
    }

    @Override
    public Builder<T> initialValue(final T initialValue) {
      this.initialValue = initialValue;
      return this;
    }

    @Override
    public Builder<T> enabledState(final StateObserver enabledState) {
      this.enabledState = enabledState;
      return this;
    }

    @Override
    public Builder<T> calendarButton(final boolean calendarButton) {
      this.calendarButton = calendarButton;
      return this;
    }

    @Override
    public TemporalInputPanel<T> build() {
      final TemporalInputPanel<T> inputPanel;
      final Class<T> temporalClass = temporalField.getTemporalClass();
      if (temporalClass.equals(LocalDate.class)) {
        inputPanel = (TemporalInputPanel<T>) new LocalDateInputPanel((TemporalField<LocalDate>) temporalField,
                calendarButton, enabledState);
      }
      else if (temporalClass.equals(LocalDateTime.class)) {
        inputPanel = (TemporalInputPanel<T>) new LocalDateTimeInputPanel((TemporalField<LocalDateTime>) temporalField,
                calendarButton, enabledState);
      }
      else {
        inputPanel = new TemporalInputPanel<>(temporalField, enabledState);
      }

      inputPanel.setTemporal(initialValue);

      return inputPanel;
    }
  }
}
