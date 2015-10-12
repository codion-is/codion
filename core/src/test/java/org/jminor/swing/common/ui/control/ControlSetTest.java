/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.junit.Test;

import static org.junit.Assert.*;

public class ControlSetTest {

  @Test
  public void test() {
    final Control one = new Control("one");
    final Control two = new Control("two");
    ControlSet set = new ControlSet("set", one, two);
    assertTrue(set.hasName());
    assertFalse(set.hasIcon());
    assertEquals("set", set.getName());
    set = new ControlSet();
    assertFalse(set.hasName());
    assertEquals("", set.getName());
    set = new ControlSet(two);
    set.setName("set");
    assertTrue(set.hasName());
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
