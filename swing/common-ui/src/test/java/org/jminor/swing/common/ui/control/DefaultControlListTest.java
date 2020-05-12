/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.junit.jupiter.api.Test;

import static org.jminor.common.Util.nullOrEmpty;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultControlListTest {

  @Test
  public void test() {
    final Control one = Controls.control(() -> {}, "one");
    final Control two = Controls.control(() -> {}, "two");
    final DefaultControlList list = new DefaultControlList("list", one, two);
    assertThrows(NullPointerException.class, () -> list.add(null));
    assertThrows(NullPointerException.class, () -> list.addAt(0, null));
    list.remove(null);
    assertFalse(nullOrEmpty(list.getName()));
    assertNull(list.getIcon());
    assertEquals("list", list.getName());
    final DefaultControlList list1 = new DefaultControlList();
    assertTrue(nullOrEmpty(list1.getName()));
    assertEquals("", list1.getName());
    final DefaultControlList list2 = new DefaultControlList(two);
    list2.setName("list");
    assertFalse(nullOrEmpty(list2.getName()));
    assertEquals("list", list2.getName());
    list2.addAt(0, one);
    list2.addSeparatorAt(1);

    assertEquals(one, list2.get(0));
    assertNull(list2.get(1));
    assertEquals(two, list2.get(2));

    assertTrue(list2.getActions().contains(one));
    assertTrue(list2.getActions().contains(two));
    assertEquals(3, list2.size());
    list2.addSeparator();
    assertEquals(4, list2.size());

    assertEquals(0, list2.getControlLists().size());

    list2.remove(two);
    assertFalse(list2.getActions().contains(two));

    list2.removeAll();
    assertFalse(list2.getActions().contains(one));
  }
}
