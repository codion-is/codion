/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final UsageScenario<Object> SCENARIO = new AbstractUsageScenario<Object>("test") {
    int counter = 0;
    @Override
    protected void perform(final Object application) throws ScenarioException {
      if (counter++ % 2 == 0) {
        throw new ScenarioException();
      }
    }
  };

  private static final UsageScenario<Object> SCENARIO_II = new AbstractUsageScenario<Object>("testII") {
    @Override
    protected void perform(final Object application) throws ScenarioException {}
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
    final TestLoadTestModel model = new TestLoadTestModel(User.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.getApplicationBatchSizeValue().set(-5));
  }

  @Test
  void setUpdateIntervalNegative() {
    final TestLoadTestModel model = new TestLoadTestModel(User.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.setUpdateInterval(-1));
  }

  @Test
  void setLoginDelayFactorNegative() {
    final TestLoadTestModel model = new TestLoadTestModel(User.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.getLoginDelayFactorValue().set(-1));
  }

  @Test
  void setMinimumThinkTimeNegative() {
    final TestLoadTestModel model = new TestLoadTestModel(User.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.getMinimumThinkTimeValue().set(-1));
  }

  @Test
  void setMaximumThinkTimeNegative() {
    final TestLoadTestModel model = new TestLoadTestModel(User.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.getMaximumThinkTimeValue().set(-1));
  }

  @Test
  void getUnknownUsageScenario() {
    final TestLoadTestModel model = new TestLoadTestModel(User.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.getUsageScenario("bla"));
  }

  @Test
  void test() throws Exception {
    final TestLoadTestModel model = new TestLoadTestModel(UNIT_TEST_USER, 50, 2, 2);
    assertEquals(2, model.getApplicationBatchSizeValue().get());
    model.getCollectChartDataState().set(true);

    assertNotNull(model.getMemoryUsageDataset());
    assertNotNull(model.getNumberOfApplicationsDataset());
    assertNotNull(model.getThinkTimeDataset());
    assertNotNull(model.getUsageScenarioDataset());

    assertEquals(2, model.getLoginDelayFactorValue().get());
    model.getLoginDelayFactorValue().set(3);
    assertEquals(3, model.getLoginDelayFactorValue().get());
    assertEquals(LoadTestModel.DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS, model.getUpdateInterval());
    assertEquals(2, model.getApplicationBatchSizeValue().get());

    assertEquals(25, model.getMinimumThinkTimeValue().get());
    assertEquals(50, model.getMaximumThinkTimeValue().get());
    model.getMaximumThinkTimeValue().set(40);
    model.getMinimumThinkTimeValue().set(20);
    assertEquals(20, model.getMinimumThinkTimeValue().get());
    assertEquals(40, model.getMaximumThinkTimeValue().get());

    model.getApplicationBatchSizeValue().set(5);
    assertTrue(model.getUsageScenarios().contains(SCENARIO.getName()));
    model.setUser(UNIT_TEST_USER);
    assertEquals(UNIT_TEST_USER, model.getUser());
    assertNotNull(model.getScenarioChooser());
    model.setWeight(SCENARIO.getName(), 2);
    model.setScenarioEnabled(SCENARIO_II.getName(), false);
    model.addApplicationBatch();
    Thread.sleep(500);
    model.getPausedState().set(true);
    Thread.sleep(200);
    model.getPausedState().set(false);
    assertEquals(5, model.getApplicationCount());
    assertEquals(0, SCENARIO_II.getTotalRunCount());
    assertTrue(SCENARIO.getSuccessfulRunCount() > 0);
    assertTrue(SCENARIO.getUnsuccessfulRunCount() > 0);
    assertTrue(SCENARIO.getExceptions().size() > 0);
    SCENARIO.clearExceptions();
    assertEquals(0, SCENARIO.getExceptions().size());
    assertEquals(SCENARIO.getSuccessfulRunCount() + SCENARIO.getUnsuccessfulRunCount(), SCENARIO.getTotalRunCount());
    SCENARIO.resetRunCount();
    assertEquals(0, SCENARIO.getSuccessfulRunCount());
    assertEquals(0, SCENARIO.getUnsuccessfulRunCount());
    model.resetChartData();
    model.removeApplicationBatch();
    assertEquals(0, model.getApplicationCount());

    final AtomicInteger exitCounter = new AtomicInteger();
    model.addShutdownListener(exitCounter::incrementAndGet);
    model.shutdown();
    assertEquals(1, exitCounter.get());
  }

  public static final class TestLoadTestModel extends LoadTestModel<Object> {

    public TestLoadTestModel(final User user, final int maximumThinkTime, final int loginDelayFactor,
                             final int applicationBatchSize) {
      super(user, asList(SCENARIO, SCENARIO_II), maximumThinkTime, loginDelayFactor, applicationBatchSize);
    }

    @Override
    protected Object initializeApplication() throws CancelException {
      return new Object();
    }

    @Override
    protected void disconnectApplication(final Object application) {}
  }
}
