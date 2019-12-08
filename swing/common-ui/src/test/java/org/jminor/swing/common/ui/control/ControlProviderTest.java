/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.event.Events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ControlProviderTest {

  private ControlSet set;
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
    set = new ControlSet("hello");
    set.add(new Control("one"));
    set.add(new Control("two"));
    set.add(Controls.toggleControl(this, "booleanValue", "three", Events.event()));
  }

  @Test
  public void createCheckBox() {
    final JCheckBox box = ControlProvider.createCheckBox(Controls.toggleControl(this, "booleanValue", "Test", Events.event()));
    assertEquals("Test", box.getText());
  }

  @Test
  public void createCheckBoxMenuItem() {
    final JMenuItem item = ControlProvider.createCheckBoxMenuItem(Controls.toggleControl(this, "booleanValue", "Test", Events.event()));
    assertEquals("Test", item.getText());
  }

  @Test
  public void createMenuBar() {
    final ControlSet base = new ControlSet();
    base.add(set);

    final JMenuBar menu = ControlProvider.createMenuBar(base);
    assertEquals(1, menu.getMenuCount());
    assertEquals(3, menu.getMenu(0).getItemCount());
    assertEquals("one", menu.getMenu(0).getItem(0).getText());
    assertEquals("two", menu.getMenu(0).getItem(1).getText());
    assertEquals("three", menu.getMenu(0).getItem(2).getText());

    final List<ControlSet> sets = new ArrayList<>();
    sets.add(set);
    sets.add(base);
    ControlProvider.createMenuBar(sets);
  }

  @Test
  public void createPopupMenu() {
    final ControlSet base = new ControlSet();
    base.add(set);

    ControlProvider.createPopupMenu(base);
  }

  @Test
  public void createHorizontalButtonPanel() {
    ControlProvider.createHorizontalButtonPanel(set);
    final JPanel base = new JPanel();
    ControlProvider.createHorizontalButtonPanel(base, set);
  }

  @Test
  public void createVerticalButtonPanel() {
    ControlProvider.createVerticalButtonPanel(set);
    final JPanel base = new JPanel();
    ControlProvider.createVerticalButtonPanel(base, set);
  }

  @Test
  public void createToolBar() {
    final JToolBar bar = ControlProvider.createToolBar(set, JToolBar.VERTICAL);
    final JToolBar barTwo = new JToolBar();
    ControlProvider.createToolBar(barTwo, set);
  }
}
