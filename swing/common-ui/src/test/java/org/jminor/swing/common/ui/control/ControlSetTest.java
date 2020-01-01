/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.junit.jupiter.api.Test;

import static org.jminor.common.Util.nullOrEmpty;
import static org.junit.jupiter.api.Assertions.*;

public class ControlSetTest {

  @Test
  public void test() {
    final Control one = new Control("one");
    final Control two = new Control("two");
    ControlSet set = new ControlSet("set", one, two);
    set.add(null);
    set.addAt(null, 0);
    set.remove(null);
    assertFalse(nullOrEmpty(set.getName()));
    assertNull(set.getIcon());
    assertEquals("set", set.getName());
    set = new ControlSet();
    assertTrue(nullOrEmpty(set.getName()));
    assertEquals("", set.getName());
    set = new ControlSet(two);
    set.setName("set");
    assertFalse(nullOrEmpty(set.getName()));
    assertEquals("set", set.getName());
    set.addAt(one, 0);
    set.addSeparatorAt(1);

    assertEquals(one, set.get(0));
    assertNull(set.get(1));
    assertEquals(two, set.get(2));

    assertTrue(set.getActions().contains(one));
    assertTrue(set.getActions().contains(two));
    assertEquals(3, set.size());
    set.addSeparator();
    assertEquals(4, set.size());

    assertEquals(0, set.getControlSets().size());

    set.remove(two);
    assertFalse(set.getActions().contains(two));

    set.removeAll();
    assertFalse(set.getActions().contains(one));
  }
}
