/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.time.TemporalInputPanel;
import is.codion.swing.common.ui.value.UpdateOn;

import java.time.temporal.Temporal;

/**
 * Builds a TemporalInputPanel.
 * @param <T> the temporal type
 */
public interface TemporalInputPanelBuilder<T extends Temporal> extends ComponentBuilder<T, TemporalInputPanel<T>, TemporalInputPanelBuilder<T>> {

  /**
   * @param columns the number of colums in the temporal field
   * @return this builder instance
   */
  TemporalInputPanelBuilder<T> columns(int columns);

  /**
   * @param updateOn specifies when the underlying value should be updated
   * @return this builder instance
   */
  TemporalInputPanelBuilder<T> updateOn(UpdateOn updateOn);

  /**
   * @param dateTimePattern the date time pattern, if applicable
   * @return this builder instance
   */
  TemporalInputPanelBuilder<T> dateTimePattern(String dateTimePattern);

  /**
   * @param calendarButton true if a calendar button should be included (may not be supported)
   * @return this builder instance
   */
  TemporalInputPanelBuilder<T> calendarButton(boolean calendarButton);
}
