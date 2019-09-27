/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class StatesTest {

  @Test
  public void listeners() {
    final State state = States.state();
    state.getChangeObserver();
    final AtomicInteger stateChangeCounter = new AtomicInteger();
    final EventListener stateChangeListener = stateChangeCounter::incrementAndGet;
    state.addListener(stateChangeListener);
    //this has no effect, coverage whoring
    state.getObserver().addListener(stateChangeListener);

    state.set(true);
    assertEquals(1, stateChangeCounter.get());
    state.set(false);
    assertEquals(2, stateChangeCounter.get());
    state.set(true);
    assertEquals(3, stateChangeCounter.get());
    state.set(false);
    state.removeListener(stateChangeListener);
    //this has no effect, coverage whoring
    state.getObserver().removeListener(stateChangeListener);

    state.set(false);
    state.set(true);
    assertEquals(4, stateChangeCounter.get());
  }

  @Test
  public void reversedState() {
    final AtomicInteger stateCounter = new AtomicInteger();
    final EventListener listener = stateCounter::incrementAndGet;
    final AtomicInteger reversedStateCounter = new AtomicInteger();
    final EventListener reversedListener = reversedStateCounter::incrementAndGet;
    final AtomicInteger reversedReversedStateCounter = new AtomicInteger();
    final EventListener reversedReversedListener = reversedReversedStateCounter::incrementAndGet;
    final State state = States.state();
    final StateObserver reversed = state.getReversedObserver();
    final StateObserver reversedReversed = reversed.getReversedObserver();
    state.addListener(listener);
    reversed.addListener(reversedListener);
    reversedReversed.addListener(reversedReversedListener);
    assertTrue(state.get() != reversed.get());
    assertEquals(state.get(), reversedReversed.get());
    state.set(true);
    assertEquals(1, stateCounter.get());
    assertEquals(1, reversedStateCounter.get());
    assertEquals(1, reversedReversedStateCounter.get());
    assertTrue(state.get() != reversed.get());
    assertEquals(state.get(), reversedReversed.get());
    state.set(false);
    assertEquals(2, stateCounter.get());
    assertEquals(2, reversedStateCounter.get());
    assertEquals(2, reversedReversedStateCounter.get());
    assertTrue(state.get() != reversed.get());
    assertEquals(state.get(), reversedReversed.get());
  }

  @Test
  public void test() {
    final State state = States.state();
    assertFalse(state.get(), "State should be inactive when initialized");
    state.set(true);
    assertTrue(state.get(), "State should be active after activation");
    assertEquals("true", state.toString());
    assertFalse(state.getReversedObserver().get(), "Reversed state should be inactive after activation");
    state.set(true);
    state.set(false);
    assertFalse(state.get(), "State should be inactive after deactivation");
    assertEquals("false", state.toString());
    assertTrue(state.getReversedObserver().get(), "Reversed state should be active after deactivation");
  }

  @Test
  public void group() throws Exception {
    final State stateOne = States.state(true);
    final State stateTwo = States.state(true);
    final State stateThree = States.state(true);
    final State.Group stateGroup = States.group(stateOne);

    stateGroup.addState(stateOne);//has no effect
    stateGroup.addState(stateTwo);
    assertFalse(stateOne.get());
    assertTrue(stateTwo.get());
    stateGroup.addState(stateThree);
    assertFalse(stateOne.get());
    assertFalse(stateTwo.get());
    assertTrue(stateThree.get());

    stateOne.set(true);
    assertFalse(stateTwo.get());
    assertFalse(stateThree.get());
  }

  @Test
  public void aggregateStateSetActive() {
    final State.AggregateState orState = States.aggregateState(Conjunction.OR);
    assertThrows(UnsupportedOperationException.class, () -> orState.set(true));
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
    assertEquals("Aggregate and false, false, false, false", andState.toString());

    assertFalse(orState.get(), "Or state should be inactive");
    assertFalse(andState.get(), "And state should be inactive");

    stateOne.set(true);

    assertTrue(orState.get(), "Or state should be active when one component state is active");
    assertFalse(andState.get(), "And state should be inactive when only one component state is active");

    stateTwo.set(true);

    assertTrue(orState.get(), "Or state should be active when two component states are active");
    assertFalse(andState.get(), "And state should be inactive when only two of three component states is active");

    stateThree.set(true);

    assertTrue(orState.get(), "Or state should be active when all component states are active");
    assertTrue(andState.get(), "And state should be active when all component states are active");

    stateOne.set(false);

    assertTrue(orState.get(), "Or state should be active when two component states are active");
    assertFalse(andState.get(), "And state should be inactive when only two of three component states is active");

    andState.removeState(stateOne);
    assertTrue(andState.get());

    stateOne.set(false);
    stateTwo.set(false);
    stateThree.set(false);
    orState = States.aggregateState(Conjunction.OR);
    orState.addState(stateOne);
    orState.addState(stateTwo);
    orState.addState(stateThree);
    andState = States.aggregateState(Conjunction.AND, stateOne, stateTwo, stateThree);
    assertEquals("Aggregate and false, false, false, false", andState.toString());

    assertFalse(orState.get(), "Or state should be inactive");
    assertFalse(andState.get(), "And state should be inactive");

    stateOne.set(true);

    assertTrue(orState.get(), "Or state should be active when one component state is active");
    assertFalse(andState.get(), "And state should be inactive when only one component state is active");

    stateTwo.set(true);

    assertTrue(orState.get(), "Or state should be active when two component states are active");
    assertFalse(andState.get(), "And state should be inactive when only two of three component states is active");

    stateThree.set(true);

    assertTrue(orState.get(), "Or state should be active when all component states are active");
    assertTrue(andState.get(), "And state should be active when all component states are active");

    stateOne.set(false);

    assertTrue(orState.get(), "Or state should be active when two component states are active");
    assertFalse(andState.get(), "And state should be inactive when only two of three component states is active");

    andState.removeState(stateOne);
    assertTrue(andState.get());

    stateTwo.set(false);
    assertTrue(orState.get(), "Or state should be active when one component state is active");
    stateThree.set(false);
    assertFalse(orState.get(), "Or state should be inactive when no component state is active");
    stateTwo.set(true);
    assertTrue(orState.get(), "Or state should be active when one component state is active");
  }

  @Test
  public void aggregateStateDataListener() {
    final State one = States.state();
    final State two = States.state();
    final State three = States.state();

    final State aggregateAnd = States.aggregateState(Conjunction.AND, one, two, three);
    aggregateAnd.addDataListener(newValue -> assertEquals(aggregateAnd.get(), newValue));
    one.set(true);
    two.set(true);
    three.set(true);
    one.set(false);
    two.set(false);
    three.set(false);

    final State aggregateOr = States.aggregateState(Conjunction.OR, one, two, three);
    aggregateOr.addDataListener(newValue -> assertEquals(aggregateOr.get(), newValue));
    one.set(true);
    one.set(false);
    two.set(true);
    two.set(false);
    three.set(true);
    three.set(false);
  }

  @Test
  public void aggregateStateEvents() {
    final State stateOne = States.state(false);
    final State stateTwo = States.state(true);
    final State.AggregateState aggregate = States.aggregateState(Conjunction.OR, stateOne, stateTwo);
    final AtomicInteger stateChangeEvents = new AtomicInteger();
    aggregate.addListener(stateChangeEvents::incrementAndGet);
    assertTrue(aggregate.get());

    aggregate.removeState(stateTwo);

    assertFalse(aggregate.get());
    assertEquals(1, stateChangeEvents.get());

    aggregate.addState(stateTwo);
    assertTrue(aggregate.get());
    assertEquals(2, stateChangeEvents.get());

    aggregate.removeState(stateOne);
    assertTrue(aggregate.get());
    assertEquals(2, stateChangeEvents.get());

    aggregate.addState(stateOne);
    stateTwo.set(false);
    assertFalse(aggregate.get());
    assertEquals(3, stateChangeEvents.get());
  }
}
