/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
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
  Optional<LocalDate> selectLocalDate();

  /**
   * Retrieves a LocalTimeDate from the user.
   * @return a LocalTimeDate from the user, {@link Optional#empty()} in case the user cancels
   */
  Optional<LocalDateTime> selectLocalDateTime();
}
