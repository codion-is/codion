/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import is.codion.common.event.EventObserver;
import is.codion.common.state.State;

import org.junit.jupiter.api.Test;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class UtilitiesTest {

  @Test
  void linkToEnabledState() {
    Action action = new AbstractAction("test") {
      @Override
      public void actionPerformed(final ActionEvent e) {}
    };
    State state = State.state();

    Utilities.linkToEnabledState(state, action);
    assertFalse(action.isEnabled());
    state.set(true);
    assertTrue(action.isEnabled());
    state.set(false);
    assertFalse(action.isEnabled());

    JComponent comp = new JTextField();
    state = State.state();

    State theState = state;
    try {
      Utilities.linkToEnabledState(theState, comp);
      assertFalse(comp.isEnabled());
      theState.set(true);
      Thread.sleep(50);//due to EDT
      assertTrue(comp.isEnabled());
      theState.set(false);
      Thread.sleep(50);//due to EDT
      assertFalse(comp.isEnabled());
    }
    catch (InterruptedException e) {/*ignored*/}
  }

  @Test
  void propertyChangeObserver() {
    JTextField textField = new JTextField();
    AtomicInteger counter = new AtomicInteger();
    EventObserver<Integer> alignmentObserver =
            Utilities.propertyChangeObserver(textField, "horizontalAlignment");
    alignmentObserver.addListener(counter::incrementAndGet);
    textField.setHorizontalAlignment(SwingConstants.RIGHT);
    assertEquals(1, counter.get());
    textField.setHorizontalAlignment(SwingConstants.LEFT);
    assertEquals(2, counter.get());
  }
}
