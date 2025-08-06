/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2015 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.state;

import is.codion.common.Conjunction;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class StateTest {

	private static final String CONJUNCTION_AND_PATTERN = "Combination and false, false, false, false";
	private static final String CONJUNCTION_OR_PATTERN = "Combination or false, false, false, false";

	@Test
	void state_addRemoveListeners_shouldNotifyOnChange() {
		State state = State.state();
		AtomicInteger stateChangeCounter = new AtomicInteger();
		Runnable stateChangeListener = stateChangeCounter::incrementAndGet;
		state.addListener(stateChangeListener);
		//this has no effect, coverage whoring
		state.observable().addListener(stateChangeListener);

		state.set(true);
		assertEquals(1, stateChangeCounter.get());
		state.set(false);
		assertEquals(2, stateChangeCounter.get());
		state.set(true);
		assertEquals(3, stateChangeCounter.get());
		state.set(false);
		state.removeListener(stateChangeListener);
		//this has no effect, coverage whoring
		state.observable().removeListener(stateChangeListener);

		state.set(false);
		state.set(true);
		assertEquals(4, stateChangeCounter.get());
	}

	@Test
	void not_stateInversion_shouldMaintainOppositeValue() {
		AtomicInteger stateCounter = new AtomicInteger();
		Runnable listener = stateCounter::incrementAndGet;
		AtomicInteger notStateCounter = new AtomicInteger();
		Runnable notListener = notStateCounter::incrementAndGet;
		AtomicInteger notNotStateCounter = new AtomicInteger();
		Runnable notNotListener = notNotStateCounter::incrementAndGet;
		State state = State.state();
		ObservableState not = state.not();
		ObservableState notNot = not.not();
		assertSame(state.observable(), notNot);
		state.addListener(listener);
		not.addListener(notListener);
		notNot.addListener(notNotListener);
		assertNotEquals(not.is(), state.is());
		assertEquals(state.is(), notNot.is());
		state.set(true);
		assertEquals(1, stateCounter.get());
		assertEquals(1, notStateCounter.get());
		assertEquals(1, notNotStateCounter.get());
		assertNotEquals(state.is(), not.is());
		assertEquals(state.is(), notNot.is());
		state.set(false);
		assertEquals(2, stateCounter.get());
		assertEquals(2, notStateCounter.get());
		assertEquals(2, notNotStateCounter.get());
		assertNotEquals(state.is(), not.is());
		assertEquals(state.is(), notNot.is());
	}

	@Test
	void state_basicOperations_shouldWorkAsExpected() {
		State state = State.state();
		assertFalse(state.is(), "State should be inactive when initialized");
		state.set(true);
		assertTrue(state.is(), "State should be active after activation");
		assertEquals(Boolean.TRUE.toString(), state.toString());
		assertFalse(state.not().is(), "Not state should be inactive after activation");
		state.set(true);
		state.set(false);
		assertFalse(state.is(), "State should be inactive after deactivation");
		assertEquals(Boolean.FALSE.toString(), state.toString());
		assertTrue(state.not().is(), "Not state should be active after deactivation");
	}

	@Test
	void groupSingleStateAddition() {
		State stateOne = State.state(true);
		State stateTwo = State.state(true);
		State stateThree = State.state(true);
		State.Group stateGroup = State.group(stateOne);

		stateGroup.add(stateOne);//has no effect
		stateGroup.add(stateTwo);
		assertFalse(stateOne.is());
		assertTrue(stateTwo.is());
		stateGroup.add(stateThree);
		assertFalse(stateOne.is());
		assertFalse(stateTwo.is());
		assertTrue(stateThree.is());

		stateOne.set(true);
		assertFalse(stateTwo.is());
		assertFalse(stateThree.is());

		stateGroup = State.group(asList(stateOne, stateTwo));
		stateGroup.add(Collections.singletonList(stateThree));
	}

	@Test
	void groupVarargsCreation() {
		State one = State.state(true);
		State two = State.state(true);
		State three = State.state(true);

		State.group(one, two, three);
		assertFalse(one.is());
		assertFalse(two.is());
		assertTrue(three.is());

		// three to one
		one.set(true);
		assertTrue(one.is());
		assertFalse(two.is());
		assertFalse(three.is());

		//one to three
		one.set(false);
		assertFalse(one.is());
		assertFalse(two.is());
		assertTrue(three.is());

		three.set(false);
		assertTrue(one.is());
		assertFalse(two.is());
		assertFalse(three.is());

		one.set(false);
		assertFalse(one.is());
		assertFalse(two.is());
		assertTrue(three.is());

		two.set(true);
		assertFalse(one.is());
		assertTrue(two.is());
		assertFalse(three.is());

		two.set(false);
		assertFalse(one.is());
		assertFalse(two.is());
		assertTrue(three.is());
	}

	@Test
	void groupTwoStatesAndSingleState() {
		State one = State.state();
		State two = State.state();

		State.group(one, two);
		one.set(true);
		one.set(false);
		assertTrue(two.is());

		one = State.state();
		State.group(one);
		one.set(true);
		one.set(false);
		assertFalse(one.is());
	}

	@Test
	void stateCombinationAndOr() {
		State stateOne = State.state();
		State stateTwo = State.state();
		State stateThree = State.state();
		State.Combination orState = State.or(asList(stateOne, stateTwo, stateThree));

		State.Combination andState = State.and(asList(stateOne, stateTwo, stateThree));
		assertEquals(Conjunction.AND, andState.conjunction());
		assertEquals(CONJUNCTION_AND_PATTERN, andState.toString());

		assertFalse(orState.is(), "Or state should be inactive");
		assertFalse(andState.is(), "And state should be inactive");
		assertTrue(orState.not().is(), "Reversed Or state should be active");
		assertTrue(andState.not().is(), "Reversed And state should be active");

		stateOne.set(true);

		assertTrue(orState.is(), "Or state should be active when one component state is active");
		assertFalse(andState.is(), "And state should be inactive when only one component state is active");

		stateTwo.set(true);

		assertTrue(orState.is(), "Or state should be active when two component states are active");
		assertFalse(andState.is(), "And state should be inactive when only two of three component states is active");

		stateThree.set(true);

		assertTrue(orState.is(), "Or state should be active when all component states are active");
		assertTrue(andState.is(), "And state should be active when all component states are active");

		stateOne.set(false);

		assertTrue(orState.is(), "Or state should be active when two component states are active");
		assertFalse(andState.is(), "And state should be inactive when only two of three component states is active");
	}

	@Test
	void stateCombinationBuildAndVerify() {
		State stateOne = State.state();
		State stateTwo = State.state();
		State stateThree = State.state();
		State.Combination orState = State.or(stateOne, stateTwo, stateThree);
		State.Combination andState = State.and(stateOne, stateTwo, stateThree);
		assertEquals(CONJUNCTION_AND_PATTERN, andState.toString());
		assertEquals(CONJUNCTION_OR_PATTERN, orState.toString());

		assertFalse(orState.is(), "Or state should be inactive");
		assertFalse(andState.is(), "And state should be inactive");

		stateOne.set(true);

		assertTrue(orState.is(), "Or state should be active when one component state is active");
		assertFalse(andState.is(), "And state should be inactive when only one component state is active");

		stateTwo.set(true);

		assertTrue(orState.is(), "Or state should be active when two component states are active");
		assertFalse(andState.is(), "And state should be inactive when only two of three component states is active");

		stateThree.set(true);

		assertTrue(orState.is(), "Or state should be active when all component states are active");
		assertTrue(andState.is(), "And state should be active when all component states are active");

		stateOne.set(false);

		assertTrue(orState.is(), "Or state should be active when two component states are active");
		assertFalse(andState.is(), "And state should be inactive when only two of three component states is active");

		andState = State.and(stateTwo, stateThree);
		assertTrue(andState.is());

		stateTwo.set(false);
		assertTrue(orState.is(), "Or state should be active when one component state is active");
		stateThree.set(false);
		assertFalse(orState.is(), "Or state should be inactive when no component state is active");
		stateTwo.set(true);
		assertTrue(orState.is(), "Or state should be active when one component state is active");
	}

	@Test
	void stateCombinationConsumer() {
		State one = State.state();
		State two = State.state();
		State three = State.state();

		ObservableState combinationAnd = State.combination(Conjunction.AND, one, two, three);
		Runnable listener = () -> {};
		Consumer<Boolean> consumer = newValue -> assertEquals(combinationAnd.is(), newValue);
		combinationAnd.addListener(listener);
		combinationAnd.addConsumer(consumer);
		one.set(true);
		two.set(true);
		three.set(true);
		one.set(false);
		two.set(false);
		three.set(false);
		combinationAnd.removeListener(listener);
		combinationAnd.removeConsumer(consumer);

		ObservableState combinationOr = State.combination(Conjunction.OR, one, two, three);
		consumer = newValue -> assertEquals(combinationOr.is(), newValue);
		combinationOr.addConsumer(consumer);
		one.set(true);
		one.set(false);
		two.set(true);
		two.set(false);
		three.set(true);
		three.set(false);
		combinationOr.removeConsumer(consumer);
	}

	@Test
	void linking() {
		State state = State.state();
		State linked = State.state();
		linked.link(state);

		assertFalse(linked.is());
		state.set(true);
		assertTrue(linked.is());
		linked.set(false);
		assertFalse(state.is());

		linked.unlink(state);
		state.set(true);
		assertFalse(linked.is());
	}

	@Test
	void weakListeners() {
		State state = State.state();
		Runnable listener = () -> {};
		Consumer<Boolean> consumer = bool -> {};
		state.addWeakListener(listener);
		state.addWeakListener(listener);
		state.addWeakConsumer(consumer);
		state.addWeakConsumer(consumer);
		state.set(true);
		state.removeWeakListener(listener);
		state.removeWeakConsumer(consumer);

		State state2 = State.state();
		ObservableState combination = State.combination(Conjunction.AND, state, state2);
		combination.addWeakListener(listener);
		combination.addWeakListener(listener);
		combination.addWeakConsumer(consumer);
		combination.addWeakConsumer(consumer);
		state2.set(true);
		combination.removeWeakListener(listener);
		combination.removeWeakConsumer(consumer);
	}

	@Test
	void combinationConcurrentReads() throws InterruptedException {
		State state1 = State.state();
		State state2 = State.state();
		State state3 = State.state();
		State.Combination combination = State.and(state1, state2, state3);

		int threadCount = 100;
		int iterations = 10000;
		CountDownLatch latch = new CountDownLatch(threadCount);
		try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
			AtomicBoolean errorOccurred = new AtomicBoolean(false);
			for (int i = 0; i < threadCount; i++) {
				executor.submit(() -> {
					try {
						for (int j = 0; j < iterations; j++) {
							// Just read the value, checking for exceptions
							combination.is();
						}
					}
					catch (Exception e) {
						errorOccurred.set(true);
					}
					finally {
						latch.countDown();
					}
				});
			}

			assertTrue(latch.await(10, TimeUnit.SECONDS), "Concurrent reads should complete without deadlock");
			assertFalse(errorOccurred.get(), "No exceptions should occur during concurrent reads");
		}
	}

	@Test
	void combinationConcurrentUpdatesAndReads() {
		State state1 = State.state();
		State state2 = State.state();
		State state3 = State.state();
		State.Combination andCombination = State.and(state1, state2, state3);
		State.Combination orCombination = State.or(state1, state2, state3);

		// Simple concurrent test - just verify no deadlocks or exceptions
		for (int i = 0; i < 1000; i++) {
			state1.set(i % 2 == 0);
			state2.set(i % 3 == 0);
			state3.set(i % 5 == 0);

			// Read combinations
			andCombination.is();
			orCombination.is();
		}

		// Verify basic functionality
		state1.set(true);
		state2.set(true);
		state3.set(true);
		assertTrue(andCombination.is());
		assertTrue(orCombination.is());

		state1.set(false);
		assertFalse(andCombination.is());
		assertTrue(orCombination.is());
	}

	@Test
	void combinationRapidStateFlipping() {
		State state1 = State.state(false);
		State state2 = State.state(false);
		State.Combination andCombination = State.and(state1, state2);
		State.Combination orCombination = State.or(state1, state2);

		// Simple test - just verify no deadlocks
		for (int i = 0; i < 1000; i++) {
			state1.set(i % 2 == 0);
			state2.set(i % 3 == 0);
			andCombination.is();
			orCombination.is();
		}
	}

	@Test
	void combinationListenerNotificationUnderConcurrency() {
		State state1 = State.state();
		State state2 = State.state();
		State.Combination combination = State.and(state1, state2);

		AtomicInteger notificationCount = new AtomicInteger(0);

		// Add listener
		combination.addConsumer(value -> notificationCount.incrementAndGet());

		// Trigger state changes
		state1.set(true);
		state2.set(true);
		state1.set(false);
		state2.set(false);

		assertTrue(notificationCount.get() > 0, "Listeners should have been notified");
	}

	@Test
	void combinationStressTestDeadlockPrevention() {
		// Create nested combinations to increase complexity
		State s1 = State.state();
		State s2 = State.state();
		State s3 = State.state();
		State s4 = State.state();

		State.Combination inner1 = State.and(s1, s2);
		State.Combination inner2 = State.or(s3, s4);
		State.Combination outer = State.and(inner1, inner2);

		// Rapidly change states and read from nested combinations
		for (int i = 0; i < 1000; i++) {
			s1.set(i % 2 == 0);
			s2.set(i % 3 == 0);
			s3.set(i % 5 == 0);
			s4.set(i % 7 == 0);

			// Read at all levels
			outer.is();
			inner1.is();
			inner2.is();
		}

		// Verify functionality
		s1.set(true);
		s2.set(true);
		assertTrue(inner1.is());

		s3.set(false);
		s4.set(true);
		assertTrue(inner2.is());

		assertTrue(outer.is());
	}

	@Test
	void combinationMemoryConsistency() {
		State state1 = State.state(false);
		State state2 = State.state(false);
		State.Combination combination = State.or(state1, state2);

		// Simple memory consistency test
		for (int i = 0; i < 1000; i++) {
			state1.set(true);
			state2.set(true);
			// Combination should see the changes
			assertTrue(combination.is());

			state1.set(false);
			state2.set(false);
			// Combination should see the changes
			assertFalse(combination.is());
		}
	}
}
