package org.jminor.common.model;

import junit.framework.TestCase;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TestState extends TestCase {

  private int stateChanged = 0;
  private int setActive = 0;
  private int setInactive = 0;

  public TestState() {
    super("TestState");
  }

  public void test() {
    final State state = new State();
    state.evtStateChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        stateChanged++;
      }
    });
    state.evtSetActive.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        setActive++;
      }
    });
    state.evtSetInactive.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        setInactive++;
      }
    });
    assertFalse("State should be inactive when initialized", state.isActive());
    state.setActive(true);
    assertTrue("State should be active after activation", state.isActive());
    assertFalse("Reversed state should be inactive after activation", state.getReversedState().isActive());
    assertTrue("evtSetActive should have been fired when state was activated", setActive == 1);
    assertTrue("evtStateChanged should have been fired when an inactive state was activated", stateChanged == 1);
    state.setActive(true);
    assertTrue("evtSetActive should have been fired when an active state was activated", setActive == 2);
    assertTrue("evtStateChanged should not have been fired when an active state was activated", stateChanged == 1);
    state.setActive(false);
    assertFalse("State should be inactive after deactivation", state.isActive());
    assertTrue("Reversed state should be active after deactivation", state.getReversedState().isActive());
    assertTrue("evtSetInactive should have been fired when an active state was deactivated", setInactive == 1);
    assertTrue("evtStateChanged should have been fired when an inactive state was deactivated", stateChanged == 2);
    state.setActive(false);
    assertTrue("evtSetInactive should have been fired when an inactive state was deactivated", setInactive == 2);
    assertTrue("evtStateChanged should not have been fired when an inactive state was deactivated", stateChanged == 2);
  }
}
