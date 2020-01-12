/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.swing.common.ui.TemporalInputPanel;

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

  /** {@inheritDoc} */
  @Override
  public V get() {
    try {
      final String dateText = getComponent().getInputField().getText();
      if (dateText.length() == 0) {
        return null;
      }
      if (!dateText.contains("_")) {
        return getComponent().getTemporal();
      }
      else {
        return null;
      }
    }
    catch (final DateTimeParseException e) {
      throw new IllegalArgumentException("Wrong date format " + getComponent().getDateFormat() + " expected", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void setInternal(final V value) {
    getComponent().setTemporal(value);
  }
}
