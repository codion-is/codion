/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.event.Event;
import is.codion.common.value.Value;

import org.junit.jupiter.api.Test;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultControlsTest {

  private boolean booleanValue;

  public boolean isBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(boolean booleanValue) {
    this.booleanValue = booleanValue;
  }

  private final Controls controls = Controls.builder().controls(
          Control.builder(() -> {}).caption("one"),
          Control.builder(() -> {}).caption("two"),
          ToggleControl.builder(Value.propertyValue(this, "booleanValue", boolean.class, Event.event()))
                  .caption("three")).build();

  @Test
  void test() {
    Control one = Control.control(() -> {});
    Control two = Control.control(() -> {});
    Controls list = Controls.builder().caption("list").controls(one, two).build();
    assertThrows(NullPointerException.class, () -> list.add(null));
    assertThrows(NullPointerException.class, () -> list.addAt(0, null));
    list.remove(null);
    assertFalse(nullOrEmpty(list.getCaption()));
    assertNull(list.getSmallIcon());
    assertEquals("list", list.getCaption());
    Controls list1 = Controls.controls();
    assertTrue(nullOrEmpty(list1.getCaption()));
    assertEquals("", list1.getCaption());
    Controls list2 = Controls.builder().control(two).build();
    list2.setCaption("list");
    assertFalse(nullOrEmpty(list2.getCaption()));
    assertEquals("list", list2.getCaption());
    list2.addAt(0, one);
    list2.addSeparatorAt(1);

    assertEquals(one, list2.get(0));
    assertNull(list2.get(1));
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
    assertTrue(list2.isEmpty());
  }
}
