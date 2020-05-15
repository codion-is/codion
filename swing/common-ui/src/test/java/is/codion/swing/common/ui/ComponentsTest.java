/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui;

import dev.codion.common.event.EventObserver;
import dev.codion.common.state.State;
import dev.codion.common.state.States;

import org.junit.jupiter.api.Test;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ComponentsTest {

  @Test
  public void linkToEnabledState() {
    final Action action = new AbstractAction("test") {
      @Override
      public void actionPerformed(final ActionEvent e) {}
    };
    State state = States.state();

    Components.linkToEnabledState(state, action);
    assertFalse(action.isEnabled());
    state.set(true);
    assertTrue(action.isEnabled());
    state.set(false);
    assertFalse(action.isEnabled());

    final JComponent comp = new JTextField();
    state = States.state();

    final State theState = state;
    SwingUtilities.invokeLater(() -> {
      Components.linkToEnabledState(theState, comp);
      assertFalse(comp.isEnabled());
      theState.set(true);
      assertTrue(comp.isEnabled());
      theState.set(false);
      assertFalse(comp.isEnabled());
    });
  }

  @Test
  public void setPreferredWidth() {
    final JTextField textField = new JTextField();
    Components.setPreferredWidth(textField, 42);
    assertEquals(new Dimension(42, textField.getPreferredSize().height), textField.getPreferredSize());
    final JComboBox box = new JComboBox();
    box.setPreferredSize(new Dimension(10, 10));
    Components.setPreferredWidth(box, 42);
    assertEquals(10, box.getPreferredSize().height);
    assertEquals(42, box.getPreferredSize().width);
  }

  @Test
  public void setPreferredHeight() {
    final JTextField textField = new JTextField();
    Components.setPreferredHeight(textField, 42);
    assertEquals(new Dimension(textField.getPreferredSize().width, 42), textField.getPreferredSize());
  }

  @Test
  public void propertyChangeObserver() {
    final JTextField textField = new JTextField();
    final AtomicInteger counter = new AtomicInteger();
    final EventObserver<Integer> alignmentObserver =
            Components.propertyChangeObserver(textField, "horizontalAlignment");
    alignmentObserver.addListener(counter::incrementAndGet);
    textField.setHorizontalAlignment(SwingConstants.RIGHT);
    assertEquals(1, counter.get());
    textField.setHorizontalAlignment(SwingConstants.LEFT);
    assertEquals(2, counter.get());
  }
}
