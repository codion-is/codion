/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import junit.framework.TestCase;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StateTest extends TestCase {

  private int stateChanged = 0;

  public void test() {
    final State state = new State();
    state.evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        stateChanged++;
      }
    });
    assertFalse("State should be inactive when initialized", state.isActive());
    state.setActive(true);
    assertTrue("State should be active after activation", state.isActive());
    assertFalse("Reversed state should be inactive after activation", state.getReversedState().isActive());
    assertTrue("evtStateChanged should have been fired when an inactive state was activated", stateChanged == 1);
    state.setActive(true);
    assertTrue("evtStateChanged should not have been fired when an active state was activated", stateChanged == 1);
    state.setActive(false);
    assertFalse("State should be inactive after deactivation", state.isActive());
    assertTrue("Reversed state should be active after deactivation", state.getReversedState().isActive());
    assertTrue("evtStateChanged should have been fired when an inactive state was deactivated", stateChanged == 2);
    state.setActive(false);
    assertTrue("evtStateChanged should not have been fired when an inactive state was deactivated", stateChanged == 2);
    try {
      state.getLinkedState().setActive(false);
      fail("should not be able to set the state of a linked state");
    }
    catch (Exception e) {}
    try {
      state.getReversedState().setActive(false);
      fail("should not be able to set the state of a reversed state");
    }
    catch (Exception e) {}
  }
}
