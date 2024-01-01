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
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.calendar;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class CalendarPanelTest {

  @Test
  void events() {
    AtomicInteger dateChangedCounter = new AtomicInteger();
    AtomicInteger dateTimeChangedCounter = new AtomicInteger();
    LocalDateTime startDate = LocalDateTime.of(2021, 8, 21, 16, 30);
    CalendarPanel panel = CalendarPanel.builder()
            .initialValue(startDate)
            .build();

    Consumer<LocalDate> dataListener = date -> dateChangedCounter.incrementAndGet();
    Consumer<LocalDateTime> dateTimeListener = dateTime -> dateTimeChangedCounter.incrementAndGet();

    panel.addLocalDateListener(dataListener);
    panel.addLocalDateTimeListener(dateTimeListener);

    panel.setLocalDate(startDate.toLocalDate());

    assertEquals(0, dateChangedCounter.get());
    assertEquals(2, dateTimeChangedCounter.get());//time gets truncated, once for hours, again for minutes

    panel.setLocalDateTime(startDate.minusMinutes(1));

    assertEquals(0, dateChangedCounter.get());
    assertEquals(4, dateTimeChangedCounter.get());//again, once for hours, again for minutes

    panel.setLocalDateTime(startDate.withMonth(7));

    assertEquals(1, dateChangedCounter.get());

    panel.setLocalDateTime(startDate.withMonth(7).withDayOfMonth(18));

    assertEquals(2, dateChangedCounter.get());

    panel.removeLocalDateListener(dataListener);
    panel.removeLocalDateTimeListener(dateTimeListener);
  }

  @Test
  void navigation() {
    LocalDateTime startDate = LocalDateTime.of(2021, 8, 21, 16, 30);
    CalendarPanel panel = CalendarPanel.builder()
            .initialValue(startDate)
            .build();

    panel.previousYear();
    assertEquals(startDate.withYear(2020), panel.getLocalDateTime());
    panel.nextYear();
    assertEquals(startDate, panel.getLocalDateTime());

    panel.previousMonth();
    assertEquals(startDate.withMonth(7), panel.getLocalDateTime());
    panel.nextMonth();
    assertEquals(startDate, panel.getLocalDateTime());

    panel.previousWeek();
    assertEquals(startDate.withDayOfMonth(14), panel.getLocalDateTime());
    panel.nextWeek();
    assertEquals(startDate, panel.getLocalDateTime());

    panel.previousDay();
    assertEquals(startDate.withDayOfMonth(20), panel.getLocalDateTime());
    panel.nextDay();
    assertEquals(startDate, panel.getLocalDateTime());

    panel.previousHour();
    assertEquals(startDate.withHour(15), panel.getLocalDateTime());
    panel.nextHour();
    assertEquals(startDate, panel.getLocalDateTime());

    panel.previousMinute();
    assertEquals(startDate.withMinute(29), panel.getLocalDateTime());
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

    CalendarPanel panel = CalendarPanel.builder()
            .initialValue(startDate)
            .includeTime(false)
            .build();
    panel.setLocalDateTime(startDate);

    assertEquals(startDate.withHour(0).withMinute(0), panel.getLocalDateTime());
  }
}
