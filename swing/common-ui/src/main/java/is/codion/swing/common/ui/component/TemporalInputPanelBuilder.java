/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.TemporalInputPanel;
import is.codion.swing.common.ui.textfield.TemporalInputPanel.CalendarProvider;

import java.time.temporal.Temporal;

/**
 * Builds a TemporalInputPanel.
 * @param <T> the temporal type
 */
public interface TemporalInputPanelBuilder<T extends Temporal> extends ComponentBuilder<T, TemporalInputPanel<T>, TemporalInputPanelBuilder<T>> {

  /**
   * @param selectAllOnFocusGained if true the component will select contents on focus gained
   * @return this builder instance
   */
  TemporalInputPanelBuilder<T> selectAllOnFocusGained(boolean selectAllOnFocusGained);

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
   * @param calendarProvider the calendar provider to use for calendar input
   * @return this builder instance
   */
  TemporalInputPanelBuilder<T> calendarProvider(CalendarProvider calendarProvider);
}
