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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.text.TextComponentBuilder.CaretPosition;

import org.junit.jupiter.api.Test;

import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public final class CaretPositionFocusListenerTest {

	private static final String FLATLAF_SELECT_ALL_ON_FOCUS_POLICY = "JTextField.selectAllOnFocusPolicy";

	@Test
	void temporalFieldStart() {
		TemporalField<LocalDate> field = temporalField().build();
		field.set(LocalDate.of(2025, 1, 2));
		assertEquals(10, field.getCaretPosition());
		focusGained(field);
		assertEquals(0, field.getCaretPosition());
		assertEquals("never", field.getClientProperty(FLATLAF_SELECT_ALL_ON_FOCUS_POLICY));
	}

	@Test
	void mousePressed() {
		TemporalField<LocalDate> field = temporalField().build();
		field.setCaretPosition(7);
		// The press positions the caret, the focus gained following it leaves it there
		mousePressed(field);
		focusGained(field);
		assertEquals(7, field.getCaretPosition());
		// The next focus gained, by the keyboard, positions it
		focusLost(field, false);
		focusGained(field);
		assertEquals(0, field.getCaretPosition());
	}

	@Test
	void temporaryLoss() {
		TemporalField<LocalDate> field = temporalField().build();
		field.setCaretPosition(7);
		focusLost(field, true);
		focusGained(field);
		assertEquals(7, field.getCaretPosition());
		focusLost(field, false);
		focusGained(field);
		assertEquals(0, field.getCaretPosition());
	}

	@Test
	void selectAllTakesPrecedence() {
		TemporalField<LocalDate> field = temporalField()
						.selectAllOnFocusGained(true)
						.build();
		assertFalse(caretPositionListener(field));
		focusGained(field);
		assertEquals(0, field.getSelectionStart());
		assertEquals(10, field.getSelectionEnd());
		assertEquals("never", field.getClientProperty(FLATLAF_SELECT_ALL_ON_FOCUS_POLICY));
	}

	@Test
	void textField() {
		JTextField field = Components.stringField()
						.value("text")
						.build();
		assertFalse(caretPositionListener(field));
		assertEquals("never", field.getClientProperty(FLATLAF_SELECT_ALL_ON_FOCUS_POLICY));

		field = Components.stringField()
						.value("text")
						.caretPositionOnFocusGained(CaretPosition.END)
						.build();
		assertEquals(0, field.getCaretPosition());
		focusGained(field);
		assertEquals(4, field.getCaretPosition());

		field = Components.stringField()
						.value("text")
						.caretPositionOnFocusGained(CaretPosition.END)
						.caretPositionOnFocusGained(null)
						.build();
		assertFalse(caretPositionListener(field));
	}

	@Test
	void maskedTextField() {
		JFormattedTextField field = Components.maskedTextField()
						.mask("##-##")
						.build();
		field.setText("12-34");
		focusGained(field);
		assertEquals(0, field.getCaretPosition());
		assertEquals("never", field.getClientProperty(FLATLAF_SELECT_ALL_ON_FOCUS_POLICY));
	}

	private static TemporalField.Builder<LocalDate> temporalField() {
		return TemporalField.builder()
						.type(LocalDate.class)
						.dateTimePattern("dd-MM-yyyy")
						.value(LocalDate.of(2026, 11, 21));
	}

	private static boolean caretPositionListener(JTextComponent component) {
		return Arrays.stream(component.getFocusListeners()).anyMatch(CaretPositionFocusListener.class::isInstance);
	}

	// Only the builder's listeners, the caret and the look and feel listen as well

	private static void focusGained(JTextComponent component) {
		FocusEvent event = new FocusEvent(component, FocusEvent.FOCUS_GAINED);
		for (FocusListener listener : component.getFocusListeners()) {
			if (listener instanceof CaretPositionFocusListener || listener instanceof SelectAllFocusListener) {
				listener.focusGained(event);
			}
		}
	}

	private static void focusLost(JTextComponent component, boolean temporary) {
		FocusEvent event = new FocusEvent(component, FocusEvent.FOCUS_LOST, temporary);
		for (FocusListener listener : component.getFocusListeners()) {
			if (listener instanceof CaretPositionFocusListener) {
				listener.focusLost(event);
			}
		}
	}

	private static void mousePressed(JTextComponent component) {
		MouseEvent event = new MouseEvent(component, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
						0, 5, 5, 1, false, MouseEvent.BUTTON1);
		for (MouseListener listener : component.getMouseListeners()) {
			if (listener instanceof CaretPositionFocusListener) {
				listener.mousePressed(event);
			}
		}
	}
}
