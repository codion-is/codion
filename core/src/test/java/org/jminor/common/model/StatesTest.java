/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class StatesTest {

  @Test
  public void listeners() {
    final State state = States.state();
    state.getChangeObserver();
    final AtomicInteger stateChangeCounter = new AtomicInteger();
    final EventListener stateChangeListener = new EventListener() {
      @Override
      public void eventOccurred() {
        stateChangeCounter.incrementAndGet();
      }
    };
    final AtomicInteger activationCounter = new AtomicInteger();
    final EventListener activationListener = new EventListener() {
      @Override
      public void eventOccurred() {
        activationCounter.incrementAndGet();
      }
    };
    final AtomicInteger deactivationCounter = new AtomicInteger();
    final EventListener deactivationListener = new EventListener() {
      @Override
      public void eventOccurred() {
        deactivationCounter.incrementAndGet();
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
    assertEquals(1, activationCounter.get());
    assertEquals(1, stateChangeCounter.get());
    state.setActive(false);
    assertEquals(2, stateChangeCounter.get());
    assertEquals(1, activationCounter.get());
    assertEquals(1, deactivationCounter.get());
    state.setActive(true);
    assertEquals(3, stateChangeCounter.get());
    assertEquals(2, activationCounter.get());
    state.setActive(false);
    assertEquals(2, deactivationCounter.get());
    state.removeActivateListener(activationListener);
    state.removeDeactivateListener(activationListener);
    state.removeListener(stateChangeListener);
    //these have no effect, coverage whoring
    state.getObserver().removeActivateListener(activationListener);
    state.getObserver().removeDeactivateListener(activationListener);
    state.getObserver().removeListener(stateChangeListener);

    state.setActive(false);
    state.setActive(true);
    assertEquals(2, activationCounter.get());
    assertEquals(2, deactivationCounter.get());
    assertEquals(4, stateChangeCounter.get());
  }

  @Test
  public void reversedState() {
    final AtomicInteger stateCounter = new AtomicInteger();
    final EventListener listener = new EventListener() {
      @Override
      public void eventOccurred() {
        stateCounter.incrementAndGet();
      }
    };
    final AtomicInteger reversedStateCounter = new AtomicInteger();
    final EventListener reversedListener = new EventListener() {
      @Override
      public void eventOccurred() {
        reversedStateCounter.incrementAndGet();
      }
    };
    final AtomicInteger reversedReversedStateCounter = new AtomicInteger();
    final EventListener reversedReversedListener = new EventListener() {
      @Override
      public void eventOccurred() {
        reversedReversedStateCounter.incrementAndGet();
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
    assertEquals(1, stateCounter.get());
    assertEquals(1, reversedStateCounter.get());
    assertEquals(1, reversedReversedStateCounter.get());
    assertTrue(state.isActive() != reversed.isActive());
    assertTrue(state.isActive() == reversedReversed.isActive());
    state.setActive(false);
    assertEquals(2, stateCounter.get());
    assertEquals(2, reversedStateCounter.get());
    assertEquals(2, reversedReversedStateCounter.get());
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
    final State.Group stateGroup = States.group(stateOne);

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

  @Test(expected = UnsupportedOperationException.class)
  public void aggregateStateSetActive() {
    final State.AggregateState orState = States.aggregateState(Conjunction.OR);
    orState.setActive(true);
  }

  @Test
  public void aggregateState() {
    State.AggregateState orState = States.aggregateState(Conjunction.OR);
    final State stateOne = States.state();
    final State stateTwo = States.state();
    final State stateThree = States.state();
    orState.addState(stateOne);
    orState.addState(stateTwo);
    orState.addState(stateThree);

    State.AggregateState andState = States.aggregateState(Conjunction.AND, stateOne, stateTwo, stateThree);
    assertEquals(Conjunction.AND, andState.getConjunction());
    assertEquals("Aggregate and inactive, inactive, inactive, inactive", andState.toString());

    assertFalse("Or state should be inactive", orState.isActive());
    assertFalse("And state should be inactive", andState.isActive());

    stateOne.setActive(true);

    assertTrue("Or state should be active when one component state is active", orState.isActive());
    assertFalse("And state should be inactive when only one component state is active", andState.isActive());

    stateTwo.setActive(true);

    assertTrue("Or state should be active when two component states are active", orState.isActive());
    assertFalse("And state should be inactive when only two of three component states is active", andState.isActive());

    stateThree.setActive(true);

    assertTrue("Or state should be active when all component states are active", orState.isActive());
    assertTrue("And state should be active when all component states are active", andState.isActive());

    stateOne.setActive(false);

    assertTrue("Or state should be active when two component states are active", orState.isActive());
    assertFalse("And state should be inactive when only two of three component states is active", andState.isActive());

    andState.removeState(stateOne);
    assertTrue(andState.isActive());

    stateOne.setActive(false);
    stateTwo.setActive(false);
    stateThree.setActive(false);
    orState = States.aggregateState(Conjunction.OR);
    orState.addState(stateOne);
    orState.addState(stateTwo);
    orState.addState(stateThree);
    andState = States.aggregateState(Conjunction.AND, stateOne, stateTwo, stateThree);
    assertEquals("Aggregate and inactive, inactive, inactive, inactive", andState.toString());

    assertFalse("Or state should be inactive", orState.isActive());
    assertFalse("And state should be inactive", andState.isActive());

    stateOne.setActive(true);

    assertTrue("Or state should be active when one component state is active", orState.isActive());
    assertFalse("And state should be inactive when only one component state is active", andState.isActive());

    stateTwo.setActive(true);

    assertTrue("Or state should be active when two component states are active", orState.isActive());
    assertFalse("And state should be inactive when only two of three component states is active", andState.isActive());

    stateThree.setActive(true);

    assertTrue("Or state should be active when all component states are active", orState.isActive());
    assertTrue("And state should be active when all component states are active", andState.isActive());

    stateOne.setActive(false);

    assertTrue("Or state should be active when two component states are active", orState.isActive());
    assertFalse("And state should be inactive when only two of three component states is active", andState.isActive());

    andState.removeState(stateOne);
    assertTrue(andState.isActive());

    stateTwo.setActive(false);
    assertTrue("Or state should be active when one component state is active", orState.isActive());
    stateThree.setActive(false);
    assertFalse("Or state should be inactive when no component state is active", orState.isActive());
    stateTwo.setActive(true);
    assertTrue("Or state should be active when one component state is active", orState.isActive());
  }

  @Test
  public void aggregateStateEvents() {
    final State stateOne = States.state(false);
    final State stateTwo = States.state(true);
    final State.AggregateState aggregate = States.aggregateState(Conjunction.OR, stateOne, stateTwo);
    final AtomicInteger stateChangeEvents = new AtomicInteger();
    aggregate.addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        stateChangeEvents.incrementAndGet();
      }
    });
    final AtomicInteger stateActivatedEvents = new AtomicInteger();
    aggregate.addActivateListener(new EventListener() {
      @Override
      public void eventOccurred() {
        stateActivatedEvents.incrementAndGet();
      }
    });
    final AtomicInteger stateDeactivatedEvents = new AtomicInteger();
    aggregate.addDeactivateListener(new EventListener() {
      @Override
      public void eventOccurred() {
        stateDeactivatedEvents.incrementAndGet();
      }
    });

    assertTrue(aggregate.isActive());

    aggregate.removeState(stateTwo);

    assertFalse(aggregate.isActive());
    assertEquals(1, stateChangeEvents.get());
    assertEquals(1, stateDeactivatedEvents.get());
    assertEquals(0, stateActivatedEvents.get());

    aggregate.addState(stateTwo);
    assertTrue(aggregate.isActive());
    assertEquals(2, stateChangeEvents.get());
    assertEquals(1, stateDeactivatedEvents.get());
    assertEquals(1, stateActivatedEvents.get());

    aggregate.removeState(stateOne);
    assertTrue(aggregate.isActive());
    assertEquals(2, stateChangeEvents.get());
    assertEquals(1, stateDeactivatedEvents.get());
    assertEquals(1, stateActivatedEvents.get());

    aggregate.addState(stateOne);
    stateTwo.setActive(false);
    assertFalse(aggregate.isActive());
    assertEquals(3, stateChangeEvents.get());
    assertEquals(2, stateDeactivatedEvents.get());
    assertEquals(1, stateActivatedEvents.get());
  }
}
