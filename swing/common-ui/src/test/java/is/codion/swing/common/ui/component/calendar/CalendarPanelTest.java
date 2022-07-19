/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.calendar;

import is.codion.common.event.EventDataListener;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class CalendarPanelTest {

  @Test
  void events() {
    AtomicInteger dateChangedCounter = new AtomicInteger();
    AtomicInteger dateTimeChangedCounter = new AtomicInteger();
    LocalDateTime startDate = LocalDateTime.of(2021, 8, 21, 16, 30);
    CalendarPanel panel = CalendarPanel.dateTimeCalendarPanel();
    panel.setDateTime(startDate);

    EventDataListener<LocalDate> dataListener = date -> dateChangedCounter.incrementAndGet();
    EventDataListener<LocalDateTime> dateTimeListener = dateTime -> dateTimeChangedCounter.incrementAndGet();

    panel.addDateListener(dataListener);
    panel.addDateTimeListener(dateTimeListener);

    panel.setDate(startDate.toLocalDate());

    assertEquals(0, dateChangedCounter.get());
    assertEquals(2, dateTimeChangedCounter.get());//time gets truncated, once for hours, again for minutes

    panel.setDateTime(startDate.minus(1, ChronoUnit.MINUTES));

    assertEquals(0, dateChangedCounter.get());
    assertEquals(4, dateTimeChangedCounter.get());//again, once for hours, again for minutes

    panel.setDateTime(startDate.with(ChronoField.MONTH_OF_YEAR, 7));

    assertEquals(1, dateChangedCounter.get());

    panel.setDateTime(startDate.with(ChronoField.MONTH_OF_YEAR, 7).with(ChronoField.DAY_OF_MONTH, 18));

    assertEquals(2, dateChangedCounter.get());

    panel.removeDateListener(dataListener);
    panel.removeDateTimeListener(dateTimeListener);
  }

  @Test
  void navigation() {
    LocalDateTime startDate = LocalDateTime.of(2021, 8, 21, 16, 30);
    CalendarPanel panel = CalendarPanel.dateTimeCalendarPanel();
    panel.setDateTime(startDate);

    panel.previousYear();
    assertEquals(startDate.with(ChronoField.YEAR, 2020), panel.getDateTime());
    panel.nextYear();
    assertEquals(startDate, panel.getDateTime());

    panel.previousMonth();
    assertEquals(startDate.with(ChronoField.MONTH_OF_YEAR, 7), panel.getDateTime());
    panel.nextMonth();
    assertEquals(startDate, panel.getDateTime());

    panel.previousWeek();
    assertEquals(startDate.with(ChronoField.DAY_OF_MONTH, 14), panel.getDateTime());
    panel.nextWeek();
    assertEquals(startDate, panel.getDateTime());

    panel.previousDay();
    assertEquals(startDate.with(ChronoField.DAY_OF_MONTH, 20), panel.getDateTime());
    panel.nextDay();
    assertEquals(startDate, panel.getDateTime());

    panel.previousHour();
    assertEquals(startDate.with(ChronoField.HOUR_OF_DAY, 15), panel.getDateTime());
    panel.nextHour();
    assertEquals(startDate, panel.getDateTime());

    panel.previousMinute();
    assertEquals(startDate.with(ChronoField.MINUTE_OF_HOUR, 29), panel.getDateTime());
    panel.nextMinute();
    assertEquals(startDate, panel.getDateTime());

    LocalDateTime date = LocalDateTime.of(2021, 12, 31, 23, 59);
    panel.setDateTime(date);

    panel.nextHour();
    assertEquals(date.withYear(2022).withMonth(1).withDayOfMonth(1).withHour(0).withMinute(59), panel.getDateTime());
    panel.previousHour();
    assertEquals(date, panel.getDateTime());

    panel.nextMinute();
    assertEquals(date.withYear(2022).withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0), panel.getDateTime());
    panel.previousMinute();
    assertEquals(date, panel.getDateTime());
  }

  @Test
  void dateOnly() {
    LocalDateTime startDate = LocalDateTime.of(2021, 8, 21, 16, 30);

    CalendarPanel panel = CalendarPanel.dateCalendarPanel();
    panel.setDateTime(startDate);

    assertEquals(startDate.withHour(0).withMinute(0), panel.getDateTime());
  }
}
