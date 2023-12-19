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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import org.junit.jupiter.api.Test;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
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
    assertFalse(nullOrEmpty(list.getName()));
    assertNull(list.getSmallIcon());
    assertEquals("list", list.getName());
    Controls list1 = Controls.controls();
    assertTrue(nullOrEmpty(list1.getName()));
    assertEquals("", list1.getName());
    Controls list2 = Controls.builder().control(two).build();
    list2.setName("list");
    assertFalse(nullOrEmpty(list2.getName()));
    assertEquals("list", list2.getName());
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
  }
}
