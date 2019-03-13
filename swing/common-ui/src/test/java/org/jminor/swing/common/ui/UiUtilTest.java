/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.State;
import org.jminor.common.States;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.textfield.SizedDocument;

import org.junit.jupiter.api.Test;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.*;

public class UiUtilTest {

  @Test
  public void linkToEnabledState() {
    final Action action = new AbstractAction("test") {
      @Override
      public void actionPerformed(final ActionEvent e) {}
    };
    State state = States.state();

    UiUtil.linkToEnabledState(state, action);
    assertFalse(action.isEnabled());
    state.setActive(true);
    assertTrue(action.isEnabled());
    state.setActive(false);
    assertFalse(action.isEnabled());

    final JComponent comp = new JTextField();
    state = States.state();

    UiUtil.linkToEnabledState(state, comp);
    assertFalse(comp.isEnabled());
    state.setActive(true);
    assertTrue(comp.isEnabled());
    state.setActive(false);
    assertFalse(comp.isEnabled());
  }

  @Test
  public void makeUpperCase() {
    JTextField txt = UiUtil.makeUpperCase(new JTextField());
    txt.setText("hello");
    assertEquals("HELLO", txt.getText());

    txt = new JTextField();
    txt.setDocument(new SizedDocument());
    UiUtil.makeUpperCase(txt);
    txt.setText("hello");
    assertEquals("HELLO", txt.getText());
  }

  @Test
  public void makeLowerCase() {
    JTextField txt = UiUtil.makeLowerCase(new JTextField());
    txt.setText("HELLO");
    assertEquals("hello", txt.getText());

    txt = new JTextField();
    txt.setDocument(new SizedDocument());
    UiUtil.makeLowerCase(txt);
    txt.setText("HELLO");
    assertEquals("hello", txt.getText());
  }

  @Test
  public void addKeyEventWithoutName() {
    final JTextField txt = new JTextField();
    final String actionName = txt.getClass().getSimpleName() + KeyEvent.VK_ENTER + 0 + "true";
    assertNull(txt.getActionMap().get(actionName));
    UiUtil.addKeyEvent(txt, KeyEvent.VK_ENTER, Controls.control(() -> {}));
    assertNotNull(txt.getActionMap().get(actionName));
  }

  @Test
  public void setPreferredWidth() {
    final JTextField txt = new JTextField();
    UiUtil.setPreferredWidth(txt, 42);
    assertEquals(new Dimension(42, txt.getPreferredSize().height), txt.getPreferredSize());
  }

  @Test
  public void setPreferredHeight() {
    final JTextField txt = new JTextField();
    UiUtil.setPreferredHeight(txt, 42);
    assertEquals(new Dimension(txt.getPreferredSize().width, 42), txt.getPreferredSize());
  }

  @Test
  public void selectAllOnFocusGained() {
    final JTextField txt = new JTextField("test");
    final int focusListenerCount = txt.getFocusListeners().length;
    UiUtil.selectAllOnFocusGained(txt);
    assertEquals(focusListenerCount + 1, txt.getFocusListeners().length);
    UiUtil.selectNoneOnFocusGained(txt);
    assertEquals(focusListenerCount, txt.getFocusListeners().length);
  }
}
