/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Builds a dialog for displaying a calendar for date/time input.
 */
public interface CalendarDialogBuilder extends DialogBuilder<CalendarDialogBuilder> {

  /**
   * @param initialValue the initial value
   * @return this builder instance
   */
  CalendarDialogBuilder initialValue(LocalDate initialValue);

  /**
   * @param initialValue the initial value
   * @return this builder instance
   */
  CalendarDialogBuilder initialValue(LocalDateTime initialValue);

  /**
   * Retrieves a LocalDate from the user.
   * @return a LocalDate from the user, {@link Optional#empty()} in case the user cancels
   */
  Optional<LocalDate> selectDate();

  /**
   * Retrieves a LocalTimeDate from the user.
   * @return a LocalTimeDate from the user, {@link Optional#empty()} in case the user cancels
   */
  Optional<LocalDateTime> selectDateTime();
}
