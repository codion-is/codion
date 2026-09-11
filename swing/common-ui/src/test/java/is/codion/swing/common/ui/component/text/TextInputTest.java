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
package is.codion.swing.common.ui.component.text;

import is.codion.common.reactive.state.State;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.key.KeyEvents;

import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusListener;

import static is.codion.swing.common.ui.Utilities.enabled;
import static java.awt.event.KeyEvent.VK_F5;
import static java.util.Arrays.asList;
import static javax.swing.JComponent.WHEN_FOCUSED;
import static org.junit.jupiter.api.Assertions.*;

public class TextInputTest {

	@Test
	void test() {
		TextInput panel = TextInput.builder()
						.caption("caption")
						.dialogTitle("title")
						.build();
		assertNotNull(panel.button());
		panel.textField().setText("hello");
		assertEquals("hello", panel.getText());
		panel.setText("just");
		assertEquals("just", panel.textField().getText());
	}

	@Test
	void setTextExceedMaxLength() {
		TextInput panel = TextInput.builder()
						.maximumLength(5)
						.dialogTitle("title")
						.build();
		panel.setText("12345");
		assertThrows(IllegalArgumentException.class, () -> panel.setText("123456"));
	}

	@Test
	void enabledState() throws InterruptedException {
		State enabledState = State.state();
		TextInput inputPanel = TextInput.builder()
						.build();
		enabled(enabledState, inputPanel);
		assertFalse(inputPanel.textField().isEnabled());
		assertFalse(inputPanel.button().isEnabled());
		enabledState.set(true);
		Thread.sleep(100);
		assertTrue(inputPanel.textField().isEnabled());
		assertTrue(inputPanel.button().isEnabled());
	}

	@Test
	void keyEventsAndListenersLandOnTheTextField() {
		FocusListener focusListener = new FocusAdapter() {};
		TextInput panel = TextInput.builder()
						.keyEvent(KeyEvents.builder()
										.keyCode(VK_F5)
										.action(Control.action(e -> {})))
						.focusListener(focusListener)
						.build();
		KeyStroke f5 = KeyStroke.getKeyStroke(VK_F5, 0);
		assertNull(panel.getInputMap(WHEN_FOCUSED).get(f5));
		assertNotNull(panel.textField().getInputMap(WHEN_FOCUSED).get(f5));
		assertFalse(asList(panel.getFocusListeners()).contains(focusListener));
		assertTrue(asList(panel.textField().getFocusListeners()).contains(focusListener));
	}
}
