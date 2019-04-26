/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.swing.common.ui.TemporalInputPanel;

import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;

/**
 * A InputProvider for Temporal value input
 * @param <T> the Temporal type
 */
public final class TemporalInputProvider<T extends Temporal> extends AbstractInputProvider<T, TemporalInputPanel<T>> {

  /**
   * Instantiates a new {@link InputProvider} for {@link Temporal} values.
   * @param inputPanel the input panel to use
   */
  public TemporalInputProvider(final TemporalInputPanel<T> inputPanel) {
    super(inputPanel);
  }

  /** {@inheritDoc} */
  @Override
  public T getValue() {
    try {
      final String dateText = getInputComponent().getInputField().getText();
      if (dateText.length() == 0) {
        return null;
      }
      if (!dateText.contains("_")) {
        return getInputComponent().getTemporal();
      }
      else {
        return null;
      }
    }
    catch (final DateTimeParseException e) {
      throw new IllegalArgumentException("Wrong date format " + getInputComponent().getDateFormat() + " expected", e);
    }
  }
}
