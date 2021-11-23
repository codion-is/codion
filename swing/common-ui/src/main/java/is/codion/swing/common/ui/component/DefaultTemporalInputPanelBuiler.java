/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TemporalInputPanel;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.common.ui.value.UpdateOn;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

final class DefaultTemporalInputPanelBuiler<T extends Temporal>
        extends AbstractComponentBuilder<T, TemporalInputPanel<T>, TemporalInputPanelBuilder<T>>
        implements TemporalInputPanelBuilder<T> {

  private final Class<T> valueClass;

  private int columns;
  private UpdateOn updateOn = UpdateOn.KEYSTROKE;
  private String dateTimePattern;
  private boolean selectAllOnFocusGained;

  DefaultTemporalInputPanelBuiler(final Class<T> valueClass, final String dateTimePattern) {
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
    return new TemporalInputPanel<>(createTemporalField());
  }

  @Override
  protected ComponentValue<T, TemporalInputPanel<T>> buildComponentValue(final TemporalInputPanel<T> component) {
    return ComponentValues.temporalInputPanel(component);
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
      return (TemporalField<T>) new DefaultLocalTimeFieldBuilder(dateTimePattern)
              .updateOn(updateOn)
              .selectAllOnFocusGained(selectAllOnFocusGained)
              .columns(columns)
              .build();
    }
    else if (valueClass.equals(LocalDate.class)) {
      return (TemporalField<T>) new DefaultLocalDateFieldBuilder(dateTimePattern)
              .updateOn(updateOn)
              .selectAllOnFocusGained(selectAllOnFocusGained)
              .columns(columns)
              .build();
    }
    else if (valueClass.equals(LocalDateTime.class)) {
      return (TemporalField<T>) new DefaultLocalDateTimeFieldBuilder(dateTimePattern)
              .updateOn(updateOn)
              .selectAllOnFocusGained(selectAllOnFocusGained)
              .columns(columns)
              .build();
    }
    else if (valueClass.equals(OffsetDateTime.class)) {
      return (TemporalField<T>) new DefaultOffsetDateTimeFieldBuilder(dateTimePattern)
              .updateOn(updateOn)
              .selectAllOnFocusGained(selectAllOnFocusGained)
              .columns(columns)
              .build();
    }

    throw new IllegalStateException("Unsopported temporal type: " + valueClass);
  }
}
