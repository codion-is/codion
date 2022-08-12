/*
 * Copyright (c) 2015 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;
import is.codion.common.event.EventListener;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class StateTest {

  @Test
  void listeners() {
    State state = State.state();
    AtomicInteger stateChangeCounter = new AtomicInteger();
    EventListener stateChangeListener = stateChangeCounter::incrementAndGet;
    state.addListener(stateChangeListener);
    //this has no effect, coverage whoring
    state.observer().addListener(stateChangeListener);

    state.set(true);
    assertEquals(1, stateChangeCounter.get());
    state.set(false);
    assertEquals(2, stateChangeCounter.get());
    state.set(true);
    assertEquals(3, stateChangeCounter.get());
    state.set(false);
    state.removeListener(stateChangeListener);
    //this has no effect, coverage whoring
    state.observer().removeListener(stateChangeListener);

    state.set(false);
    state.set(true);
    assertEquals(4, stateChangeCounter.get());
  }

  @Test
  void reversedState() {
    AtomicInteger stateCounter = new AtomicInteger();
    EventListener listener = stateCounter::incrementAndGet;
    AtomicInteger reversedStateCounter = new AtomicInteger();
    EventListener reversedListener = reversedStateCounter::incrementAndGet;
    AtomicInteger reversedReversedStateCounter = new AtomicInteger();
    EventListener reversedReversedListener = reversedReversedStateCounter::incrementAndGet;
    State state = State.state();
    StateObserver reversed = state.reversedObserver();
    StateObserver reversedReversed = reversed.reversedObserver();
    state.addListener(listener);
    reversed.addListener(reversedListener);
    reversedReversed.addListener(reversedReversedListener);
    assertNotEquals(reversed.get(), state.get());
    assertEquals(state.get(), reversedReversed.get());
    state.set(true);
    assertFalse(reversed.toOptional().orElse(null));
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
    State state = State.state();
    assertFalse(state.get(), "State should be inactive when initialized");
    assertFalse(state.isNull());
    assertTrue(state.isNotNull());
    assertFalse(state.isNullable());
    assertFalse(state.equalTo(true));
    assertTrue(state.toOptional().isPresent());
    state.onEvent(true);//calls set()
    assertTrue(state.get(), "State should be active after activation");
    assertEquals("true", state.toString());
    assertFalse(state.reversedObserver().get(), "Reversed state should be inactive after activation");
    state.set(true);
    state.set(false);
    assertFalse(state.get(), "State should be inactive after deactivation");
    assertEquals("false", state.toString());
    assertTrue(state.reversedObserver().get(), "Reversed state should be active after deactivation");
  }

  @Test
  void group() throws Exception {
    State stateOne = State.state(true);
    State stateTwo = State.state(true);
    State stateThree = State.state(true);
    State.Group stateGroup = State.group(stateOne);

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
    State stateOne = State.state();
    State stateTwo = State.state();
    State stateThree = State.state();
    orState.addState(stateOne);
    orState.addState(stateTwo);
    orState.addState(stateThree);

    State.Combination andState = State.and(stateOne, stateTwo, stateThree);
    assertEquals(Conjunction.AND, andState.conjunction());
    assertEquals("Combination and false, false, false, false", andState.toString());

    assertFalse(orState.get(), "Or state should be inactive");
    assertFalse(andState.get(), "And state should be inactive");
    assertTrue(orState.reversedObserver().get(), "Reversed Or state should be active");
    assertTrue(andState.reversedObserver().get(), "Reversed And state should be active");

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
    assertEquals("Combination or false, false, false, false", orState.toString());

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

    assertFalse(orState.isNull());
    assertTrue(orState.isNotNull());
    assertFalse(orState.isNullable());
  }

  @Test
  void stateCombinationDataListener() {
    State one = State.state();
    State two = State.state();
    State three = State.state();

    StateObserver combinationAnd = State.combination(Conjunction.AND, one, two, three);
    combinationAnd.addDataListener(newValue -> assertEquals(combinationAnd.get(), newValue));
    one.set(true);
    two.set(true);
    three.set(true);
    one.set(false);
    two.set(false);
    three.set(false);

    StateObserver combinationOr = State.combination(Conjunction.OR, one, two, three);
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
    State stateOne = State.state(false);
    State stateTwo = State.state(true);
    State.Combination combination = State.or(stateOne, stateTwo);
    AtomicInteger stateChangeEvents = new AtomicInteger();
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

  @Test
  void linking() {
    State state = State.state();
    Value<Boolean> value = Value.value(true);

    value.link(state);
    assertFalse(value.get());
    state.set(true);
    assertTrue(value.get());

    value.unlink(state);
    state.set(false);
    assertTrue(value.get());

    ValueObserver<Boolean> valueObserver = value.observer();
    state.link(valueObserver);
    assertTrue(state.get());
    value.set(false);
    assertFalse(valueObserver.get());

    state.unlink(valueObserver);
    value.set(true);
    assertFalse(state.get());

    state.link(value);
    assertTrue(state.get());
    state.unlink(value);
    value.set(false);
    assertTrue(state.get());
  }

  @Test
  void getterSetterState() {
    State state = State.state(() -> true, value -> {});
    assertTrue(state.get());
    state.set(false);
    assertTrue(state.get());
  }
}
