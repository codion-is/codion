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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.event.Event;
import is.codion.common.model.CancelException;
import is.codion.common.state.State;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;

import static is.codion.swing.common.ui.component.Components.button;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultControlTest {

	private int callCount = 0;

	void method() {
		callCount++;
	}

	void errorMethod() throws Exception {
		throw new Exception("test");
	}

	void runtimeErrorMethod() {
		throw new RuntimeException("test");
	}

	void cancelMethod() {
		throw new CancelException();
	}

	@Test
	void test() {
		State enabledState = State.state();
		Control control = Control.builder(this::method).enabled(enabledState).build();
		JButton button = button(control).build();
		assertFalse(button.isEnabled());
		enabledState.set(true);
		assertTrue(button.isEnabled());
		button.doClick();
		assertEquals(1, callCount);
		control.setForeground(Color.RED);
		assertEquals(button.getForeground(), Color.RED);
		control.setBackground(Color.BLACK);
		assertEquals(button.getBackground(), Color.BLACK);
		Font font = button.getFont().deriveFont(Font.ITALIC);
		control.setFont(font);
		assertEquals(button.getFont(), font);
	}

	@Test
	void eventControl() {
		State state = State.state();
		Event<ActionEvent> event = Event.event();
		event.addListener(() -> state.set(true));
		Control.eventControl(event).actionPerformed(null);
		assertTrue(state.get());
	}

	@Test
	void basics() {
		Control test = Control.control(this::doNothing);
		test.setName("test");
		assertEquals("test", test.toString());
		assertEquals("test", test.getName());
		assertEquals(0, test.getMnemonic());
		test.setMnemonic(10);
		assertEquals(10, test.getMnemonic());
		assertNull(test.getSmallIcon());
		test.setKeyStroke(null);
		test.setDescription("description");
		assertEquals("description", test.getDescription());
		test.actionPerformed(null);
	}

	@Test
	void actionCommand() {
		ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "test");
		Control test = Control.actionControl(actionEvent -> {
			assertSame(this, actionEvent.getSource());
			assertEquals(actionEvent.getActionCommand(), "test");
			assertEquals(actionEvent.getID(), ActionEvent.ACTION_PERFORMED);
		});
		assertInstanceOf(DefaultActionControl.class, test);
		test.actionPerformed(event);
	}

	@Test
	void setEnabled() {
		State enabledState = State.state();
		Control control = Control.builder(this::doNothing).name("control").enabled(enabledState.observer()).build();
		assertEquals("control", control.getName());
		assertEquals(enabledState.observer(), control.enabled());
		assertFalse(control.isEnabled());
		enabledState.set(true);
		assertTrue(control.isEnabled());
		enabledState.set(false);
		assertFalse(control.isEnabled());
	}

	@Test
	void setEnabledViaMethod() {
		Control test = Control.control(this::doNothing);
		assertThrows(UnsupportedOperationException.class, () -> test.setEnabled(true));
	}

	@Test
	void exceptionOnExecute() {
		Control control = Control.control(this::errorMethod);
		assertThrows(RuntimeException.class, () -> control.actionPerformed(null));
	}

	@Test
	void runtimeExceptionOnExecute() {
		Control control = Control.control(this::runtimeErrorMethod);
		assertThrows(RuntimeException.class, () -> control.actionPerformed(null));
	}

	@Test
	void cancelOnExecute() {
		Control control = Control.control(this::cancelMethod);
		control.actionPerformed(null);
	}

	@Test
	void copy() {
		State enabled = State.state();
		Control control = Control.builder(() -> {})
						.enabled(enabled)
						.name("name")
						.description("desc")
						.mnemonic('n')
						.value("key", "value")
						.build();
		Control copy = control.copy(() -> {})
						.name("new name")
						.description("new desc")
						.value("key", "newvalue")
						.build();

		assertFalse(control.isEnabled());
		assertFalse(copy.isEnabled());

		enabled.set(true);

		assertTrue(control.isEnabled());
		assertTrue(copy.isEnabled());

		assertNotEquals(control.getName(), copy.getName());
		assertNotEquals(control.getDescription(), copy.getDescription());
		assertEquals(control.getMnemonic(), copy.getMnemonic());
		assertNotEquals(control.getValue("key"), copy.getValue("key"));
	}

	private void doNothing() {}
}
