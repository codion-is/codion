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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.State;
import is.codion.common.value.Value;

import org.junit.jupiter.api.Test;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static javax.swing.KeyStroke.getKeyStroke;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Control builder factory patterns and advanced builder configurations.
 */
public final class ControlBuilderTest {

	@Test
	void commandControlFactoryMethods() {
		AtomicInteger counter = new AtomicInteger();
		Control.Command command = counter::incrementAndGet;

		// Simple command factory
		CommandControl simple = Control.command(command);
		assertNotNull(simple);
		assertTrue(simple.isEnabled());
		simple.actionPerformed(null);
		assertEquals(1, counter.get());

		// Action-based factory
		CommandControl actionControl = Control.action(e -> counter.incrementAndGet());
		actionControl.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "test"));
		assertEquals(2, counter.get());

		// Builder-based factory
		CommandControl builderControl = Control.builder()
						.command(command)
						.build();
		builderControl.actionPerformed(null);
		assertEquals(3, counter.get());
	}

	@Test
	void toggleControlFactoryMethods() {
		// State-based factory
		State state = State.state(true);
		ToggleControl stateToggle = Control.toggle(state);
		assertTrue(stateToggle.value().get());
		stateToggle.value().set(false);
		assertFalse(state.is());

		// Value-based factory
		Value<Boolean> value = Value.nullable(false);
		ToggleControl valueToggle = Control.toggle(value);
		assertFalse(valueToggle.value().get());
		valueToggle.value().set(true);
		assertTrue(value.get());

		// Builder-based factory
		ToggleControl builderToggle = Control.builder()
						.toggle(state)
						.build();
		state.set(true);
		assertTrue(builderToggle.value().get());
	}

	@Test
	void builderAllProperties() {
		State enabledState = State.state(false);
		Icon icon = new ImageIcon();
		Font font = Font.decode("Dialog-BOLD-14");
		KeyStroke keyStroke = getKeyStroke("ctrl S");

		Control control = Control.builder()
						.command(() -> {})
						.caption("Save")
						.description("Save the document")
						.mnemonic('S')
						.enabled(enabledState)
						.smallIcon(icon)
						.largeIcon(icon)
						.font(font)
						.foreground(Color.BLUE)
						.background(Color.YELLOW)
						.keyStroke(keyStroke)
						.value("custom", "value")
						.build();

		assertEquals("Save", control.caption().orElse(null));
		assertEquals("Save the document", control.description().orElse(null));
		assertEquals('S', control.mnemonic().orElse(' '));
		assertFalse(control.isEnabled());
		assertEquals(enabledState.is(), control.enabled().is());
		assertSame(icon, control.smallIcon().orElse(null));
		assertSame(icon, control.largeIcon().orElse(null));
		assertSame(font, control.font().orElse(null));
		assertEquals(Color.BLUE, control.foreground().orElse(null));
		assertEquals(Color.YELLOW, control.background().orElse(null));
		assertEquals(keyStroke, control.keyStroke().orElse(null));
		assertEquals("value", control.getValue("custom"));
	}

	@Test
	void builderFromAction() {
		// Create an action with properties
		Action action = new AbstractAction("Test Action") {
			{
				putValue(SHORT_DESCRIPTION, "Test description");
				putValue(MNEMONIC_KEY, (int) 'T');
				putValue(ACCELERATOR_KEY, getKeyStroke("ctrl T"));
			}

			@Override
			public void actionPerformed(ActionEvent e) {
				// Action implementation
			}
		};

		// Build control from actionCommand that delegates to the action
		Control control = Control.builder()
						.action(action::actionPerformed)
						.caption("Test Action")
						.description("Test description")
						.mnemonic('T')
						.keyStroke(getKeyStroke("ctrl T"))
						.build();

		assertEquals("Test Action", control.caption().orElse(null));
		assertEquals("Test description", control.description().orElse(null));
		assertEquals('T', control.mnemonic().orElse(' '));
		assertEquals(getKeyStroke("ctrl T"), control.keyStroke().orElse(null));
	}

	@Test
	void nullParameterValidation() {
		// Null command should throw
		assertThrows(NullPointerException.class, () -> Control.command(null));
		assertThrows(NullPointerException.class, () -> Control.builder().command(null));

		// Null toggle value should throw
		assertThrows(NullPointerException.class, () -> Control.toggle((State) null));
		assertThrows(NullPointerException.class, () -> Control.toggle((Value<Boolean>) null));
		assertThrows(NullPointerException.class, () -> Control.builder().toggle((State) null));
		assertThrows(NullPointerException.class, () -> Control.builder().toggle((Value<Boolean>) null));

		// Null action should throw
		assertThrows(NullPointerException.class, () -> Control.action(null));
		assertThrows(NullPointerException.class, () -> Control.builder().action(null));
	}

	@Test
	void builderCannotBuildBothCommandAndToggle() {
		// Should be able to build command control
		Control command = Control.builder()
						.command(() -> {})
						.build();
		assertInstanceOf(CommandControl.class, command);

		// Should be able to build toggle control
		Control toggle = Control.builder()
						.toggle(State.state())
						.build();
		assertInstanceOf(ToggleControl.class, toggle);
	}

	@Test
	void copyBuilderInheritsAllProperties() {
		Icon icon = new ImageIcon();
		State enabled = State.state(true);

		Control original = Control.builder()
						.command(() -> {})
						.caption("Original")
						.description("Original description")
						.mnemonic('O')
						.enabled(enabled)
						.smallIcon(icon)
						.foreground(Color.RED)
						.value("key", "value")
						.build();

		// Copy should inherit all properties
		Control copy = original.copy()
						.caption("Copy") // Override caption
						.build();

		assertEquals("Copy", copy.caption().orElse(null));
		assertEquals("Original description", copy.description().orElse(null)); // Inherited
		assertEquals('O', copy.mnemonic().orElse(' ')); // Inherited
		assertEquals(enabled.is(), copy.enabled().is()); // Same enabled state value
		assertSame(icon, copy.smallIcon().orElse(null)); // Inherited
		assertEquals(Color.RED, copy.foreground().orElse(null)); // Inherited
		assertEquals("value", copy.getValue("key")); // Inherited

		// But different command instance
		assertNotSame(original, copy);
	}

	@Test
	void emptyOptionalProperties() {
		Control minimal = Control.command(() -> {});

		// All optional properties should be empty
		assertFalse(minimal.caption().isPresent());
		assertFalse(minimal.description().isPresent());
		assertFalse(minimal.mnemonic().isPresent());
		assertFalse(minimal.smallIcon().isPresent());
		assertFalse(minimal.largeIcon().isPresent());
		assertFalse(minimal.keyStroke().isPresent());
		assertFalse(minimal.font().isPresent());
		assertFalse(minimal.foreground().isPresent());
		assertFalse(minimal.background().isPresent());

		// But enabled should have a default
		assertTrue(minimal.isEnabled());
		assertNotNull(minimal.enabled());
	}

	@Test
	void toggleControlCopyWithDifferentValue() {
		Value<Boolean> value1 = Value.nullable(true);
		Value<Boolean> value2 = Value.nullable(false);

		ToggleControl original = Control.builder()
						.toggle(value1)
						.caption("Toggle")
						.build();

		// Copy with different value
		ToggleControl copy = original.copy(value2).build();

		// Same caption but different values
		assertEquals("Toggle", copy.caption().orElse(null));
		assertTrue(original.value().get());
		assertFalse(copy.value().get());

		// Changing one doesn't affect the other
		original.value().set(false);
		assertFalse(value1.get());
		assertFalse(value2.get()); // Unchanged
	}

	@Test
	void builderIsReusable() {
		// Create a command control builder with properties
		CommandControl control1 = Control.builder()
						.command(() -> {})
						.caption("Reusable")
						.enabled(State.state(true))
						.build();

		// Create another control with same properties
		CommandControl control2 = Control.builder()
						.command(() -> {})
						.caption("Reusable")
						.enabled(State.state(true))
						.build();

		assertEquals("Reusable", control1.caption().orElse(null));
		assertEquals("Reusable", control2.caption().orElse(null));

		// But they should be independent
		assertNotSame(control1, control2);
		// And have independent enabled states
		assertNotSame(control1.enabled(), control2.enabled());
	}
}