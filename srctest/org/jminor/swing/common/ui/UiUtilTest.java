/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.model.State;
import org.jminor.common.model.States;
import org.jminor.swing.common.ui.textfield.SizedDocument;

import org.junit.Test;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import static org.junit.Assert.*;

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
    JTextField txt = new JTextField();
    UiUtil.makeUpperCase(txt);
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
    JTextField txt = new JTextField();
    UiUtil.makeLowerCase(txt);
    txt.setText("HELLO");
    assertEquals("hello", txt.getText());

    txt = new JTextField();
    txt.setDocument(new SizedDocument());
    UiUtil.makeLowerCase(txt);
    txt.setText("HELLO");
    assertEquals("hello", txt.getText());
  }

  @Test(expected = IllegalArgumentException.class)
  public void addKeyEventWithoutName() {
    final JTextField txt = new JTextField();
    UiUtil.addKeyEvent(txt, KeyEvent.VK_ENTER, new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {}
    });
  }
}
