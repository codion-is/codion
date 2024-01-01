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
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.state.State;
import is.codion.swing.common.ui.component.calendar.CalendarPanel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;

final class DefaultCalendarDialogBuilder extends AbstractDialogBuilder<CalendarDialogBuilder> implements CalendarDialogBuilder {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(DefaultCalendarDialogBuilder.class.getName());

  private LocalDate initialDate;
  private LocalDateTime initialDateTime;

  @Override
  public CalendarDialogBuilder initialValue(LocalDate initialValue) {
    this.initialDate = initialValue;
    if (initialValue != null) {
      this.initialDateTime = initialValue.atStartOfDay();
    }
    return this;
  }

  @Override
  public CalendarDialogBuilder initialValue(LocalDateTime initialValue) {
    this.initialDateTime = initialValue;
    if (initialValue != null) {
      this.initialDate = initialValue.toLocalDate();
    }
    return this;
  }

  @Override
  public Optional<LocalDate> selectLocalDate() {
    CalendarPanel.Builder calendarPanelBuilder = CalendarPanel.builder();
    if (initialDate != null) {
      calendarPanelBuilder.initialValue(initialDate);
    }
    CalendarPanel calendarPanel = calendarPanelBuilder.build();
    State okPressed = State.state();
    new DefaultOkCancelDialogBuilder(calendarPanel)
            .owner(owner)
            .locationRelativeTo(locationRelativeTo)
            .title(MESSAGES.getString("select_date"))
            .onShown(dialog -> calendarPanel.requestCurrentDayButtonFocus())
            .onOk(() -> okPressed.set(true))
            .show();

    return okPressed.get() ? Optional.of(calendarPanel.getLocalDate()) : Optional.empty();
  }

  @Override
  public Optional<LocalDateTime> selectLocalDateTime() {
    CalendarPanel.Builder calendarPanelBuilder = CalendarPanel.builder();
    if (initialDateTime != null) {
      calendarPanelBuilder.initialValue(initialDateTime);
    }
    CalendarPanel calendarPanel = calendarPanelBuilder.build();
    State okPressed = State.state();
    new DefaultOkCancelDialogBuilder(calendarPanel)
            .owner(owner)
            .locationRelativeTo(locationRelativeTo)
            .title(MESSAGES.getString("select_date_time"))
            .onShown(dialog -> calendarPanel.requestCurrentDayButtonFocus())
            .onOk(() -> okPressed.set(true))
            .show();

    return okPressed.get() ? Optional.of(calendarPanel.getLocalDateTime()) : Optional.empty();
  }
}
