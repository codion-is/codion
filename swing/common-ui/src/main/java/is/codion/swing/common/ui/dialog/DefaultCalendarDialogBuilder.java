/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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
    CalendarPanel.Builder calendarPanelBuilder = CalendarPanel.builder()
            .initialValue(initialDate);
    State okPressed = State.state();
    CalendarPanel calendarPanel = showCalendarDialog(calendarPanelBuilder.build(), MESSAGES.getString("select_date"), okPressed);

    return okPressed.get() ? Optional.of(calendarPanel.getLocalDate()) : Optional.empty();
  }

  @Override
  public Optional<LocalDateTime> selectLocalDateTime() {
    CalendarPanel.Builder calendarPanelBuilder = CalendarPanel.builder()
            .initialValue(initialDateTime);
    State okPressed = State.state();
    CalendarPanel calendarPanel = showCalendarDialog(calendarPanelBuilder.build(), MESSAGES.getString("select_date_time"), okPressed);

    return okPressed.get() ? Optional.of(calendarPanel.getLocalDateTime()) : Optional.empty();
  }

  private CalendarPanel showCalendarDialog(CalendarPanel calendarPanel, String title, State okPressed) {
    new DefaultOkCancelDialogBuilder(calendarPanel)
            .owner(owner)
            .locationRelativeTo(locationRelativeTo)
            .title(title)
            .onShown(dialog -> calendarPanel.requestCurrentDayButtonFocus())
            .onOk(() -> okPressed.set(true))
            .show();

    return calendarPanel;
  }
}
