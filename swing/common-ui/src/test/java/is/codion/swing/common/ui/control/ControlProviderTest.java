/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.event.Events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import java.util.ArrayList;
import java.util.List;

import static is.codion.swing.common.ui.control.Controls.toggleControl;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ControlProviderTest {

  private ControlList controlList;
  private boolean booleanValue;
  private Object selectedValue;

  public boolean isBooleanValue() {
    return booleanValue;
  }

  public void setBooleanValue(final boolean booleanValue) {
    this.booleanValue = booleanValue;
  }

  public Object getSelectedValue() {
    return selectedValue;
  }

  public void setSelectedValue(final Object selectedValue) {
    this.selectedValue = selectedValue;
  }

  @BeforeEach
  public void setUp() {
    controlList = Controls.controlList("hello");
    controlList.add(Controls.control(() -> {}, "one"));
    controlList.add(Controls.control(() -> {}, "two"));
    controlList.add(toggleControl(this, "booleanValue", "three", Events.event()));
  }

  @Test
  public void createCheckBox() {
    final JCheckBox box = Controls.createCheckBox(toggleControl(this, "booleanValue",
            "Test", Events.event()));
    assertEquals("Test", box.getText());
  }

  @Test
  public void createCheckBoxMenuItem() {
    final JMenuItem item = Controls.createCheckBoxMenuItem(toggleControl(this, "booleanValue",
            "Test", Events.event()));
    assertEquals("Test", item.getText());
  }

  @Test
  public void createMenuBar() {
    final ControlList base = Controls.controlList();
    base.add(controlList);

    final JMenuBar menu = Controls.createMenuBar(base);
    assertEquals(1, menu.getMenuCount());
    assertEquals(3, menu.getMenu(0).getItemCount());
    assertEquals("one", menu.getMenu(0).getItem(0).getText());
    assertEquals("two", menu.getMenu(0).getItem(1).getText());
    assertEquals("three", menu.getMenu(0).getItem(2).getText());

    final List<ControlList> lists = new ArrayList<>();
    lists.add(controlList);
    lists.add(base);
    Controls.createMenuBar(lists);
  }

  @Test
  public void createPopupMenu() {
    final ControlList base = Controls.controlList();
    base.add(controlList);

    Controls.createPopupMenu(base);
  }

  @Test
  public void createHorizontalButtonPanel() {
    Controls.createHorizontalButtonPanel(controlList);
    final JPanel base = new JPanel();
    base.add(Controls.createHorizontalButtonPanel(controlList));
  }

  @Test
  public void createVerticalButtonPanel() {
    Controls.createVerticalButtonPanel(controlList);
    final JPanel base = new JPanel();
    base.add(Controls.createVerticalButtonPanel(controlList));
  }

  @Test
  public void createToolBar() {
    Controls.createToolBar(controlList, JToolBar.VERTICAL);
  }
}
