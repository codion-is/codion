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

import static org.junit.jupiter.api.Assertions.*;

public class DefaultControlsTest {

	@Test
	void test() {
		Control one = Control.control(() -> {});
		Control two = Control.control(() -> {});
		Controls list = Controls.builder().name("list").controls(one, two).build();
		assertThrows(NullPointerException.class, () -> list.add(null));
		assertThrows(NullPointerException.class, () -> list.addAt(0, null));
		list.remove(null);
		assertTrue(list.name().isPresent());
		assertFalse(list.smallIcon().isPresent());
		assertEquals("list", list.name().orElse(null));
		Controls list1 = Controls.controls();
		assertFalse(list1.name().isPresent());
		Controls list2 = Controls.builder()
						.name("list")
						.control(two)
						.build();
		assertTrue(list2.name().isPresent());
		assertEquals("list", list2.name().orElse(null));
		list2.addAt(0, one);
		list2.addSeparatorAt(1);

		assertEquals(one, list2.get(0));
		assertSame(Controls.SEPARATOR, list2.get(1));
		assertEquals(two, list2.get(2));

		assertTrue(list2.actions().contains(one));
		assertTrue(list2.actions().contains(two));
		assertEquals(3, list2.size());
		list2.addSeparator();
		assertEquals(4, list2.size());

		list2.remove(two);
		assertFalse(list2.actions().contains(two));

		list2.removeAll();
		assertFalse(list2.actions().contains(one));
		assertTrue(list2.empty());
		assertFalse(list2.notEmpty());

		assertThrows(UnsupportedOperationException.class, () -> list.setEnabled(false));
		assertThrows(UnsupportedOperationException.class, () -> list.putValue("enabled", false));
		assertThrows(IllegalArgumentException.class, () -> Control.builder(() -> {}).value("enabled", false));
	}
}
