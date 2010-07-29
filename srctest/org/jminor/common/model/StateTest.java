/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StateTest {

  private int stateChanged = 0;

  @Test
  public void test() {
    final Action listeningAction = new AbstractAction("test") {
      public void actionPerformed(final ActionEvent e) {}
    };
    final State state = new State();
    state.addListeningAction(listeningAction);
    final ActionListener listener = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        stateChanged++;
      }
    };
    state.addStateListener(listener);

    assertFalse("State should be inactive when initialized", state.isActive());
    state.setActive(true);
    assertTrue("State should be active after activation", state.isActive());
    assertTrue("Listening action should be active after activation", listeningAction.isEnabled());
    assertEquals("active", state.toString());
    assertFalse("Reversed state should be inactive after activation", state.getReversedState().isActive());
    assertTrue("evtStateChanged should have been fired when an inactive state was activated", stateChanged == 1);
    state.setActive(true);
    assertTrue("evtStateChanged should not have been fired when an active state was activated", stateChanged == 1);
    state.setActive(false);
    assertFalse("State should be inactive after deactivation", state.isActive());
    assertFalse("Listening action should be inactive after deactivation", listeningAction.isEnabled());
    assertEquals("inactive", state.toString());
    assertTrue("Reversed state should be active after deactivation", state.getReversedState().isActive());
    assertTrue("evtStateChanged should have been fired when an inactive state was deactivated", stateChanged == 2);
    state.setActive(false);
    assertTrue("evtStateChanged should not have been fired when an inactive state was deactivated", stateChanged == 2);
    try {
      final State linkedState = state.getLinkedState();
      assertEquals("inactive linked", linkedState.toString());
      linkedState.setActive(false);
      fail("should not be able to set the state of a linked state");
    }
    catch (Exception e) {}
    try {
      final State reversedState = state.getReversedState();
      assertEquals(state, reversedState.getReversedState());
      assertEquals("active reversed", reversedState.toString());
      reversedState.setActive(false);
      fail("should not be able to set the state of a reversed state");
    }
    catch (Exception e) {}

    state.removeStateListener(listener);
  }

  @Test
  public void stateGroup() throws Exception {
    final State stateOne = new State(true);
    final State stateTwo = new State(true);
    final State stateThree = new State(true);
    final State.StateGroup stateGroup = new State.StateGroup();

    stateGroup.addState(stateOne);
    stateGroup.addState(stateTwo);
    assertFalse(stateOne.isActive());
    assertTrue(stateTwo.isActive());
    stateGroup.addState(stateThree);
    assertFalse(stateOne.isActive());
    assertFalse(stateTwo.isActive());
    assertTrue(stateThree.isActive());

    stateOne.setActive(true);
    assertFalse(stateTwo.isActive());
    assertFalse(stateThree.isActive());
  }
}
