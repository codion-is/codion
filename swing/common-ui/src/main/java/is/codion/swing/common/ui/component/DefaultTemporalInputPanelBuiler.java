/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.time.TemporalInputPanel;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.common.ui.value.UpdateOn;

import java.time.temporal.Temporal;

import static java.util.Objects.requireNonNull;

final class DefaultTemporalInputPanelBuiler<T extends Temporal>
        extends AbstractComponentBuilder<T, TemporalInputPanel<T>, TemporalInputPanelBuilder<T>>
        implements TemporalInputPanelBuilder<T> {

  private final TemporalField<T> temporalField;

  private UpdateOn updateOn = UpdateOn.KEYSTROKE;
  private boolean calendarButton;
  private int columns;

  DefaultTemporalInputPanelBuiler(final Value<T> value, final TemporalField<T> temporalField) {
    super(value);
    this.temporalField = requireNonNull(temporalField);
  }

  @Override
  public TemporalInputPanelBuilder<T> updateOn(final UpdateOn updateOn) {
    this.updateOn = requireNonNull(updateOn);
    return this;
  }

  @Override
  public TemporalInputPanelBuilder<T> columns(final int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  public TemporalInputPanelBuilder<T> calendarButton(final boolean calendarButton) {
    this.calendarButton = calendarButton;
    return this;
  }

  @Override
  protected TemporalInputPanel<T> buildComponent() {
    final TemporalInputPanel<T> inputPanel = createTemporalInputPanel();
    inputPanel.getInputField().setColumns(columns);

    return inputPanel;
  }

  @Override
  protected void setTransferFocusOnEnter(final TemporalInputPanel<T> component) {
    Components.transferFocusOnEnter(component.getInputField());
    if (component.getCalendarButton() != null) {
      Components.transferFocusOnEnter(component.getCalendarButton());
    }
  }

  private <T extends Temporal> TemporalInputPanel<T> createTemporalInputPanel() {
    ComponentValues.temporalField(temporalField, updateOn).link(value);

    return (TemporalInputPanel<T>) TemporalInputPanel.builder()
            .temporalField((TemporalField<Temporal>) temporalField)
            .calendarButton(calendarButton)
            .enabledState(enabledState)
            .build();
  }
}
