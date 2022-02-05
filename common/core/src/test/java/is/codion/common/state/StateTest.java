/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;
import is.codion.common.event.EventListener;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class StateTest {

  @Test
  void listeners() {
    final State state = State.state();
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
  void reversedState() {
    final AtomicInteger stateCounter = new AtomicInteger();
    final EventListener listener = stateCounter::incrementAndGet;
    final AtomicInteger reversedStateCounter = new AtomicInteger();
    final EventListener reversedListener = reversedStateCounter::incrementAndGet;
    final AtomicInteger reversedReversedStateCounter = new AtomicInteger();
    final EventListener reversedReversedListener = reversedReversedStateCounter::incrementAndGet;
    final State state = State.state();
    final StateObserver reversed = state.getReversedObserver();
    final StateObserver reversedReversed = reversed.getReversedObserver();
    state.addListener(listener);
    reversed.addListener(reversedListener);
    reversedReversed.addListener(reversedReversedListener);
    assertNotEquals(reversed.get(), state.get());
    assertEquals(state.get(), reversedReversed.get());
    state.set(true);
    assertEquals(1, stateCounter.get());
    assertEquals(1, reversedStateCounter.get());
    assertEquals(1, reversedReversedStateCounter.get());
    assertNotEquals(state.get(), reversed.get());
    assertEquals(state.get(), reversedReversed.get());
    state.set(false);
    assertEquals(2, stateCounter.get());
    assertEquals(2, reversedStateCounter.get());
    assertEquals(2, reversedReversedStateCounter.get());
    assertNotEquals(state.get(), reversed.get());
    assertEquals(state.get(), reversedReversed.get());
  }

  @Test
  void test() {
    final State state = State.state();
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
  void group() throws Exception {
    final State stateOne = State.state(true);
    final State stateTwo = State.state(true);
    final State stateThree = State.state(true);
    final State.Group stateGroup = State.group(stateOne);

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
  void stateCombination() {
    State.Combination orState = State.or();
    final State stateOne = State.state();
    final State stateTwo = State.state();
    final State stateThree = State.state();
    orState.addState(stateOne);
    orState.addState(stateTwo);
    orState.addState(stateThree);

    State.Combination andState = State.and(stateOne, stateTwo, stateThree);
    assertEquals(Conjunction.AND, andState.getConjunction());
    assertEquals("Combination and false, false, false, false", andState.toString());

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
    orState = State.or();
    orState.addState(stateOne);
    orState.addState(stateTwo);
    orState.addState(stateThree);
    andState = State.and(stateOne, stateTwo, stateThree);
    assertEquals("Combination and false, false, false, false", andState.toString());

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
  void stateCombinationDataListener() {
    final State one = State.state();
    final State two = State.state();
    final State three = State.state();

    final StateObserver combinationAnd = State.and(one, two, three);
    combinationAnd.addDataListener(newValue -> assertEquals(combinationAnd.get(), newValue));
    one.set(true);
    two.set(true);
    three.set(true);
    one.set(false);
    two.set(false);
    three.set(false);

    final StateObserver combinationOr = State.or(one, two, three);
    combinationOr.addDataListener(newValue -> assertEquals(combinationOr.get(), newValue));
    one.set(true);
    one.set(false);
    two.set(true);
    two.set(false);
    three.set(true);
    three.set(false);
  }

  @Test
  void stateCombinationEvents() {
    final State stateOne = State.state(false);
    final State stateTwo = State.state(true);
    final State.Combination combination = State.or(stateOne, stateTwo);
    final AtomicInteger stateChangeEvents = new AtomicInteger();
    combination.addListener(stateChangeEvents::incrementAndGet);
    assertTrue(combination.get());

    combination.removeState(stateTwo);

    assertFalse(combination.get());
    assertEquals(1, stateChangeEvents.get());

    combination.addState(stateTwo);
    assertTrue(combination.get());
    assertEquals(2, stateChangeEvents.get());

    combination.removeState(stateOne);
    assertTrue(combination.get());
    assertEquals(2, stateChangeEvents.get());

    combination.addState(stateOne);
    stateTwo.set(false);
    assertFalse(combination.get());
    assertEquals(3, stateChangeEvents.get());
  }
}
