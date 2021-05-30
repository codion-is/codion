/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.event.Event;
import is.codion.common.value.Value;

import org.junit.jupiter.api.Test;

import javax.swing.JMenuBar;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;

import static is.codion.common.Util.nullOrEmpty;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultControlsTest {

  private boolean booleanValue;

  public boolean isBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(final boolean booleanValue) {
    this.booleanValue = booleanValue;
  }

  private final Controls controls = Controls.builder().controls(
          Control.builder(() -> {}).caption("one"),
          Control.builder(() -> {}).caption("two"),
          ToggleControl.builder(Value.propertyValue(this, "booleanValue", boolean.class, Event.event()))
                  .caption("three")).build();

  @Test
  void test() {
    final Control one = Control.control(() -> {});
    final Control two = Control.control(() -> {});
    final Controls list = Controls.builder().caption("list").controls(one, two).build();
    assertThrows(NullPointerException.class, () -> list.add(null));
    assertThrows(NullPointerException.class, () -> list.addAt(0, null));
    list.remove(null);
    assertFalse(nullOrEmpty(list.getName()));
    assertNull(list.getIcon());
    assertEquals("list", list.getName());
    final Controls list1 = Controls.controls();
    assertTrue(nullOrEmpty(list1.getName()));
    assertNull(list1.getName());
    final Controls list2 = Controls.builder().control(two).build();
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

    list2.remove(two);
    assertFalse(list2.getActions().contains(two));

    list2.removeAll();
    assertFalse(list2.getActions().contains(one));
    assertTrue(list2.isEmpty());
  }

  @Test
  void menuBar() {
    final Controls base = Controls.controls();
    base.add(controls);

    final JMenuBar menu = base.createMenuBar();
    assertEquals(1, menu.getMenuCount());
    assertEquals(3, menu.getMenu(0).getItemCount());
    assertEquals("one", menu.getMenu(0).getItem(0).getText());
    assertEquals("two", menu.getMenu(0).getItem(1).getText());
    assertEquals("three", menu.getMenu(0).getItem(2).getText());

    final List<Controls> lists = new ArrayList<>();
    lists.add(controls);
    lists.add(base);
  }

  @Test
  void popupMenu() {
    final Controls base = Controls.controls();
    base.add(controls);

    base.createPopupMenu();
  }

  @Test
  void horizontalButtonPanel() {
    final JPanel base = new JPanel();
    base.add(controls.createHorizontalButtonPanel());
  }

  @Test
  void verticalButtonPanel() {
    final JPanel base = new JPanel();
    base.add(controls.createVerticalButtonPanel());
  }

  @Test
  void toolBar() {
    controls.createVerticalToolBar();
    controls.createHorizontalToolBar();
  }
}
