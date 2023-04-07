/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.loadtest;

import is.codion.common.model.CancelException;
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
    protected void perform(Object application) throws Exception {}
  };

  @Test
  void constructorNegativeThinkTime() {
    assertThrows(IllegalArgumentException.class, () -> new TestLoadTestModel(UNIT_TEST_USER, -100, 2, 5));
  }

  @Test
  void constructorNegativeLoginDelayFactor() {
    assertThrows(IllegalArgumentException.class, () -> new TestLoadTestModel(UNIT_TEST_USER, 100, -2, 5));
  }

  @Test
  void constructorNegativeApplicationBatchSize() {
    assertThrows(IllegalArgumentException.class, () -> new TestLoadTestModel(UNIT_TEST_USER, 100, 2, -5));
  }

  @Test
  void setApplicationBatchSizeNegative() {
    TestLoadTestModel model = new TestLoadTestModel(User.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.applicationBatchSizeValue().set(-5));
  }

  @Test
  void setUpdateIntervalNegative() {
    TestLoadTestModel model = new TestLoadTestModel(User.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.setUpdateInterval(-1));
  }

  @Test
  void setLoginDelayFactorNegative() {
    TestLoadTestModel model = new TestLoadTestModel(User.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.loginDelayFactorValue().set(-1));
  }

  @Test
  void setMinimumThinkTimeNegative() {
    TestLoadTestModel model = new TestLoadTestModel(User.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.minimumThinkTimeValue().set(-1));
  }

  @Test
  void setMaximumThinkTimeNegative() {
    TestLoadTestModel model = new TestLoadTestModel(User.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.maximumThinkTimeValue().set(-1));
  }

  @Test
  void unknownUsageScenario() {
    TestLoadTestModel model = new TestLoadTestModel(User.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.usageScenario("bla"));
  }

  @Test
  void test() throws Exception {
    TestLoadTestModel model = new TestLoadTestModel(UNIT_TEST_USER, 50, 2, 2);
    assertEquals(2, model.applicationBatchSizeValue().get());
    model.collectChartDataState().set(true);

    assertNotNull(model.memoryUsageDataset());
    assertNotNull(model.numberOfApplicationsDataset());
    assertNotNull(model.thinkTimeDataset());
    assertNotNull(model.usageScenarioDataset());

    assertEquals(2, model.loginDelayFactorValue().get());
    model.loginDelayFactorValue().set(3);
    assertEquals(3, model.loginDelayFactorValue().get());
    assertEquals(LoadTestModel.DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS, model.getUpdateInterval());
    assertEquals(2, model.applicationBatchSizeValue().get());

    assertEquals(25, model.minimumThinkTimeValue().get());
    assertEquals(50, model.maximumThinkTimeValue().get());
    model.maximumThinkTimeValue().set(40);
    model.minimumThinkTimeValue().set(20);
    assertEquals(20, model.minimumThinkTimeValue().get());
    assertEquals(40, model.maximumThinkTimeValue().get());

    model.applicationBatchSizeValue().set(5);
    assertTrue(model.usageScenarios().contains(SCENARIO.name()));
    model.setUser(UNIT_TEST_USER);
    assertEquals(UNIT_TEST_USER, model.getUser());
    assertNotNull(model.scenarioChooser());
    model.setWeight(SCENARIO.name(), 2);
    model.setScenarioEnabled(SCENARIO_II.name(), false);
    model.addApplicationBatch();
    Thread.sleep(500);
    model.pausedState().set(true);
    Thread.sleep(200);
    model.pausedState().set(false);
    assertEquals(5, model.applicationCount());
    assertEquals(0, SCENARIO_II.totalRunCount());
    assertTrue(SCENARIO.successfulRunCount() > 0);
    assertTrue(SCENARIO.unsuccessfulRunCount() > 0);
    assertTrue(SCENARIO.exceptions().size() > 0);
    SCENARIO.clearExceptions();
    assertEquals(0, SCENARIO.exceptions().size());
    assertEquals(SCENARIO.successfulRunCount() + SCENARIO.unsuccessfulRunCount(), SCENARIO.totalRunCount());
    SCENARIO.resetRunCount();
    assertEquals(0, SCENARIO.successfulRunCount());
    assertEquals(0, SCENARIO.unsuccessfulRunCount());
    model.clearChartData();
    model.removeApplicationBatch();
    assertEquals(0, model.applicationCount());

    AtomicInteger exitCounter = new AtomicInteger();
    model.addShutdownListener(exitCounter::incrementAndGet);
    model.shutdown();
    assertEquals(1, exitCounter.get());
  }

  public static final class TestLoadTestModel extends LoadTestModel<Object> {

    public TestLoadTestModel(User user, int maximumThinkTime, int loginDelayFactor,
                             int applicationBatchSize) {
      super(user, asList(SCENARIO, SCENARIO_II), maximumThinkTime, loginDelayFactor, applicationBatchSize);
    }

    @Override
    protected Object createApplication() throws CancelException {
      return new Object();
    }

    @Override
    protected void disconnectApplication(Object application) {}
  }
}
