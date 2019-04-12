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
    JTextField textField = UiUtil.makeUpperCase(new JTextField());
    textField.setText("hello");
    assertEquals("HELLO", textField.getText());

    textField = new JTextField();
    textField.setDocument(new SizedDocument());
    UiUtil.makeUpperCase(textField);
    textField.setText("hello");
    assertEquals("HELLO", textField.getText());
  }

  @Test
  public void makeLowerCase() {
    JTextField textField = UiUtil.makeLowerCase(new JTextField());
    textField.setText("HELLO");
    assertEquals("hello", textField.getText());

    textField = new JTextField();
    textField.setDocument(new SizedDocument());
    UiUtil.makeLowerCase(textField);
    textField.setText("HELLO");
    assertEquals("hello", textField.getText());
  }

  @Test
  public void addKeyEventWithoutName() {
    final JTextField textField = new JTextField();
    final String actionName = textField.getClass().getSimpleName() + KeyEvent.VK_ENTER + 0 + "true";
    assertNull(textField.getActionMap().get(actionName));
    UiUtil.addKeyEvent(textField, KeyEvent.VK_ENTER, Controls.control(() -> {}));
    assertNotNull(textField.getActionMap().get(actionName));
  }

  @Test
  public void setPreferredWidth() {
    final JTextField textField = new JTextField();
    UiUtil.setPreferredWidth(textField, 42);
    assertEquals(new Dimension(42, textField.getPreferredSize().height), textField.getPreferredSize());
  }

  @Test
  public void setPreferredHeight() {
    final JTextField textField = new JTextField();
    UiUtil.setPreferredHeight(textField, 42);
    assertEquals(new Dimension(textField.getPreferredSize().width, 42), textField.getPreferredSize());
  }

  @Test
  public void selectAllOnFocusGained() {
    final JTextField textField = new JTextField("test");
    final int focusListenerCount = textField.getFocusListeners().length;
    UiUtil.selectAllOnFocusGained(textField);
    assertEquals(focusListenerCount + 1, textField.getFocusListeners().length);
    UiUtil.selectNoneOnFocusGained(textField);
    assertEquals(focusListenerCount, textField.getFocusListeners().length);
  }
}
