/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.textfield.TemporalInputPanel;

import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

/**
 * A InputProvider for Temporal value input
 * @param <V> the Temporal type
 */
final class TemporalInputPanelValue<V extends Temporal> extends AbstractComponentValue<V, TemporalInputPanel<V>> {

  /**
   * Instantiates a new {@link TemporalInputPanelValue} for {@link Temporal} values.
   * @param inputPanel the input panel to use
   */
  TemporalInputPanelValue(final TemporalInputPanel<V> inputPanel) {
    super(inputPanel);
  }

  @Override
  protected V getComponentValue(final TemporalInputPanel<V> component) {
    try {
      return component.getTemporal();
    }
    catch (final DateTimeParseException e) {
      throw new IllegalArgumentException("Wrong date format " + component.getDateTimePattern() + " expected", e);
    }
  }

  @Override
  protected void setComponentValue(final TemporalInputPanel<V> component, final V value) {
    component.setTemporal(value);
  }
}
