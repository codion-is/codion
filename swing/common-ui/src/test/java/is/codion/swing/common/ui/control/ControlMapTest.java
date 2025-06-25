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

import is.codion.common.value.Value;
import is.codion.swing.common.ui.key.KeyEvents;

import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;
import java.util.Optional;

import static javax.swing.KeyStroke.getKeyStroke;
import static org.junit.jupiter.api.Assertions.*;

public final class ControlMapTest {

	// Test control key definitions
	public static final ControlKey<CommandControl> SAVE = CommandControl.key("save", getKeyStroke("ctrl S"));
	public static final ControlKey<CommandControl> DELETE = CommandControl.key("delete", getKeyStroke("DELETE"));
	public static final ControlKey<ToggleControl> TOGGLE_VIEW = ToggleControl.key("toggleView");

	// This should be ignored (not public)
	static final ControlKey<CommandControl> PRIVATE_KEY = CommandControl.key("private");

	// This should be ignored (not static)
	public final ControlKey<CommandControl> INSTANCE_KEY = CommandControl.key("instance");

	// This should be ignored (not final)
	public static ControlKey<CommandControl> MUTABLE_KEY = CommandControl.key("mutable");

	@Test
	void reflectiveControlKeyDiscovery() {
		ControlMap controlMap = ControlMap.controlMap(ControlMapTest.class);

		// Should discover public static final ControlKey fields
		assertNotNull(controlMap.control(SAVE));
		assertNotNull(controlMap.control(DELETE));
		assertNotNull(controlMap.control(TOGGLE_VIEW));

		// Should not discover non-public, non-static, or non-final fields
		assertThrows(IllegalArgumentException.class, () -> controlMap.control(PRIVATE_KEY));
		assertThrows(IllegalArgumentException.class, () -> controlMap.control(INSTANCE_KEY));
		assertThrows(IllegalArgumentException.class, () -> controlMap.control(MUTABLE_KEY));
	}

	@Test
	void controlManagement() {
		ControlMap controlMap = ControlMap.controlMap(ControlMapTest.class);

		// Initially no controls are set
		Value<CommandControl> saveControl = controlMap.control(SAVE);
		assertNull(saveControl.get());

		// Set a control
		CommandControl save = Control.command(() -> System.out.println("Save"));
		saveControl.set(save);
		assertEquals(save, saveControl.get());

		// Control values are mutable
		CommandControl newSave = Control.command(() -> System.out.println("New Save"));
		saveControl.set(newSave);
		assertEquals(newSave, saveControl.get());
	}

	@Test
	void keyStrokeManagement() {
		ControlMap controlMap = ControlMap.controlMap(ControlMapTest.class);

		// Default keystrokes are preserved
		Value<KeyStroke> saveKeyStroke = controlMap.keyStroke(SAVE);
		assertEquals(getKeyStroke("ctrl S"), saveKeyStroke.get());

		Value<KeyStroke> deleteKeyStroke = controlMap.keyStroke(DELETE);
		assertEquals(getKeyStroke("DELETE"), deleteKeyStroke.get());

		// No default keystroke
		Value<KeyStroke> toggleKeyStroke = controlMap.keyStroke(TOGGLE_VIEW);
		assertNull(toggleKeyStroke.get());

		// Keystrokes can be modified
		KeyStroke newKeyStroke = getKeyStroke("ctrl shift S");
		saveKeyStroke.set(newKeyStroke);
		assertEquals(newKeyStroke, saveKeyStroke.get());

		// Original default is unchanged
		assertEquals(getKeyStroke("ctrl S"), SAVE.defaultKeystroke().get());
	}

	@Test
	void keyEventBuilder() {
		ControlMap controlMap = ControlMap.controlMap(ControlMapTest.class);

		// No control or keystroke - no key event
		Optional<KeyEvents.Builder> noControl = controlMap.keyEvent(SAVE);
		assertFalse(noControl.isPresent());

		// Set control but no keystroke for TOGGLE_VIEW
		ToggleControl toggle = Control.toggle(Value.nullable(false));
		controlMap.control(TOGGLE_VIEW).set(toggle);
		Optional<KeyEvents.Builder> noKeyStroke = controlMap.keyEvent(TOGGLE_VIEW);
		assertFalse(noKeyStroke.isPresent());

		// Set both control and keystroke
		CommandControl save = Control.command(() -> {});
		controlMap.control(SAVE).set(save);
		Optional<KeyEvents.Builder> keyEvent = controlMap.keyEvent(SAVE);
		assertTrue(keyEvent.isPresent());

		// Remove keystroke - no key event
		controlMap.keyStroke(SAVE).set(null);
		Optional<KeyEvents.Builder> noKeyStrokeAgain = controlMap.keyEvent(SAVE);
		assertFalse(noKeyStrokeAgain.isPresent());
	}

	@Test
	void copyPreservesStateNotReferences() {
		ControlMap original = ControlMap.controlMap(ControlMapTest.class);

		// Set up original
		CommandControl saveControl = Control.command(() -> {});
		original.control(SAVE).set(saveControl);
		original.keyStroke(SAVE).set(getKeyStroke("ctrl alt S"));

		// Create copy
		ControlMap copy = original.copy();

		// Copy has same values
		assertEquals(saveControl, copy.control(SAVE).get());
		assertEquals(getKeyStroke("ctrl alt S"), copy.keyStroke(SAVE).get());

		// But modifications don't affect original
		CommandControl newControl = Control.command(() -> {});
		copy.control(SAVE).set(newControl);
		copy.keyStroke(SAVE).set(getKeyStroke("F2"));

		assertEquals(saveControl, original.control(SAVE).get());
		assertEquals(getKeyStroke("ctrl alt S"), original.keyStroke(SAVE).get());
		assertEquals(newControl, copy.control(SAVE).get());
		assertEquals(getKeyStroke("F2"), copy.keyStroke(SAVE).get());
	}

	@Test
	void unknownControlKey() {
		ControlMap controlMap = ControlMap.controlMap(ControlMapTest.class);
		ControlKey<CommandControl> unknown = CommandControl.key("unknown");

		assertThrows(IllegalArgumentException.class, () -> controlMap.control(unknown));
		assertThrows(IllegalArgumentException.class, () -> controlMap.keyStroke(unknown));
	}

	@Test
	void nullControlKey() {
		ControlMap controlMap = ControlMap.controlMap(ControlMapTest.class);

		assertThrows(NullPointerException.class, () -> controlMap.control(null));
		assertThrows(NullPointerException.class, () -> controlMap.keyStroke(null));
	}

	@Test
	void allControlsCollection() {
		ControlMap controlMap = ControlMap.controlMap(ControlMapTest.class);

		// Should have 3 controls (SAVE, DELETE, TOGGLE_VIEW)
		assertEquals(3, controlMap.controls().size());

		// All initially null
		assertTrue(controlMap.controls().stream().allMatch(value -> value.get() == null));

		// Set some controls
		controlMap.control(SAVE).set(Control.command(() -> {}));
		controlMap.control(TOGGLE_VIEW).set(Control.toggle(Value.nullable(true)));

		// Now 2 non-null
		assertEquals(2, controlMap.controls().stream().filter(value -> value.get() != null).count());
	}
}