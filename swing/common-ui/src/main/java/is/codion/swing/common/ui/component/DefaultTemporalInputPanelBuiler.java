/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.calendar.CalendarPanel;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TemporalInputPanel;

import javax.swing.JComponent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

final class DefaultTemporalInputPanelBuiler<T extends Temporal>
        extends AbstractComponentBuilder<T, TemporalInputPanel<T>, TemporalInputPanelBuilder<T>>
        implements TemporalInputPanelBuilder<T> {

  private final Class<T> valueClass;

  private int columns;
  private UpdateOn updateOn = UpdateOn.KEYSTROKE;
  private String dateTimePattern;
  private boolean selectAllOnFocusGained;

  DefaultTemporalInputPanelBuiler(final Class<T> valueClass, final String dateTimePattern, final Value<T> linkedValue) {
    super(linkedValue);
    this.valueClass = requireNonNull(valueClass);
    this.dateTimePattern = dateTimePattern;
  }

  @Override
  public TemporalInputPanelBuilder<T> selectAllOnFocusGained(final boolean selectAllOnFocusGained) {
    this.selectAllOnFocusGained = selectAllOnFocusGained;
    return this;
  }

  @Override
  public TemporalInputPanelBuilder<T> columns(final int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  public TemporalInputPanelBuilder<T> updateOn(final UpdateOn updateOn) {
    this.updateOn = requireNonNull(updateOn);
    return this;
  }

  @Override
  public TemporalInputPanelBuilder<T> dateTimePattern(final String dateTimePattern) {
    this.dateTimePattern = requireNonNull(dateTimePattern);
    return this;
  }

  @Override
  protected TemporalInputPanel<T> buildComponent() {
    return new TemporalInputPanel<>(createTemporalField(), calendarProvider());
  }

  @Override
  protected ComponentValue<T, TemporalInputPanel<T>> buildComponentValue(final TemporalInputPanel<T> component) {
    return component.componentValue();
  }

  @Override
  protected void setTransferFocusOnEnter(final TemporalInputPanel<T> component) {
    component.setTransferFocusOnEnter(true);
  }

  @Override
  protected void setInitialValue(final TemporalInputPanel<T> component, final T initialValue) {
    component.setTemporal(initialValue);
  }

  private TemporalField<T> createTemporalField() {
    if (valueClass.equals(LocalTime.class)) {
      return (TemporalField<T>) new DefaultLocalTimeFieldBuilder(dateTimePattern, null)
              .updateOn(updateOn)
              .selectAllOnFocusGained(selectAllOnFocusGained)
              .columns(columns)
              .build();
    }
    else if (valueClass.equals(LocalDate.class)) {
      return (TemporalField<T>) new DefaultLocalDateFieldBuilder(dateTimePattern, null)
              .updateOn(updateOn)
              .selectAllOnFocusGained(selectAllOnFocusGained)
              .columns(columns)
              .build();
    }
    else if (valueClass.equals(LocalDateTime.class)) {
      return (TemporalField<T>) new DefaultLocalDateTimeFieldBuilder(dateTimePattern, null)
              .updateOn(updateOn)
              .selectAllOnFocusGained(selectAllOnFocusGained)
              .columns(columns)
              .build();
    }
    else if (valueClass.equals(OffsetDateTime.class)) {
      return (TemporalField<T>) new DefaultOffsetDateTimeFieldBuilder(dateTimePattern, null)
              .updateOn(updateOn)
              .selectAllOnFocusGained(selectAllOnFocusGained)
              .columns(columns)
              .build();
    }

    throw new IllegalStateException("Unsopported temporal type: " + valueClass);
  }

  private static TemporalInputPanel.CalendarProvider calendarProvider() {
    return new TemporalInputPanel.CalendarProvider() {
      @Override
      public Optional<LocalDate> getLocalDate(final String dialogTitle, final JComponent dialogOwner,
                                              final LocalDate startDate) {
        return CalendarPanel.getLocalDate(dialogTitle, dialogOwner, startDate);
      }

      @Override
      public Optional<LocalDateTime> getLocalDateTime(final String dialogTitle, final JComponent dialogOwner,
                                                      final LocalDateTime startDateTime) {
        return CalendarPanel.getLocalDateTime(dialogTitle, dialogOwner, startDateTime);
      }
    };
  }
}
