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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.loadtest;

import is.codion.common.utilities.user.User;
import is.codion.tools.loadtest.Scenario.Performer;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultLoadTestTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final Scenario<Object> SCENARIO = Scenario.scenario(new Performer<Object>() {
		int counter = 0;

		@Override
		public void perform(Object application) throws Exception {
			if (counter++ % 2 == 0) {
				throw new Exception();
			}
		}
	});

	private static final Scenario<Object> SCENARIO_II = Scenario.scenario(application -> {});

	@Test
	void test() throws Exception {
		LoadTest<Object> model = LoadTest.builder()
						.createApplication(user -> new Object())
						.closeApplication(object -> {})
						.user(UNIT_TEST_USER)
						.scenarios(asList(SCENARIO, SCENARIO_II))
						.minimumThinkTime(25)
						.maximumThinkTime(50)
						.loginDelayFactor(2)
						.applicationBatchSize(2)
						.build();
		Map<String, List<Scenario.Result>> results = new HashMap<>();
		model.result().addConsumer(result -> results.computeIfAbsent(result.scenario(), scenarioName -> new ArrayList<>()).add(result));
		assertEquals(2, model.applications().batchSize().get());

		assertEquals(2, model.applications().loginDelayFactor().get());
		model.applications().loginDelayFactor().set(3);
		assertEquals(3, model.applications().loginDelayFactor().get());
		assertEquals(2, model.applications().batchSize().get());

		assertEquals(25, model.thinkTime().minimum().get());
		assertEquals(50, model.thinkTime().maximum().get());
		model.thinkTime().maximum().set(40);
		model.thinkTime().minimum().set(20);
		assertEquals(20, model.thinkTime().minimum().get());
		assertEquals(40, model.thinkTime().maximum().get());

		model.applications().batchSize().set(5);
		assertTrue(model.scenarios().contains(SCENARIO));
		model.applications().user().set(UNIT_TEST_USER);
		assertEquals(UNIT_TEST_USER, model.applications().user().get());
		assertNotNull(model.randomizer());
		model.randomizer().weight(SCENARIO).set(2);
		model.randomizer().enabled(SCENARIO_II).set(false);
		model.applications().addBatch();
		Thread.sleep(500);
		model.paused().set(true);
		Thread.sleep(200);
		model.paused().set(false);
		assertEquals(5, model.applications().count().get());
		assertNull(results.get(SCENARIO_II.name()));
		assertFalse(results.get(SCENARIO.name()).isEmpty());

		model.applications().removeBatch();
		assertEquals(0, model.applications().count().get());

		results.values().forEach(result -> assertFalse(result.isEmpty()));

		AtomicInteger exitCounter = new AtomicInteger();
		model.shuttingDown().addListener(exitCounter::incrementAndGet);
		model.shutdown();
		assertEquals(1, exitCounter.get());
	}

	@Test
	void setLoginDelayFactorNegative() {
		LoadTest<Object> model = LoadTest.builder()
						.createApplication(user -> new Object())
						.closeApplication(object -> {})
						.user(User.user("test"))
						.build();
		assertThrows(IllegalArgumentException.class, () -> model.applications().loginDelayFactor().set(-1));
	}
}
