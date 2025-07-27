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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.swing.common.ui.component.calendar.CalendarPanel;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.util.ResourceBundle.getBundle;

final class DefaultCalendarDialogBuilder extends AbstractDialogBuilder<CalendarDialogBuilder> implements CalendarDialogBuilder {

	private static final MessageBundle MESSAGES =
					messageBundle(DefaultCalendarDialogBuilder.class, getBundle(DefaultCalendarDialogBuilder.class.getName()));

	private @Nullable LocalDate initialDate;
	private @Nullable LocalDateTime initialDateTime;

	@Override
	public CalendarDialogBuilder value(@Nullable LocalDate value) {
		this.initialDate = value;
		if (value != null) {
			this.initialDateTime = value.atStartOfDay();
		}
		return this;
	}

	@Override
	public CalendarDialogBuilder value(@Nullable LocalDateTime value) {
		this.initialDateTime = value;
		if (value != null) {
			this.initialDate = value.toLocalDate();
		}
		return this;
	}

	@Override
	public Optional<LocalDate> selectLocalDate() {
		CalendarPanel.Builder calendarPanelBuilder = CalendarPanel.builder()
						.value(initialDate);
		State okPressed = State.state();
		CalendarPanel calendarPanel = showCalendarDialog(calendarPanelBuilder.build(), MESSAGES.getString("select_date"), okPressed);

		return okPressed.get() ? calendarPanel.date().optional() : Optional.empty();
	}

	@Override
	public Optional<LocalDateTime> selectLocalDateTime() {
		CalendarPanel.Builder calendarPanelBuilder = CalendarPanel.builder()
						.value(initialDateTime);
		State okPressed = State.state();
		CalendarPanel calendarPanel = showCalendarDialog(calendarPanelBuilder.build(), MESSAGES.getString("select_date_time"), okPressed);

		return okPressed.get() ? calendarPanel.dateTime().optional() : Optional.empty();
	}

	private CalendarPanel showCalendarDialog(CalendarPanel calendarPanel, String title, State okPressed) {
		OkCancelDialogBuilder dialogBuilder = new DefaultOkCancelDialogBuilder()
						.component(calendarPanel)
						.owner(owner)
						.locationRelativeTo(locationRelativeTo)
						.title(title)
						.onBuild(dialog ->
										calendarPanel.doubleClicked().addListener(() -> {
											okPressed.set(true);
											dialog.dispose();
										}))
						.onShown(dialog -> calendarPanel.requestInputFocus())
						.onOk(() -> okPressed.set(true));
		onBuildConsumers.forEach(dialogBuilder::onBuild);

		dialogBuilder.show();

		return calendarPanel;
	}
}
