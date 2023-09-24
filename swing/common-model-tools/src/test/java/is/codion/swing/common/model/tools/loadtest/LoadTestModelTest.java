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
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.tools.loadtest;

import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class LoadTestModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final UsageScenario<Object> SCENARIO = new AbstractUsageScenario<Object>("test") {
    int counter = 0;
    @Override
    protected void perform(Object application) throws Exception {
      if (counter++ % 2 == 0) {
        throw new Exception();
      }
    }
  };

  private static final UsageScenario<Object> SCENARIO_II = new AbstractUsageScenario<Object>("testII") {
    @Override
    protected void perform(Object application) {}
  };

  @Test
  void unknownUsageScenario() {
    LoadTestModel<Object> model = LoadTestModel.builder(user -> new Object(), object -> {})
            .user(User.user("test"))
            .usageScenarios(asList(SCENARIO, SCENARIO_II))
            .minimumThinkTime(25)
            .maximumThinkTime(50)
            .loginDelayFactor(2)
            .applicationBatchSize(2)
            .build();
    assertThrows(IllegalArgumentException.class, () -> model.usageScenario("bla"));
  }

  @Test
  void test() throws Exception {
    LoadTestModel<Object> model = LoadTestModel.builder(user -> new Object(), object -> {})
            .user(UNIT_TEST_USER)
            .usageScenarios(asList(SCENARIO, SCENARIO_II))
            .minimumThinkTime(25)
            .maximumThinkTime(50)
            .loginDelayFactor(2)
            .applicationBatchSize(2)
            .build();
    assertEquals(2, model.applicationBatchSize().get());
    model.collectChartData().set(true);

    assertNotNull(model.memoryUsageDataset());
    assertNotNull(model.numberOfApplicationsDataset());
    assertNotNull(model.thinkTimeDataset());
    assertNotNull(model.usageScenarioDataset());

    assertEquals(2, model.loginDelayFactor().get());
    model.loginDelayFactor().set(3);
    assertEquals(3, model.loginDelayFactor().get());
    assertEquals(DefaultLoadTestModel.DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS, model.getUpdateInterval());
    assertEquals(2, model.applicationBatchSize().get());

    assertEquals(25, model.minimumThinkTime().get());
    assertEquals(50, model.maximumThinkTime().get());
    model.maximumThinkTime().set(40);
    model.minimumThinkTime().set(20);
    assertEquals(20, model.minimumThinkTime().get());
    assertEquals(40, model.maximumThinkTime().get());

    model.applicationBatchSize().set(5);
    assertTrue(model.usageScenarios().contains(SCENARIO.name()));
    model.user().set(UNIT_TEST_USER);
    assertEquals(UNIT_TEST_USER, model.user().get());
    assertNotNull(model.scenarioChooser());
    model.setWeight(SCENARIO.name(), 2);
    model.setScenarioEnabled(SCENARIO_II.name(), false);
    model.addApplicationBatch();
    Thread.sleep(500);
    model.paused().set(true);
    Thread.sleep(200);
    model.paused().set(false);
    assertEquals(5, model.applicationCount().get());
    assertEquals(0, SCENARIO_II.totalRunCount());
    assertTrue(SCENARIO.successfulRunCount() > 0);
    assertTrue(SCENARIO.unsuccessfulRunCount() > 0);
    assertFalse(SCENARIO.exceptions().isEmpty());
    SCENARIO.clearExceptions();
    assertEquals(0, SCENARIO.exceptions().size());
    assertEquals(SCENARIO.successfulRunCount() + SCENARIO.unsuccessfulRunCount(), SCENARIO.totalRunCount());
    SCENARIO.resetRunCount();
    assertEquals(0, SCENARIO.successfulRunCount());
    assertEquals(0, SCENARIO.unsuccessfulRunCount());
    model.clearChartData();
    model.removeApplicationBatch();
    assertEquals(0, model.applicationCount().get());

    AtomicInteger exitCounter = new AtomicInteger();
    model.addShutdownListener(exitCounter::incrementAndGet);
    model.shutdown();
    assertEquals(1, exitCounter.get());
  }
}
