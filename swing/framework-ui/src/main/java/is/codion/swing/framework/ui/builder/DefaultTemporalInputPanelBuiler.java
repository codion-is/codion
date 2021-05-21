/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.textfield.TemporalField;
import is.codion.swing.common.ui.time.TemporalInputPanel;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.common.ui.value.UpdateOn;

import java.awt.Dimension;
import java.time.temporal.Temporal;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultTemporalInputPanelBuiler<T extends Temporal> extends AbstractComponentBuilder<T,
        TemporalInputPanel<T>>
        implements TemporalInputPanelBuilder<T> {

  private UpdateOn updateOn = UpdateOn.KEYSTROKE;
  private boolean calendarButton;
  private int columns;

  DefaultTemporalInputPanelBuiler(final Property<T> attribute, final Value<T> value) {
    super(attribute, value);
  }

  @Override
  public TemporalInputPanelBuilder<T> preferredHeight(final int preferredHeight) {
    return (TemporalInputPanelBuilder<T>) super.preferredHeight(preferredHeight);
  }

  @Override
  public TemporalInputPanelBuilder<T> preferredWidth(final int preferredWidth) {
    return (TemporalInputPanelBuilder<T>) super.preferredWidth(preferredWidth);
  }

  @Override
  public TemporalInputPanelBuilder<T> preferredSize(final Dimension preferredSize) {
    return (TemporalInputPanelBuilder<T>) super.preferredSize(preferredSize);
  }

  @Override
  public TemporalInputPanelBuilder<T> transferFocusOnEnter(final boolean transferFocusOnEnter) {
    return (TemporalInputPanelBuilder<T>) super.transferFocusOnEnter(transferFocusOnEnter);
  }

  @Override
  public TemporalInputPanelBuilder<T> enabledState(final StateObserver enabledState) {
    return (TemporalInputPanelBuilder<T>) super.enabledState(enabledState);
  }

  @Override
  public TemporalInputPanelBuilder<T> onBuild(final Consumer<TemporalInputPanel<T>> onBuild) {
    return (TemporalInputPanelBuilder<T>) super.onBuild(onBuild);
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
  public TemporalInputPanel<T> build() {
    final TemporalInputPanel<T> inputPanel = createTemporalInputPanel();
    setPreferredSize(inputPanel);
    onBuild(inputPanel);
    inputPanel.getInputField().setColumns(columns);
    if (transferFocusOnEnter) {
      Components.transferFocusOnEnter(inputPanel.getInputField());
      if (inputPanel.getCalendarButton() != null) {
        Components.transferFocusOnEnter(inputPanel.getCalendarButton());
      }
    }

    return inputPanel;
  }

  private <T extends Temporal> TemporalInputPanel<T> createTemporalInputPanel() {
    if (!property.getAttribute().isTemporal()) {
      throw new IllegalArgumentException("Property " + property.getAttribute() + " is not a date or time attribute");
    }

    final TemporalField<Temporal> temporalField =
            (TemporalField<Temporal>) DefaultTextFieldBuilder.createTextField(property, enabledState);

    ComponentValues.temporalField(temporalField, updateOn).link((Value<Temporal>) value);

    return (TemporalInputPanel<T>) TemporalInputPanel.builder()
            .temporalField(temporalField)
            .calendarButton(calendarButton)
            .enabledState(enabledState)
            .build();
  }
}
