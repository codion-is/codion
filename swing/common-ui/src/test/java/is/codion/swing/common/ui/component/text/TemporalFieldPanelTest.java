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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.state.State;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static is.codion.swing.common.ui.Utilities.linkToEnabledState;
import static org.junit.jupiter.api.Assertions.*;

public class TemporalFieldPanelTest {

	@Test
	void setText() {
		TemporalFieldPanel<LocalDate> panel = TemporalFieldPanel.builder(LocalDate.class, "dd.MM.yyyy").build();
		panel.temporalField().setText("01.03.2010");
		assertEquals(LocalDate.parse("01.03.2010", DateTimeFormatter.ofPattern("dd.MM.yyyy")), panel.getTemporal());
	}

	@Test
	void setTemporal() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		TemporalFieldPanel<LocalDate> panel = TemporalFieldPanel.builder(LocalDate.class, "dd.MM.yyyy").build();
		panel.setTemporal(LocalDate.parse("03.04.2010", formatter));
		assertEquals("03.04.2010", panel.temporalField().getText());
		panel.setTemporal(null);
		assertEquals("__.__.____", panel.temporalField().getText());
	}

	@Test
	void getTemporal() {
		TemporalFieldPanel<LocalDate> panel = TemporalFieldPanel.builder(LocalDate.class, "dd.MM.yyyy").build();
		assertFalse(panel.optional().isPresent());
		panel.temporalField().setText("03");
		assertFalse(panel.optional().isPresent());
		panel.temporalField().setText("03.04");
		assertFalse(panel.optional().isPresent());
		panel.temporalField().setText("03.04.2010");
		assertNotNull(panel.getTemporal());
	}

	@Test
	void unsupportedType() {
		assertThrows(IllegalArgumentException.class, () -> TemporalFieldPanel.builder(LocalTime.class, "hh:MM"));
	}

	@Test
	void constructorNullInputField() {
		assertThrows(NullPointerException.class, () -> new TemporalFieldPanel<>(null));
	}

	@Test
	void enabledState() throws InterruptedException {
		State enabledState = State.state();
		TemporalFieldPanel<LocalDate> inputPanel = TemporalFieldPanel.builder(LocalDate.class, "dd.MM.yyyy").build();
		linkToEnabledState(enabledState, inputPanel);
		assertFalse(inputPanel.temporalField().isEnabled());
		JButton calendarButton = inputPanel.calendarButton();
		assertFalse(calendarButton.isEnabled());
		enabledState.set(true);
		Thread.sleep(100);
		assertTrue(calendarButton.isEnabled());
	}
}
