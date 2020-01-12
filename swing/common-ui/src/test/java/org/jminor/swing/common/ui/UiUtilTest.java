/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.state.State;
import org.jminor.common.state.States;
import org.jminor.swing.common.ui.textfield.SizedDocument;
import org.jminor.swing.common.ui.textfield.TextFields;

import org.junit.jupiter.api.Test;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

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
    state.set(true);
    assertTrue(action.isEnabled());
    state.set(false);
    assertFalse(action.isEnabled());

    final JComponent comp = new JTextField();
    state = States.state();

    UiUtil.linkToEnabledState(state, comp);
    assertFalse(comp.isEnabled());
    state.set(true);
    assertTrue(comp.isEnabled());
    state.set(false);
    assertFalse(comp.isEnabled());
  }

  @Test
  public void makeUpperCase() {
    JTextField textField = TextFields.makeUpperCase(new JTextField());
    textField.setText("hello");
    assertEquals("HELLO", textField.getText());

    textField = new JTextField();
    textField.setDocument(new SizedDocument());
    TextFields.makeUpperCase(textField);
    textField.setText("hello");
    assertEquals("HELLO", textField.getText());
  }

  @Test
  public void makeLowerCase() {
    JTextField textField = TextFields.makeLowerCase(new JTextField());
    textField.setText("HELLO");
    assertEquals("hello", textField.getText());

    textField = new JTextField();
    textField.setDocument(new SizedDocument());
    TextFields.makeLowerCase(textField);
    textField.setText("HELLO");
    assertEquals("hello", textField.getText());
  }

  @Test
  public void setPreferredWidth() {
    final JTextField textField = new JTextField();
    UiUtil.setPreferredWidth(textField, 42);
    assertEquals(new Dimension(42, textField.getPreferredSize().height), textField.getPreferredSize());
    final JComboBox box = new JComboBox();
    box.setPreferredSize(new Dimension(10, 10));
    UiUtil.setPreferredWidth(box, 42);
    assertEquals(10, box.getPreferredSize().height);
    assertEquals(42, box.getPreferredSize().width);
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
    TextFields.selectAllOnFocusGained(textField);
    assertEquals(focusListenerCount + 1, textField.getFocusListeners().length);
    TextFields.selectNoneOnFocusGained(textField);
    assertEquals(focusListenerCount, textField.getFocusListeners().length);
  }
}
