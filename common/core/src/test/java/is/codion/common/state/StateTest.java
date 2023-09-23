/*
 * Copyright (c) 2015 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class StateTest {

  @Test
  void listeners() {
    State state = State.state();
    AtomicInteger stateChangeCounter = new AtomicInteger();
    Runnable stateChangeListener = stateChangeCounter::incrementAndGet;
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
  void not() {
    AtomicInteger stateCounter = new AtomicInteger();
    Runnable listener = stateCounter::incrementAndGet;
    AtomicInteger notStateCounter = new AtomicInteger();
    Runnable notListener = notStateCounter::incrementAndGet;
    AtomicInteger notNotStateCounter = new AtomicInteger();
    Runnable notNotListener = notNotStateCounter::incrementAndGet;
    State state = State.state();
    StateObserver not = state.not();
    StateObserver notNot = not.not();
    assertSame(state.observer(), notNot);
    state.addListener(listener);
    not.addListener(notListener);
    notNot.addListener(notNotListener);
    assertNotEquals(not.get(), state.get());
    assertEquals(state.get(), notNot.get());
    state.set(true);
    assertFalse(not.optional().orElse(null));
    assertEquals(1, stateCounter.get());
    assertEquals(1, notStateCounter.get());
    assertEquals(1, notNotStateCounter.get());
    assertNotEquals(state.get(), not.get());
    assertEquals(state.get(), notNot.get());
    state.set(false);
    assertEquals(2, stateCounter.get());
    assertEquals(2, notStateCounter.get());
    assertEquals(2, notNotStateCounter.get());
    assertNotEquals(state.get(), not.get());
    assertEquals(state.get(), notNot.get());
  }

  @Test
  void test() {
    State state = State.state();
    assertFalse(state.get(), "State should be inactive when initialized");
    assertFalse(state.isNull());
    assertTrue(state.isNotNull());
    assertFalse(state.isNullable());
    assertFalse(state.equalTo(true));
    assertTrue(state.optional().isPresent());
    state.accept(true);//calls set()
    assertTrue(state.get(), "State should be active after activation");
    assertEquals("true", state.toString());
    assertFalse(state.not().get(), "Not state should be inactive after activation");
    state.set(true);
    state.set(false);
    assertFalse(state.get(), "State should be inactive after deactivation");
    assertEquals("false", state.toString());
    assertTrue(state.not().get(), "Not state should be active after deactivation");
  }

  @Test
  void group() {
    State stateOne = State.state(true);
    State stateTwo = State.state(true);
    State stateThree = State.state(true);
    State.Group stateGroup = State.group(stateOne);

    stateGroup.add(stateOne);//has no effect
    stateGroup.add(stateTwo);
    assertFalse(stateOne.get());
    assertTrue(stateTwo.get());
    stateGroup.add(stateThree);
    assertFalse(stateOne.get());
    assertFalse(stateTwo.get());
    assertTrue(stateThree.get());

    stateOne.set(true);
    assertFalse(stateTwo.get());
    assertFalse(stateThree.get());

    stateGroup = State.group(Arrays.asList(stateOne, stateTwo));
    stateGroup.add(Collections.singletonList(stateThree));
  }

  @Test
  void stateCombination() {
    State stateOne = State.state();
    State stateTwo = State.state();
    State stateThree = State.state();
    State.Combination orState = State.or(Arrays.asList(stateOne, stateTwo, stateThree));

    State.Combination andState = State.and(Arrays.asList(stateOne, stateTwo, stateThree));
    assertEquals(Conjunction.AND, andState.conjunction());
    assertEquals("Combination and false, false, false, false", andState.toString());

    assertFalse(orState.get(), "Or state should be inactive");
    assertFalse(andState.get(), "And state should be inactive");
    assertTrue(orState.not().get(), "Reversed Or state should be active");
    assertTrue(andState.not().get(), "Reversed And state should be active");

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

    andState.remove(stateOne);
    assertTrue(andState.get());

    stateOne.set(false);
    stateTwo.set(false);
    stateThree.set(false);
    orState = State.or();
    orState.add(stateOne);
    orState.add(stateTwo);
    orState.add(stateThree);
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

    andState.remove(stateOne);
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
    Runnable listener = () -> {};
    Consumer<Boolean> dataListener = newValue -> assertEquals(combinationAnd.get(), newValue);
    combinationAnd.addListener(listener);
    combinationAnd.addDataListener(dataListener);
    one.set(true);
    two.set(true);
    three.set(true);
    one.set(false);
    two.set(false);
    three.set(false);
    combinationAnd.removeListener(listener);
    combinationAnd.removeDataListener(dataListener);

    StateObserver combinationOr = State.combination(Conjunction.OR, one, two, three);
    dataListener = newValue -> assertEquals(combinationOr.get(), newValue);
    combinationOr.addDataListener(dataListener);
    one.set(true);
    one.set(false);
    two.set(true);
    two.set(false);
    three.set(true);
    three.set(false);
    combinationOr.removeDataListener(dataListener);
  }

  @Test
  void stateCombinationEvents() {
    State stateOne = State.state(false);
    State stateTwo = State.state(true);
    State.Combination combination = State.or(stateOne, stateTwo);
    AtomicInteger stateChangeEvents = new AtomicInteger();
    combination.addListener(stateChangeEvents::incrementAndGet);
    assertTrue(combination.get());

    combination.remove(stateTwo);

    assertFalse(combination.get());
    assertEquals(1, stateChangeEvents.get());

    combination.add(stateTwo);
    assertTrue(combination.get());
    assertEquals(2, stateChangeEvents.get());

    combination.remove(stateOne);
    assertTrue(combination.get());
    assertEquals(2, stateChangeEvents.get());

    combination.add(stateOne);
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
  void weakListeners() {
    State state = State.state();
    Runnable listener = () -> {};
    Consumer<Boolean> dataListener = bool -> {};
    state.addWeakListener(listener);
    state.addWeakListener(listener);
    state.addWeakDataListener(dataListener);
    state.addWeakDataListener(dataListener);
    state.set(true);
    state.removeWeakListener(listener);
    state.removeWeakDataListener(dataListener);

    State state2 = State.state();
    StateObserver combination = State.combination(Conjunction.AND, state, state2);
    combination.addWeakListener(listener);
    combination.addWeakListener(listener);
    combination.addWeakDataListener(dataListener);
    combination.addWeakDataListener(dataListener);
    state2.set(true);
    combination.removeWeakListener(listener);
    combination.removeWeakDataListener(dataListener);
  }
}
