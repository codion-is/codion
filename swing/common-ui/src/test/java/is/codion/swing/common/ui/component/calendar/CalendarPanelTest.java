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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.calendar;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
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
						.value(startDate)
						.build();

		Consumer<LocalDate> dateConsumer = date -> dateChangedCounter.incrementAndGet();
		Consumer<LocalDateTime> dateTimeConsumer = dateTime -> dateTimeChangedCounter.incrementAndGet();

		panel.date().addConsumer(dateConsumer);
		panel.dateTime().addConsumer(dateTimeConsumer);

		panel.date().set(startDate.toLocalDate());

		assertEquals(0, dateChangedCounter.get());
		assertEquals(2, dateTimeChangedCounter.get());//time gets truncated, once for hours, again for minutes

		panel.dateTime().set(startDate.minusMinutes(1));

		assertEquals(0, dateChangedCounter.get());
		assertEquals(4, dateTimeChangedCounter.get());//again, once for hours, again for minutes

		panel.dateTime().set(startDate.withMonth(7));

		assertEquals(1, dateChangedCounter.get());

		panel.dateTime().set(startDate.withMonth(7).withDayOfMonth(18));

		assertEquals(2, dateChangedCounter.get());

		panel.date().removeConsumer(dateConsumer);
		panel.dateTime().removeConsumer(dateTimeConsumer);
	}

	@Test
	void navigation() {
		LocalDateTime startDate = LocalDateTime.of(2021, 8, 21, 16, 30);
		CalendarPanel panel = CalendarPanel.builder()
						.value(startDate)
						.build();

		panel.previousYear();
		assertEquals(startDate.withYear(2020), panel.dateTime().get());
		panel.nextYear();
		assertEquals(startDate, panel.dateTime().get());

		panel.previousMonth();
		assertEquals(startDate.withMonth(7), panel.dateTime().get());
		panel.nextMonth();
		assertEquals(startDate, panel.dateTime().get());

		panel.previousWeek();
		assertEquals(startDate.withDayOfMonth(14), panel.dateTime().get());
		panel.nextWeek();
		assertEquals(startDate, panel.dateTime().get());

		panel.previousDay();
		assertEquals(startDate.withDayOfMonth(20), panel.dateTime().get());
		panel.nextDay();
		assertEquals(startDate, panel.dateTime().get());

		panel.previousHour();
		assertEquals(startDate.withHour(15), panel.dateTime().get());
		panel.nextHour();
		assertEquals(startDate, panel.dateTime().get());

		panel.previousMinute();
		assertEquals(startDate.withMinute(29), panel.dateTime().get());
		panel.nextMinute();
		assertEquals(startDate, panel.dateTime().get());

		LocalDateTime date = LocalDateTime.of(2021, 12, 31, 23, 59);
		panel.dateTime().set(date);

		panel.nextHour();
		assertEquals(date.withYear(2022).withMonth(1).withDayOfMonth(1).withHour(0).withMinute(59), panel.dateTime().get());
		panel.previousHour();
		assertEquals(date, panel.dateTime().get());

		panel.nextMinute();
		assertEquals(date.withYear(2022).withMonth(1).withDayOfMonth(1).withHour(0).withMinute(0), panel.dateTime().get());
		panel.previousMinute();
		assertEquals(date, panel.dateTime().get());
	}

	@Test
	void weekNumbers() {
		LocalDateTime startDate = LocalDateTime.of(2022, 6, 14, 0, 0);

		CalendarPanel panel = CalendarPanel.builder()
						.value(startDate)
						.includeWeekNumbers(true)
						.build();

		assertEquals(startDate, panel.dateTime().get());

		panel.dateTime().set(startDate.withMonth(1));
		assertEquals(startDate.withMonth(1), panel.dateTime().get());
	}

	@Test
	void customWeekConfiguration() {
		LocalDateTime startDate = LocalDateTime.of(2022, 1, 1, 0, 0);

		// Test custom week configuration
		CalendarPanel panel = CalendarPanel.builder()
						.value(startDate)
						.firstDayOfWeek(DayOfWeek.SUNDAY)
						.minimalDaysInFirstWeek(1)
						.includeWeekNumbers(true)
						.build();

		assertEquals(startDate, panel.dateTime().get());
	}
}
