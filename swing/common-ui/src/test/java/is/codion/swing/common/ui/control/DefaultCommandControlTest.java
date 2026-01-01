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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.model.CancelException;
import is.codion.common.reactive.state.State;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;

import static is.codion.swing.common.ui.component.Components.button;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultCommandControlTest {

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
		Font font = new JButton().getFont().deriveFont(Font.ITALIC);
		Control control = Control.builder()
						.command(this::method)
						.enabled(enabledState)
						.foreground(Color.RED)
						.background(Color.BLACK)
						.font(font)
						.build();
		JButton button = button().control(control).build();
		assertFalse(button.isEnabled());
		SwingUtilities.invokeLater(() -> {
			enabledState.set(true);
			assertTrue(button.isEnabled());
			button.doClick();
			assertEquals(1, callCount);
			assertEquals(Color.RED, button.getForeground());
			assertEquals(Color.BLACK, button.getBackground());
			assertEquals(font, button.getFont());
			assertThrows(UnsupportedOperationException.class, () -> control.putValue("test", "test"));
		});
	}

	@Test
	void basics() {
		Control test = Control.builder()
						.command(this::doNothing)
						.caption("test")
						.description("description")
						.mnemonic(10)
						.build();
		assertEquals("test", test.toString());
		assertEquals("test", test.caption().orElse(null));
		assertEquals(10, test.mnemonic().orElse(0));
		assertFalse(test.smallIcon().isPresent());
		assertEquals("description", test.description().orElse(null));
		test.actionPerformed(null);
	}

	@Test
	void actionCommand() {
		ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "test");
		Control test = Control.action(actionEvent -> {
			assertSame(this, actionEvent.getSource());
			assertEquals("test", actionEvent.getActionCommand());
			assertEquals(ActionEvent.ACTION_PERFORMED, actionEvent.getID());
		});
		test.actionPerformed(event);
	}

	@Test
	void setEnabled() {
		State enabledState = State.state();
		Control control = Control.builder().command(this::doNothing).caption("control").enabled(enabledState.observable()).build();
		assertEquals("control", control.caption().orElse(null));
		assertSame(enabledState.observable(), control.enabled());
		assertFalse(control.isEnabled());
		SwingUtilities.invokeLater(() -> {
			enabledState.set(true);
			assertTrue(control.isEnabled());
			enabledState.set(false);
			assertFalse(control.isEnabled());
		});
	}

	@Test
	void setEnabledViaMethod() {
		Control test = Control.command(this::doNothing);
		assertThrows(UnsupportedOperationException.class, () -> test.setEnabled(true));
	}

	@Test
	void exceptionOnExecute() {
		Control control = Control.command(this::errorMethod);
		assertThrows(RuntimeException.class, () -> control.actionPerformed(null));
	}

	@Test
	void runtimeExceptionOnExecute() {
		Control control = Control.command(this::runtimeErrorMethod);
		assertThrows(RuntimeException.class, () -> control.actionPerformed(null));
	}

	@Test
	void cancelOnExecute() {
		Control control = Control.command(this::cancelMethod);
		control.actionPerformed(null);
	}

	@Test
	void copy() {
		State enabled = State.state();
		Control control = Control.builder()
						.command(() -> {})
						.enabled(enabled)
						.caption("name")
						.description("desc")
						.mnemonic('n')
						.value("key", "value")
						.build();
		Control copy = control.copy()
						.caption("new name")
						.description("new desc")
						.value("key", "newvalue")
						.build();

		assertFalse(control.isEnabled());
		assertFalse(copy.isEnabled());

		SwingUtilities.invokeLater(() -> {
			enabled.set(true);

			assertTrue(control.isEnabled());
			assertTrue(copy.isEnabled());

			assertNotEquals(control.caption().orElse(null), copy.caption().orElse(null));
			assertNotEquals(control.description().orElse(null), copy.description().orElse(null));
			assertEquals(control.mnemonic().orElse(0), copy.mnemonic().orElse(1));
			assertNotEquals(control.getValue("key"), copy.getValue("key"));
		});
	}

	private void doNothing() {}
}
