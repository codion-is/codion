/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.control.Control;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.time.temporal.Temporal;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A panel for Temporal input
 * @param <T> the Temporal type supplied by this panel
 */
public final class TemporalInputPanel<T extends Temporal> extends JPanel {

  private final TemporalField<T> inputField;
  private final CalendarProvider calendarProvider;
  private final JButton calendarButton;

  /**
   * Instantiates a new TemporalInputPanel.
   * @param temporalField the temporal input field
   * @param calendarProvider a calendar UI provider
   */
  public TemporalInputPanel(TemporalField<T> temporalField, CalendarProvider calendarProvider) {
    super(new BorderLayout());
    this.inputField = requireNonNull(temporalField, "temporalField");
    this.calendarProvider = requireNonNull(calendarProvider, "calendarProvider");
    add(temporalField, BorderLayout.CENTER);
    if (calendarProvider.supports(temporalField.getTemporalClass())) {
      Control displayCalendarControl = Control.builder(this::displayCalendar)
              .caption("...")
              .build();
      KeyEvents.builder(KeyEvent.VK_INSERT)
              .action(displayCalendarControl)
              .enable(temporalField);
      calendarButton = new JButton(displayCalendarControl);
      calendarButton.setPreferredSize(TextComponents.DIMENSION_TEXT_FIELD_SQUARE);
      add(calendarButton, BorderLayout.EAST);
    }
    else {
      calendarButton = null;
    }
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
  public Optional<JButton> getCalendarButton() {
    return Optional.ofNullable(calendarButton);
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
  public void setTemporal(Temporal temporal) {
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
  public void setTransferFocusOnEnter(boolean transferFocusOnEnter) {
    if (transferFocusOnEnter) {
      TransferFocusOnEnter.enable(inputField);
      if (calendarButton != null) {
        TransferFocusOnEnter.enable(calendarButton);
      }
    }
    else {
      TransferFocusOnEnter.disable(inputField);
      if (calendarButton != null) {
        TransferFocusOnEnter.disable(calendarButton);
      }
    }
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    inputField.setEnabled(enabled);
    if (calendarButton != null) {
      calendarButton.setEnabled(enabled);
    }
  }

  /**
   * Provides a calendar UI in a dialog for retrieving a date and/or time from the user.
   */
  public interface CalendarProvider {

    /**
     * Retrieves a Temporal value from the user by displaying a calendar in a dialog.
     * @param temporalClass the temporal class
     * @param dialogOwner the dialog owner
     * @param initialValue the initial, if null the current date/time is used
     * @param <T> the temporal type
     * @return a Temporal value from the user, {@link Optional#empty()} in case the user cancels
     */
    <T extends Temporal> Optional<T> getTemporal(Class<T> temporalClass, JComponent dialogOwner, T initialValue);

    /**
     * @param temporalClass the temporal class
     * @param <T> the temporal type
     * @return true if this calendar provider supports the given type
     */
    <T extends Temporal> boolean supports(Class<T> temporalClass);
  }

  private void displayCalendar() {
    calendarProvider.getTemporal(inputField.getTemporalClass(), inputField, getTemporal())
            .ifPresent(inputField::setTemporal);
  }

  private static final class InputFocusAdapter extends FocusAdapter {
    private final JFormattedTextField inputField;

    private InputFocusAdapter(JFormattedTextField inputField) {
      this.inputField = inputField;
    }

    @Override
    public void focusGained(FocusEvent e) {
      inputField.requestFocusInWindow();
    }
  }
}
