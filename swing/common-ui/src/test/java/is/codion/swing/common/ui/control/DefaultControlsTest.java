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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import org.junit.jupiter.api.Test;

import javax.swing.Action;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultControlsTest {

	@Test
	void test() {
		Control one = Control.command(() -> {});
		Control two = Control.command(() -> {});
		Action action = Control.command(() -> {});
		Controls controls = Controls.builder()
						.name("controls")
						.control(one)
						.separator()
						.control(two)
						.action(action)
						.build();

		assertTrue(controls.name().isPresent());
		assertFalse(controls.smallIcon().isPresent());
		assertEquals("controls", controls.name().orElse(null));

		Controls controls1 = Controls.builder()
						.name("controls")
						.control(one)
						.separator()
						.separator()
						.action(Controls.SEPARATOR)
						.actions(Controls.SEPARATOR, Controls.SEPARATOR)
						.control(two)
						.build();
		assertTrue(controls1.name().isPresent());
		assertEquals("controls", controls1.name().orElse(null));

		assertEquals(3, controls1.actions().size());// only one separator
		assertEquals(one, controls1.actions().get(0));
		assertSame(Controls.SEPARATOR, controls1.actions().get(1));
		assertEquals(two, controls1.actions().get(2));

		assertTrue(controls1.actions().contains(one));
		assertTrue(controls1.actions().contains(two));
		assertEquals(3, controls1.actions().size());

		assertThrows(UnsupportedOperationException.class, () -> controls.setEnabled(false));
		assertThrows(UnsupportedOperationException.class, () -> controls.putValue("enabled", false));
		assertThrows(IllegalArgumentException.class, () -> Control.builder()
						.command(() -> {})
						.value("enabled", false));
	}

	@Test
	void copy() {
		Control one = Control.command(() -> {});
		Control two = Control.command(() -> {});
		Action action = Control.command(() -> {});
		Controls controls = Controls.builder()
						.name("controls")
						.control(one)
						.separator()
						.control(two)
						.action(action)
						.build();
		Controls copy = controls.copy().build();
		controls.keys().forEach(key -> assertEquals(controls.getValue(key), copy.getValue(key)));
		assertSame(controls.enabled(), copy.enabled());
		Action action2 = Control.command(() -> {});
		Controls copy2 = copy.copy()
						.separatorAt(3)
						.actionAt(4, action2)
						.build();
		assertSame(Controls.SEPARATOR, copy2.actions().get(3));
		assertSame(action2, copy2.actions().get(4));
	}
}
