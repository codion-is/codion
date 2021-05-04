/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.time;

import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.textfield.TemporalField;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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

  /**
   * A new Builder instance
   * @param <T> the Temporal type
   * @return a new builder
   */
  public static <T extends Temporal> Builder<T> builder() {
    return new TemporalPanelBuilder<>();
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
     * @param textField the input field
     * @return this builder instance
     */
    Builder<T> textField(TemporalField<T> textField);

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
     * @param calendarButton true if a calendar button should be included, may not be supported
     * @return this builder instance
     */
    Builder<T> calendarButton(boolean calendarButton);

    /**
     * @return a new {@link TemporalInputPanel} instance
     */
    TemporalInputPanel<T> build();
  }

  private static final class TemporalPanelBuilder<T extends Temporal> implements Builder<T> {

    protected TemporalField<T> textField;
    protected T initialValue;
    protected StateObserver enabledState;
    protected boolean calendarButton;

    @Override
    public Builder<T> textField(final TemporalField<T> textField) {
      this.textField = requireNonNull(textField);
      return this;
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
      if (textField == null) {
        throw new IllegalStateException("Temporal field must be set before building");
      }

      final TemporalInputPanel<T> inputPanel;
      final Class<T> temporalClass = textField.getTemporalClass();
      if (temporalClass.equals(LocalDate.class)) {
        inputPanel = (TemporalInputPanel<T>) new LocalDateInputPanel((TemporalField<LocalDate>) textField,
                calendarButton, enabledState);
      }
      else if (temporalClass.equals(LocalDateTime.class)) {
        inputPanel = (TemporalInputPanel<T>) new LocalDateTimeInputPanel((TemporalField<LocalDateTime>) textField,
                calendarButton, enabledState);
      }
      else {
        inputPanel = new TemporalInputPanel<>(textField, enabledState);
      }

      inputPanel.setTemporal(initialValue);

      return inputPanel;
    }
  }
}
