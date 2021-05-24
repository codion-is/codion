/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.textfield.TemporalInputPanel;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.common.ui.value.UpdateOn;

import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

final class DefaultTemporalInputPanelBuiler<T extends Temporal>
        extends AbstractComponentBuilder<T, TemporalInputPanel<T>, TemporalInputPanelBuilder<T>>
        implements TemporalInputPanelBuilder<T> {

  private final Class<T> valueClass;

  private int columns;
  private UpdateOn updateOn = UpdateOn.KEYSTROKE;
  private String dateTimePattern;

  DefaultTemporalInputPanelBuiler(final Class<T> valueClass) {
    this.valueClass = requireNonNull(valueClass);
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
    return new TemporalInputPanel<>((TemporalField<T>) ComponentBuilders.textFieldBuilder(valueClass)
            .updateOn(updateOn)
            .columns(columns)
            .dateTimePattern(dateTimePattern)
            .build(), enabledState);
  }

  @Override
  protected ComponentValue<T, TemporalInputPanel<T>> buildComponentValue(final TemporalInputPanel<T> component) {
    return ComponentValues.temporalInputPanel(component);
  }

  @Override
  protected void setTransferFocusOnEnter(final TemporalInputPanel<T> component) {
    component.setTransferFocusOnEnter(true);
  }
}
