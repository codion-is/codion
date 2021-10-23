/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.textfield.TemporalInputPanel;

import java.time.temporal.Temporal;

/**
 * A InputProvider for Temporal value input
 * @param <T> the Temporal type
 */
final class TemporalInputPanelValue<T extends Temporal> extends AbstractComponentValue<T, TemporalInputPanel<T>> {

  /**
   * Instantiates a new {@link TemporalInputPanelValue} for {@link Temporal} values.
   * @param inputPanel the input panel to use
   */
  TemporalInputPanelValue(final TemporalInputPanel<T> inputPanel) {
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
