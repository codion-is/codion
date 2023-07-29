/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class CalendarPanelTest {

  @Test
  void events() {
    AtomicInteger dateChangedCounter = new AtomicInteger();
    AtomicInteger dateTimeChangedCounter = new AtomicInteger();
    LocalDateTime startDate = LocalDateTime.of(2021, 8, 21, 16, 30);
    CalendarPanel panel = CalendarPanel.dateTimeCalendarPanel();
    panel.setLocalDateTime(startDate);

    Consumer<LocalDate> dataListener = date -> dateChangedCounter.incrementAndGet();
    Consumer<LocalDateTime> dateTimeListener = dateTime -> dateTimeChangedCounter.incrementAndGet();

    panel.addLocalDateListener(dataListener);
    panel.addLocalDateTimeListener(dateTimeListener);

    panel.setLocalDate(startDate.toLocalDate());

    assertEquals(0, dateChangedCounter.get());
    assertEquals(2, dateTimeChangedCounter.get());//time gets truncated, once for hours, again for minutes

    panel.setLocalDateTime(startDate.minus(1, ChronoUnit.MINUTES));

    assertEquals(0, dateChangedCounter.get());
    assertEquals(4, dateTimeChangedCounter.get());//again, once for hours, again for minutes

    panel.setLocalDateTime(startDate.with(ChronoField.MONTH_OF_YEAR, 7));

    assertEquals(1, dateChangedCounter.get());

    panel.setLocalDateTime(startDate.with(ChronoField.MONTH_OF_YEAR, 7).with(ChronoField.DAY_OF_MONTH, 18));

    assertEquals(2, dateChangedCounter.get());

    panel.removeLocalDateListener(dataListener);
    panel.removeLocalDateTimeListener(dateTimeListener);
  }

  @Test
  void navigation() {
    LocalDateTime startDate = LocalDateTime.of(2021, 8, 21, 16, 30);
    CalendarPanel panel = CalendarPanel.dateTimeCalendarPanel();
    panel.setLocalDateTime(startDate);

    panel.previousYear();
    assertEquals(startDate.with(ChronoField.YEAR, 2020), panel.getLocalDateTime());
    panel.nextYear();
    assertEquals(startDate, panel.getLocalDateTime());

    panel.previousMonth();
    assertEquals(startDate.with(ChronoField.MONTH_OF_YEAR, 7), panel.getLocalDateTime());
    panel.nextMonth();
    assertEquals(startDate, panel.getLocalDateTime());

    panel.previousWeek();
    assertEquals(startDate.with(ChronoField.DAY_OF_MONTH, 14), panel.getLocalDateTime());
    panel.nextWeek();
    assertEquals(startDate, panel.getLocalDateTime());

    panel.previousDay();
    assertEquals(startDate.with(ChronoField.DAY_OF_MONTH, 20), panel.getLocalDateTime());
    panel.nextDay();
    assertEquals(startDate, panel.getLocalDateTime());

    panel.previousHour();
    assertEquals(startDate.with(ChronoField.HOUR_OF_DAY, 15), panel.getLocalDateTime());
    panel.nextHour();
    assertEquals(startDate, panel.getLocalDateTime());

    panel.previousMinute();
    assertEquals(startDate.with(ChronoField.MINUTE_OF_HOUR, 29), panel.getLocalDateTime());
    panel.nextMinute();
    assertEquals(startDate, panel.getLocalDateTime());

    LocalDateTime date = LocalDateTime.of(2021, 12, 31, 23, 59);
    panel.setLocalDateTime(date);

    panel.nextHour();
    assertEquals(date.withYear(2022).withMonth(1).withDayOfMonth(1).withHour(0).withMinute(59), panel.getLocalDateTime());
    panel.previousHour();
    assertEquals(date, panel.getLocalDateTime());

    panel.nextMinute();
    assertEquals(date.withYear(2022).withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0), panel.getLocalDateTime());
    panel.previousMinute();
    assertEquals(date, panel.getLocalDateTime());
  }

  @Test
  void dateOnly() {
    LocalDateTime startDate = LocalDateTime.of(2021, 8, 21, 16, 30);

    CalendarPanel panel = CalendarPanel.dateCalendarPanel();
    panel.setLocalDateTime(startDate);

    assertEquals(startDate.withHour(0).withMinute(0), panel.getLocalDateTime());
  }
}
