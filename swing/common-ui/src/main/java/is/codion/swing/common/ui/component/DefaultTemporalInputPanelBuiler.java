/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.calendar.CalendarPanel;
import is.codion.swing.common.ui.component.textfield.TemporalField;
import is.codion.swing.common.ui.component.textfield.TemporalInputPanel;
import is.codion.swing.common.ui.component.textfield.TemporalInputPanel.CalendarProvider;

import javax.swing.JComponent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

final class DefaultTemporalInputPanelBuiler<T extends Temporal>
        extends AbstractComponentBuilder<T, TemporalInputPanel<T>, TemporalInputPanelBuilder<T>>
        implements TemporalInputPanelBuilder<T> {

  private static final List<Class<?>> SUPPORTED_TYPES =
          Arrays.asList(LocalTime.class, LocalDate.class, LocalDateTime.class, OffsetDateTime.class);

  private final Class<T> valueClass;
  private final String dateTimePattern;

  private int columns;
  private UpdateOn updateOn = UpdateOn.KEYSTROKE;
  private boolean selectAllOnFocusGained;
  private CalendarProvider calendarProvider = calendarProvider();
  private boolean buttonFocusable;

  DefaultTemporalInputPanelBuiler(Class<T> valueClass, String dateTimePattern, Value<T> linkedValue) {
    super(linkedValue);
    this.valueClass = requireNonNull(valueClass);
    this.dateTimePattern = requireNonNull(dateTimePattern);
  }

  @Override
  public TemporalInputPanelBuilder<T> selectAllOnFocusGained(boolean selectAllOnFocusGained) {
    this.selectAllOnFocusGained = selectAllOnFocusGained;
    return this;
  }

  @Override
  public TemporalInputPanelBuilder<T> columns(int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  public TemporalInputPanelBuilder<T> updateOn(UpdateOn updateOn) {
    this.updateOn = requireNonNull(updateOn);
    return this;
  }

  @Override
  public TemporalInputPanelBuilder<T> calendarProvider(CalendarProvider calendarProvider) {
    this.calendarProvider = requireNonNull(calendarProvider);
    return this;
  }

  @Override
  public TemporalInputPanelBuilder<T> buttonFocusable(boolean buttonFocusable) {
    this.buttonFocusable = buttonFocusable;
    return this;
  }

  @Override
  protected TemporalInputPanel<T> createComponent() {
    TemporalInputPanel<T> inputPanel = new TemporalInputPanel<>(createTemporalField(), calendarProvider);
    inputPanel.getCalendarButton().ifPresent(button ->
            button.setFocusable(buttonFocusable));

    return inputPanel;
  }

  @Override
  protected ComponentValue<T, TemporalInputPanel<T>> createComponentValue(TemporalInputPanel<T> component) {
    return new TemporalInputPanelValue<>(component);
  }

  @Override
  protected void setTransferFocusOnEnter(TemporalInputPanel<T> component) {
    component.setTransferFocusOnEnter(true);
  }

  @Override
  protected void setInitialValue(TemporalInputPanel<T> component, T initialValue) {
    component.setTemporal(initialValue);
  }

  private TemporalField<T> createTemporalField() {
    if (!SUPPORTED_TYPES.contains(valueClass)) {
      throw new IllegalStateException("Unsupported temporal type: " + valueClass);
    }

    return (TemporalField<T>) new DefaultTemporalFieldBuilder<>(valueClass, dateTimePattern, null)
              .updateOn(updateOn)
              .selectAllOnFocusGained(selectAllOnFocusGained)
              .columns(columns)
              .build();
  }

  private static CalendarProvider calendarProvider() {
    return new CalendarProvider() {
      @Override
      public <T extends Temporal> Optional<T> getTemporal(Class<T> temporalClass, JComponent dialogOwner,
                                                          T initialValue) {
        if (LocalDate.class.equals(temporalClass)) {
          return (Optional<T>) CalendarPanel.getLocalDate(dialogOwner, (LocalDate) initialValue);
        }
        if (LocalDateTime.class.equals(temporalClass)) {
          return (Optional<T>) CalendarPanel.getLocalDateTime(dialogOwner, (LocalDateTime) initialValue);
        }

        throw new IllegalArgumentException("Unsupported temporal type: " + temporalClass);
      }

      @Override
      public <T extends Temporal> boolean supports(Class<T> temporalClass) {
        return LocalDate.class.equals(temporalClass) || LocalDateTime.class.equals(temporalClass);
      }
    };
  }

  private static final class TemporalInputPanelValue<T extends Temporal> extends AbstractComponentValue<T, TemporalInputPanel<T>> {

    private TemporalInputPanelValue(TemporalInputPanel<T> inputPanel) {
      super(inputPanel);
      inputPanel.getInputField().addTemporalListener(temporal -> notifyValueChange());
    }

    @Override
    protected T getComponentValue(TemporalInputPanel<T> component) {
      return component.getTemporal();
    }

    @Override
    protected void setComponentValue(TemporalInputPanel<T> component, T value) {
      component.setTemporal(value);
    }
  }
}
