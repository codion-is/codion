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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.reactive.state.State;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.key.KeyEvents;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusListener;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static is.codion.swing.common.ui.Utilities.enabled;
import static java.awt.event.KeyEvent.VK_F5;
import static java.util.Arrays.asList;
import static javax.swing.JComponent.WHEN_FOCUSED;
import static org.junit.jupiter.api.Assertions.*;

public class TemporalInputTest {

	@Test
	void setText() {
		TemporalInput<LocalDate> panel = TemporalInput.builder()
						.temporalClass(LocalDate.class)
						.dateTimePattern("dd.MM.yyyy")
						.build();
		panel.temporalField().setText("01.03.2010");
		assertEquals(LocalDate.parse("01.03.2010", DateTimeFormatter.ofPattern("dd.MM.yyyy")), panel.get());
	}

	@Test
	void set() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		TemporalInput<LocalDate> panel = TemporalInput.builder()
						.temporalClass(LocalDate.class)
						.dateTimePattern("dd.MM.yyyy")
						.build();
		panel.set(LocalDate.parse("03.04.2010", formatter));
		assertEquals("03.04.2010", panel.temporalField().getText());
		panel.set(null);
		assertEquals("__.__.____", panel.temporalField().getText());
	}

	@Test
	void get() {
		TemporalInput<LocalDate> panel = TemporalInput.builder()
						.temporalClass(LocalDate.class)
						.dateTimePattern("dd.MM.yyyy")
						.build();
		assertFalse(panel.optional().isPresent());
		panel.temporalField().setText("03");
		assertFalse(panel.optional().isPresent());
		panel.temporalField().setText("03.04");
		assertFalse(panel.optional().isPresent());
		panel.temporalField().setText("03.04.2010");
		assertNotNull(panel.get());
	}

	@Test
	void unsupportedType() {
		assertThrows(IllegalArgumentException.class, () -> TemporalInput.builder()
						.temporalClass(LocalTime.class));
	}

	@Test
	void constructorNullInputField() {
		assertThrows(NullPointerException.class, () -> new TemporalInput<>(null));
	}

	@Test
	void enabledState() {
		SwingUtilities.invokeLater(() -> {
			State enabledState = State.state();
			TemporalInput<LocalDate> inputPanel = TemporalInput.builder()
							.temporalClass(LocalDate.class)
							.dateTimePattern("dd.MM.yyyy")
							.build();
			enabled(enabledState, inputPanel);
			assertFalse(inputPanel.temporalField().isEnabled());
			JButton calendarButton = inputPanel.calendarButton();
			assertFalse(calendarButton.isEnabled());
			enabledState.set(true);
			assertTrue(calendarButton.isEnabled());
		});
	}

	@Test
	void keyEventsAndListenersLandOnTheTemporalField() {
		FocusListener focusListener = new FocusAdapter() {};
		TemporalInput<LocalDate> panel = TemporalInput.builder()
						.temporalClass(LocalDate.class)
						.dateTimePattern("dd.MM.yyyy")
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_F5)
										.action(Control.action(e -> {})))
						.focusListener(focusListener)
						.build();
		KeyStroke f5 = KeyStroke.getKeyStroke(VK_F5, 0);
		assertNull(panel.getInputMap(WHEN_FOCUSED).get(f5));
		assertNotNull(panel.temporalField().getInputMap(WHEN_FOCUSED).get(f5));
		assertFalse(asList(panel.getFocusListeners()).contains(focusListener));
		assertTrue(asList(panel.temporalField().getFocusListeners()).contains(focusListener));
	}
}
