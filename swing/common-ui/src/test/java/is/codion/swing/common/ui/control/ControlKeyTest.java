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

import org.junit.jupiter.api.Test;

import javax.swing.KeyStroke;

import static java.util.Collections.emptyList;
import static javax.swing.KeyStroke.getKeyStroke;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests ControlKey behavior and factory methods.
 */
public final class ControlKeyTest {

	@Test
	void commandControlKey() {
		// Without keystroke
		ControlKey<CommandControl> saveKey = CommandControl.key("save");
		assertEquals("save", saveKey.name());
		assertEquals(CommandControl.class, saveKey.controlClass());
		assertNotNull(saveKey.defaultKeystroke());
		assertNull(saveKey.defaultKeystroke().get());

		// With keystroke
		KeyStroke ctrlS = getKeyStroke("ctrl S");
		ControlKey<CommandControl> saveWithKey = CommandControl.key("save", ctrlS);
		assertEquals("save", saveWithKey.name());
		assertEquals(CommandControl.class, saveWithKey.controlClass());
		assertEquals(ctrlS, saveWithKey.defaultKeystroke().get());

		// Null name should throw
		assertThrows(NullPointerException.class, () -> CommandControl.key(null));
		assertThrows(NullPointerException.class, () -> CommandControl.key(null, ctrlS));
	}

	@Test
	void toggleControlKey() {
		// Without keystroke
		ControlKey<ToggleControl> toggleKey = ToggleControl.key("toggleView");
		assertEquals("toggleView", toggleKey.name());
		assertEquals(ToggleControl.class, toggleKey.controlClass());
		assertNotNull(toggleKey.defaultKeystroke());
		assertNull(toggleKey.defaultKeystroke().get());

		// With keystroke
		KeyStroke f2 = getKeyStroke("F2");
		ControlKey<ToggleControl> toggleWithKey = ToggleControl.key("toggleView", f2);
		assertEquals("toggleView", toggleWithKey.name());
		assertEquals(ToggleControl.class, toggleWithKey.controlClass());
		assertEquals(f2, toggleWithKey.defaultKeystroke().get());

		// Null name should throw
		assertThrows(NullPointerException.class, () -> ToggleControl.key(null));
		assertThrows(NullPointerException.class, () -> ToggleControl.key(null, f2));
	}

	@Test
	void controlsKey() {
		// Without layout
		Controls.ControlsKey controlsKey = Controls.key("menu");
		assertEquals("menu", controlsKey.name());
		assertEquals(Controls.class, controlsKey.controlClass());
		assertNotNull(controlsKey.defaultKeystroke());
		assertNull(controlsKey.defaultKeystroke().get());
		assertFalse(controlsKey.defaultLayout().isPresent());

		// With layout
		Controls.Layout layout = Controls.layout(emptyList());
		Controls.ControlsKey controlsWithLayout = Controls.key("menu", layout);
		assertEquals("menu", controlsWithLayout.name());
		assertEquals(Controls.class, controlsWithLayout.controlClass());
		assertTrue(controlsWithLayout.defaultLayout().isPresent());
		assertEquals(layout, controlsWithLayout.defaultLayout().get());

		// Null name should throw
		assertThrows(NullPointerException.class, () -> Controls.key(null));
		assertThrows(NullPointerException.class, () -> Controls.key(null, layout));
	}

	@Test
	void defaultKeystrokeIsMutable() {
		KeyStroke ctrlS = getKeyStroke("ctrl S");
		ControlKey<CommandControl> saveKey = CommandControl.key("save", ctrlS);

		Value<KeyStroke> defaultKeystroke = saveKey.defaultKeystroke();
		assertEquals(ctrlS, defaultKeystroke.get());

		// Default keystroke value is mutable
		KeyStroke ctrlShiftS = getKeyStroke("ctrl shift S");
		defaultKeystroke.set(ctrlShiftS);
		assertEquals(ctrlShiftS, defaultKeystroke.get());

		// Setting to null is allowed
		defaultKeystroke.set(null);
		assertNull(defaultKeystroke.get());
	}

	@Test
	void keyToStringReturnsName() {
		ControlKey<CommandControl> key = CommandControl.key("testKey");
		assertEquals("testKey", key.toString());

		ControlKey<ToggleControl> toggleKey = ToggleControl.key("toggle", getKeyStroke("F1"));
		assertEquals("toggle", toggleKey.toString());
	}

	@Test
	void sameNameDifferentInstances() {
		// Same name can be used for different key instances
		ControlKey<CommandControl> save1 = CommandControl.key("save");
		ControlKey<CommandControl> save2 = CommandControl.key("save");

		// Different instances
		assertNotSame(save1, save2);

		// But same properties
		assertEquals(save1.name(), save2.name());
		assertEquals(save1.controlClass(), save2.controlClass());

		// And independent default keystrokes
		save1.defaultKeystroke().set(getKeyStroke("ctrl S"));
		assertNull(save2.defaultKeystroke().get());
	}

	@Test
	void differentControlTypesWithSameName() {
		// Same name can be used for different control types
		String name = "action";
		ControlKey<CommandControl> commandKey = CommandControl.key(name);
		ControlKey<ToggleControl> toggleKey = ToggleControl.key(name);
		Controls.ControlsKey controlsKey = Controls.key(name);

		// Same name
		assertEquals(name, commandKey.name());
		assertEquals(name, toggleKey.name());
		assertEquals(name, controlsKey.name());

		// Different control classes
		assertEquals(CommandControl.class, commandKey.controlClass());
		assertEquals(ToggleControl.class, toggleKey.controlClass());
		assertEquals(Controls.class, controlsKey.controlClass());
	}
}