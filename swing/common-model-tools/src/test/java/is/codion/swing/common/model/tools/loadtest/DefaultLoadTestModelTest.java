/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.tools.loadtest;

import is.codion.common.model.loadtest.AbstractScenario;
import is.codion.common.model.loadtest.LoadTest;
import is.codion.common.model.loadtest.LoadTest.Scenario;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultLoadTestModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final Scenario<Object> SCENARIO = new AbstractScenario<Object>("test") {
    int counter = 0;
    @Override
    protected void perform(Object application) throws Exception {
      if (counter++ % 2 == 0) {
        throw new Exception();
      }
    }
  };

  private static final Scenario<Object> SCENARIO_II = new AbstractScenario<Object>("testII") {
    @Override
    protected void perform(Object application) {}
  };

  @Test
  void test() throws Exception {
    LoadTest<Object> loadTest = LoadTest.builder(user -> new Object(), object -> {})
            .user(UNIT_TEST_USER)
            .scenarios(asList(SCENARIO, SCENARIO_II))
            .minimumThinkTime(25)
            .maximumThinkTime(50)
            .loginDelayFactor(2)
            .applicationBatchSize(2)
            .build();
    LoadTestModel<Object> model = LoadTestModel.loadTestModel(loadTest);
    assertEquals(2, loadTest.applicationBatchSize().get());
    model.collectChartData().set(true);

    assertNotNull(model.memoryUsageDataset());
    assertNotNull(model.numberOfApplicationsDataset());
    assertNotNull(model.thinkTimeDataset());
    assertNotNull(model.scenarioDataset());

    assertEquals(2, loadTest.loginDelayFactor().get());
    loadTest.loginDelayFactor().set(3);
    assertEquals(3, loadTest.loginDelayFactor().get());
    assertEquals(DefaultLoadTestModel.DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS, model.getUpdateInterval());
    assertEquals(2, loadTest.applicationBatchSize().get());

    assertEquals(25, loadTest.minimumThinkTime().get());
    assertEquals(50, loadTest.maximumThinkTime().get());
    loadTest.maximumThinkTime().set(40);
    loadTest.minimumThinkTime().set(20);
    assertEquals(20, loadTest.minimumThinkTime().get());
    assertEquals(40, loadTest.maximumThinkTime().get());

    loadTest.applicationBatchSize().set(5);
    assertTrue(loadTest.scenarios().contains(SCENARIO));
    loadTest.user().set(UNIT_TEST_USER);
    assertEquals(UNIT_TEST_USER, loadTest.user().get());
    assertNotNull(loadTest.scenarioChooser());
    loadTest.setWeight(SCENARIO.name(), 2);
    loadTest.setScenarioEnabled(SCENARIO_II.name(), false);
    loadTest.addApplicationBatch();
    Thread.sleep(500);
    loadTest.paused().set(true);
    Thread.sleep(200);
    loadTest.paused().set(false);
    assertEquals(5, loadTest.applicationCount().get());
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

    model.applicationTableModel().refresh();
    model.applicationTableModel().selectionModel().setSelectedIndex(0);
    model.removeSelectedApplications();
    assertEquals(4, loadTest.applicationCount().get());

    model.clearCharts();
    loadTest.removeApplicationBatch();
    assertEquals(0, loadTest.applicationCount().get());

    AtomicInteger exitCounter = new AtomicInteger();
    loadTest.addShutdownListener(exitCounter::incrementAndGet);
    loadTest.shutdown();
    assertEquals(1, exitCounter.get());
  }

  @Test
  void setUpdateIntervalNegative() {
    LoadTest<Object> loadTest = LoadTest.builder(user -> new Object(), object -> {})
            .user(User.user("test"))
            .build();
    LoadTestModel<Object> model = LoadTestModel.loadTestModel(loadTest);
    assertThrows(IllegalArgumentException.class, () -> model.setUpdateInterval(-1));
  }
}
