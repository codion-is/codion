/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.Test;

import java.awt.event.ActionEvent;

import static org.junit.Assert.*;

public class StateImplTest {

  private int stateChanged = 0;

  @Test
  public void test() {
    final State state = new States.StateImpl();
    final EventListener listener = new EventListener() {
      @Override
      public void eventOccurred(final ActionEvent e) {
        stateChanged++;
      }
    };
    state.addListener(listener);

    assertFalse("State should be inactive when initialized", state.isActive());
    state.setActive(true);
    assertTrue("State should be active after activation", state.isActive());
    //assertTrue("Listening action should be active after activation", listeningAction.isEnabled());
    assertEquals(States.StateImpl.ACTIVE, state.toString());
    assertFalse("Reversed state should be inactive after activation", state.getReversedObserver().isActive());
    assertTrue("evtStateChanged should have been fired when an inactive state was activated", stateChanged == 1);
    state.setActive(true);
    assertTrue("evtStateChanged should not have been fired when an active state was activated", stateChanged == 1);
    state.setActive(false);
    assertFalse("State should be inactive after deactivation", state.isActive());
    //assertFalse("Listening action should be inactive after deactivation", listeningAction.isEnabled());
    assertEquals(States.StateImpl.INACTIVE, state.toString());
    assertTrue("Reversed state should be active after deactivation", state.getReversedObserver().isActive());
    assertTrue("evtStateChanged should have been fired when an inactive state was deactivated", stateChanged == 2);
    state.setActive(false);
    assertTrue("evtStateChanged should not have been fired when an inactive state was deactivated", stateChanged == 2);

    state.removeListener(listener);
  }

  @Test
  public void stateGroup() throws Exception {
    final State stateOne = new States.StateImpl(true);
    final State stateTwo = new States.StateImpl(true);
    final State stateThree = new States.StateImpl(true);
    final State.StateGroup stateGroup = new States.StateGroupImpl();

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
