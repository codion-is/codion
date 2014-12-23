/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

public class StateImplTest {

  @Test
  public void listeners() {
    final State state = States.state();
    state.getChangeObserver();
    final Collection<Object> stateChangeCounter = new ArrayList<>();
    final EventListener stateChangeListener = new EventListener() {
      @Override
      public void eventOccurred() {
        stateChangeCounter.add(new Object());
      }
    };
    final Collection<Object> activationCounter = new ArrayList<>();
    final EventListener activationListener = new EventListener() {
      @Override
      public void eventOccurred() {
        activationCounter.add(new Object());
      }
    };
    final Collection<Object> deactivationCounter = new ArrayList<>();
    final EventListener deactivationListener = new EventListener() {
      @Override
      public void eventOccurred() {
        deactivationCounter.add(new Object());
      }
    };
    state.addListener(stateChangeListener);
    state.addActivateListener(activationListener);
    state.addDeactivateListener(deactivationListener);
    //these have no effect, coverage whoring
    state.getObserver().addListener(stateChangeListener);
    state.getObserver().addActivateListener(activationListener);
    state.getObserver().addDeactivateListener(deactivationListener);

    state.setActive(true);
    assertEquals(1, activationCounter.size());
    assertEquals(1, stateChangeCounter.size());
    state.setActive(false);
    assertEquals(2, stateChangeCounter.size());
    assertEquals(1, activationCounter.size());
    assertEquals(1, deactivationCounter.size());
    state.setActive(true);
    assertEquals(3, stateChangeCounter.size());
    assertEquals(2, activationCounter.size());
    state.setActive(false);
    assertEquals(2, deactivationCounter.size());
    state.removeActivateListener(activationListener);
    state.removeDeactivateListener(activationListener);
    state.removeListener(stateChangeListener);
    //these have no effect, coverage whoring
    state.getObserver().removeActivateListener(activationListener);
    state.getObserver().removeDeactivateListener(activationListener);
    state.getObserver().removeListener(stateChangeListener);

    state.setActive(false);
    state.setActive(true);
    assertEquals(2, activationCounter.size());
    assertEquals(2, deactivationCounter.size());
    assertEquals(4, stateChangeCounter.size());
  }

  @Test
  public void reversedState() {
    final Collection<Object> stateCounter = new ArrayList<>();
    final EventListener listener = new EventListener() {
      @Override
      public void eventOccurred() {
        stateCounter.add(new Object());
      }
    };
    final Collection<Object> reversedStateCounter = new ArrayList<>();
    final EventListener reversedListener = new EventListener() {
      @Override
      public void eventOccurred() {
        reversedStateCounter.add(new Object());
      }
    };
    final Collection<Object> reversedReversedStateCounter = new ArrayList<>();
    final EventListener reversedReversedListener = new EventListener() {
      @Override
      public void eventOccurred() {
        reversedReversedStateCounter.add(new Object());
      }
    };
    final State state = States.state();
    final StateObserver reversed = state.getReversedObserver();
    final StateObserver reversedReversed = reversed.getReversedObserver();
    state.addListener(listener);
    reversed.addListener(reversedListener);
    reversedReversed.addListener(reversedReversedListener);
    assertTrue(state.isActive() != reversed.isActive());
    assertTrue(state.isActive() == reversedReversed.isActive());
    state.setActive(true);
    assertEquals(1, stateCounter.size());
    assertEquals(1, reversedStateCounter.size());
    assertEquals(1, reversedReversedStateCounter.size());
    assertTrue(state.isActive() != reversed.isActive());
    assertTrue(state.isActive() == reversedReversed.isActive());
    state.setActive(false);
    assertEquals(2, stateCounter.size());
    assertEquals(2, reversedStateCounter.size());
    assertEquals(2, reversedReversedStateCounter.size());
    assertTrue(state.isActive() != reversed.isActive());
    assertTrue(state.isActive() == reversedReversed.isActive());
  }

  @Test
  public void test() {
    final State state = States.state();
    assertFalse("State should be inactive when initialized", state.isActive());
    state.setActive(true);
    assertTrue("State should be active after activation", state.isActive());
    assertEquals("active", state.toString());
    assertFalse("Reversed state should be inactive after activation", state.getReversedObserver().isActive());
    state.setActive(true);
    state.setActive(false);
    assertFalse("State should be inactive after deactivation", state.isActive());
    assertEquals("inactive", state.toString());
    assertTrue("Reversed state should be active after deactivation", state.getReversedObserver().isActive());
  }

  @Test
  public void group() throws Exception {
    final State stateOne = States.state(true);
    final State stateTwo = States.state(true);
    final State stateThree = States.state(true);
    final State.Group stateGroup = new States.DefaultGroup();

    stateGroup.addState(stateOne);
    stateGroup.addState(stateOne);//has no effect
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
