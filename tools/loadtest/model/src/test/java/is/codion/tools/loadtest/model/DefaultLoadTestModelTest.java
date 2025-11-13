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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.loadtest.model;

import is.codion.common.utilities.user.User;
import is.codion.tools.loadtest.LoadTest;
import is.codion.tools.loadtest.Scenario;
import is.codion.tools.loadtest.Scenario.Performer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.tools.loadtest.Scenario.scenario;
import static is.codion.tools.loadtest.model.LoadTestModel.loadTestModel;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultLoadTestModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final Scenario<Object> SCENARIO = scenario(new Performer<Object>() {
		int counter = 0;

		@Override
		public void perform(Object application) throws Exception {
			if (counter++ % 2 == 0) {
				throw new Exception();
			}
		}
	});

	private static final Scenario<Object> SCENARIO_II = scenario(application -> {});

	@Test
	void test() throws Exception {
		LoadTest<Object> loadTest = LoadTest.builder()
						.createApplication(user -> new Object())
						.closeApplication(object -> {})
						.user(UNIT_TEST_USER)
						.scenarios(asList(SCENARIO, SCENARIO_II))
						.minimumThinkTime(25)
						.maximumThinkTime(50)
						.loginDelayFactor(2)
						.applicationBatchSize(2)
						.build();
		LoadTestModel<Object> model = loadTestModel(loadTest);
		assertEquals(2, loadTest.applications().batchSize().get());
		model.chartStatistics().set(true);

		assertNotNull(model.memoryUsageDataset());
		assertNotNull(model.numberOfApplicationsDataset());
		assertNotNull(model.thinkTimeDataset());
		assertNotNull(model.scenarioDataset());

		assertEquals(2, loadTest.applications().loginDelayFactor().get());
		loadTest.applications().loginDelayFactor().set(3);
		assertEquals(3, loadTest.applications().loginDelayFactor().get());
		Assertions.assertEquals(DefaultLoadTestModel.DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS, model.chartUpdateInterval().get());
		assertEquals(2, loadTest.applications().batchSize().get());

		assertEquals(25, loadTest.thinkTime().minimum().get());
		assertEquals(50, loadTest.thinkTime().maximum().get());
		loadTest.thinkTime().maximum().set(40);
		loadTest.thinkTime().minimum().set(20);
		assertEquals(20, loadTest.thinkTime().minimum().get());
		assertEquals(40, loadTest.thinkTime().maximum().get());

		loadTest.applications().batchSize().set(5);
		assertTrue(loadTest.scenarios().contains(SCENARIO));
		loadTest.applications().user().set(UNIT_TEST_USER);
		assertEquals(UNIT_TEST_USER, loadTest.applications().user().get());
		assertNotNull(loadTest.randomizer());
		loadTest.randomizer().weight(SCENARIO).set(2);
		loadTest.randomizer().enabled(SCENARIO_II).set(false);
		loadTest.applications().addBatch();
		Thread.sleep(500);
		loadTest.paused().set(true);
		Thread.sleep(200);
		loadTest.paused().set(false);
		assertEquals(5, loadTest.applications().count().get());
		assertEquals(0, model.totalRunCount(SCENARIO_II.name()));
		assertTrue(model.successfulRunCount(SCENARIO.name()) > 0);
		assertTrue(model.unsuccessfulRunCount(SCENARIO.name()) > 0);
		assertFalse(model.exceptions(SCENARIO.name()).isEmpty());
		model.clearExceptions(SCENARIO.name());
		assertTrue(model.exceptions(SCENARIO.name()).isEmpty());
		assertEquals(model.successfulRunCount(SCENARIO.name()) + model.unsuccessfulRunCount(SCENARIO.name()), model.totalRunCount(SCENARIO.name()));
		model.resetStatistics();
		assertEquals(0, model.successfulRunCount(SCENARIO.name()));
		assertEquals(0, model.unsuccessfulRunCount(SCENARIO.name()));

		model.applicationTableModel().items().refresh();
		model.applicationTableModel().selection().index().set(0);
		model.removeSelectedApplications();
//    assertEquals(4, loadTest.applicationCount().get()); //todo flaky in CI

		model.clearCharts();
		loadTest.applications().removeBatch();
//    assertEquals(0, loadTest.applicationCount().get()); //flaky in CI

		AtomicInteger exitCounter = new AtomicInteger();
		loadTest.shuttingDown().addListener(exitCounter::incrementAndGet);
		loadTest.shutdown();
		assertEquals(1, exitCounter.get());
	}

	@Test
	void setUpdateIntervalNegative() {
		LoadTest<Object> loadTest = LoadTest.builder()
						.createApplication(user -> new Object())
						.closeApplication(object -> {})
						.user(User.user("test"))
						.build();
		LoadTestModel<Object> model = loadTestModel(loadTest);
		assertThrows(IllegalArgumentException.class, () -> model.chartUpdateInterval().set(-1));
	}
}
