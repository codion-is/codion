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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import org.junit.jupiter.api.Test;

import javax.swing.Action;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultControlsTest {

	@Test
	void test() {
		Control one = Control.control(() -> {});
		Control two = Control.control(() -> {});
		Action action = Control.control(() -> {});
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
						.control(two)
						.build();
		assertTrue(controls1.name().isPresent());
		assertEquals("controls", controls1.name().orElse(null));

		assertEquals(one, controls1.get(0));
		assertSame(Controls.SEPARATOR, controls1.get(1));
		assertEquals(two, controls1.get(2));

		assertTrue(controls1.actions().contains(one));
		assertTrue(controls1.actions().contains(two));
		assertEquals(3, controls1.size());

		assertTrue(controls1.notEmpty());

		assertThrows(UnsupportedOperationException.class, () -> controls.setEnabled(false));
		assertThrows(UnsupportedOperationException.class, () -> controls.putValue("enabled", false));
		assertThrows(IllegalArgumentException.class, () -> Control.builder(() -> {}).value("enabled", false));
	}

	@Test
	void copy() {
		Control one = Control.control(() -> {});
		Control two = Control.control(() -> {});
		Action action = Control.control(() -> {});
		DefaultControls controls = (DefaultControls) Controls.builder()
						.name("controls")
						.control(one)
						.separator()
						.control(two)
						.action(action)
						.build();
		DefaultControls copy = (DefaultControls) controls.copy().build();
		controls.keys().forEach(key -> assertEquals(controls.getValue(key), copy.getValue(key)));
		assertSame(controls.enabled(), copy.enabled());
	}
}
