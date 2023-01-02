/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
  public Optional<LocalDate> selectDate() {
    CalendarPanel calendarPanel = CalendarPanel.dateCalendarPanel();
    if (initialDate != null) {
      calendarPanel.setDate(initialDate);
    }
    State okPressed = State.state();
    new DefaultOkCancelDialogBuilder(calendarPanel)
            .owner(owner)
            .locationRelativeTo(locationRelativeTo)
            .title(MESSAGES.getString("select_date"))
            .onShown(dialog -> calendarPanel.requestCurrentDayButtonFocus())
            .onOk(() -> okPressed.set(true))
            .show();

    return okPressed.get() ? Optional.of(calendarPanel.getDate()) : Optional.empty();
  }

  @Override
  public Optional<LocalDateTime> selectDateTime() {
    CalendarPanel calendarPanel = CalendarPanel.dateTimeCalendarPanel();
    if (initialDateTime != null) {
      calendarPanel.setDateTime(initialDateTime);
    }
    State okPressed = State.state();
    new DefaultOkCancelDialogBuilder(calendarPanel)
            .owner(owner)
            .locationRelativeTo(locationRelativeTo)
            .title(MESSAGES.getString("select_date_time"))
            .onShown(dialog -> calendarPanel.requestCurrentDayButtonFocus())
            .onOk(() -> okPressed.set(true))
            .show();

    return okPressed.get() ? Optional.of(calendarPanel.getDateTime()) : Optional.empty();
  }
}
