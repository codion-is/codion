/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.tools;

import org.jminor.common.User;
import org.jminor.common.model.CancelException;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class LoadTestModelTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  private static final LoadTestModel.UsageScenario SCENARIO = new LoadTestModel.AbstractUsageScenario("test") {
    int counter = 0;
    @Override
    protected void performScenario(final Object application) throws LoadTest.ScenarioException {
      if (counter++ % 2 == 0) {
        throw new LoadTest.ScenarioException();
      }
    }
  };

  private static final LoadTestModel.UsageScenario SCENARIO_II = new LoadTestModel.AbstractUsageScenario("testII") {
    @Override
    protected void performScenario(final Object application) throws LoadTest.ScenarioException {}
  };

  @Test(expected = IllegalArgumentException.class)
  public void constructorNegativeThinkTime() {
    new TestLoadTestModel(UNIT_TEST_USER, -100, 2, 5, 1000);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNegativeLoginDelayFactor() {
    new TestLoadTestModel(UNIT_TEST_USER, 100, -2, 5, 1000);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNegativeApplicationBatchSize() {
    new TestLoadTestModel(UNIT_TEST_USER, 100, 2, -5, 1000);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNegativeWarningTime() {
    new TestLoadTestModel(UNIT_TEST_USER, 100, 2, 5, -1000);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setApplicationBatchSizeNegative() {
    final LoadTestModel model = new TestLoadTestModel(new User("test", "hello"), 50, 2, 2, 1000);
    model.setApplicationBatchSize(-5);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setUpdateIntervalNegative() {
    final LoadTestModel model = new TestLoadTestModel(new User("test", "hello"), 50, 2, 2, 1000);
    model.setUpdateInterval(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setLoginDelayFactorNegative() {
    final LoadTestModel model = new TestLoadTestModel(new User("test", "hello"), 50, 2, 2, 1000);
    model.setLoginDelayFactor(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setWarningTimeNegative() {
    final LoadTestModel model = new TestLoadTestModel(new User("test", "hello"), 50, 2, 2, 1000);
    model.setWarningTime(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setMinimumThinkTimeNegative() {
    final LoadTestModel model = new TestLoadTestModel(new User("test", "hello"), 50, 2, 2, 1000);
    model.setMinimumThinkTime(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setMaximumThinkTimeNegative() {
    final LoadTestModel model = new TestLoadTestModel(new User("test", "hello"), 50, 2, 2, 1000);
    model.setMaximumThinkTime(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getUnknownUsageScenario() {
    final LoadTestModel model = new TestLoadTestModel(new User("test", "hello"), 50, 2, 2, 1000);
    model.getUsageScenario("bla");
  }

  @Test
  public void test() throws Exception {
    final LoadTestModel model = new TestLoadTestModel(UNIT_TEST_USER, 50, 2, 2, 1000);
    assertEquals(2, model.getApplicationBatchSize());
    model.setCollectChartData(true);

    assertNotNull(model.applicationBatchSizeObserver());
    assertNotNull(model.applicationCountObserver());
    assertNotNull(model.collectChartDataObserver());
    assertNotNull(model.maximumThinkTimeObserver());
    assertNotNull(model.getMinimumThinkTimeObserver());
    assertNotNull(model.getPauseObserver());
    assertNotNull(model.getWarningTimeObserver());

    assertNotNull(model.getMemoryUsageDataset());
    assertNotNull(model.getNumberOfApplicationsDataset());
    assertNotNull(model.getThinkTimeDataset());
    assertNotNull(model.getUsageScenarioDataset());

    assertEquals(2, model.getLoginDelayFactor());
    model.setLoginDelayFactor(3);
    assertEquals(3, model.getLoginDelayFactor());
    assertEquals(LoadTestModel.DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS, model.getUpdateInterval());
    assertEquals(1000, model.getWarningTime());
    assertEquals(2, model.getApplicationBatchSize());
    model.setWarningTime(80);
    assertEquals(80, model.getWarningTime());

    assertEquals(25, model.getMinimumThinkTime());
    assertEquals(50, model.getMaximumThinkTime());
    model.setMaximumThinkTime(40);
    model.setMinimumThinkTime(20);
    assertEquals(20, model.getMinimumThinkTime());
    assertEquals(40, model.getMaximumThinkTime());

    model.setApplicationBatchSize(5);
    assertTrue(model.getUsageScenarios().contains(SCENARIO.getName()));
    model.setUser(UNIT_TEST_USER);
    assertEquals(UNIT_TEST_USER, model.getUser());
    assertNotNull(model.getScenarioChooser());
    model.setWeight(SCENARIO.getName(), 2);
    model.setScenarioEnabled(SCENARIO_II.getName(), false);
    model.addApplicationBatch();
    Thread.sleep(500);
    model.setPaused(true);
    assertTrue(model.isPaused());
    model.setPaused(false);
    assertFalse(model.isPaused());
    assertEquals(5, model.getApplicationCount());
    assertEquals(0, SCENARIO_II.getTotalRunCount());
    assertTrue(SCENARIO.getSuccessfulRunCount() > 0);
    assertTrue(SCENARIO.getUnsuccessfulRunCount() > 0);
    assertTrue(SCENARIO.getExceptions().size() > 0);
    SCENARIO.clearExceptions();
    assertTrue(SCENARIO.getExceptions().size() == 0);
    assertEquals(SCENARIO.getSuccessfulRunCount() + SCENARIO.getUnsuccessfulRunCount(), SCENARIO.getTotalRunCount());
    SCENARIO.resetRunCount();
    assertTrue(SCENARIO.getSuccessfulRunCount() == 0);
    assertTrue(SCENARIO.getUnsuccessfulRunCount() == 0);
    model.resetChartData();
    model.removeApplicationBatch();
    assertEquals(0, model.getApplicationCount());

    final AtomicInteger exitCounter = new AtomicInteger(0);
    model.addExitListener(exitCounter::incrementAndGet);
    model.exit();
    assertEquals(1, exitCounter.get());
  }

  public static final class TestLoadTestModel extends LoadTestModel {

    public TestLoadTestModel(final User user, final int maximumThinkTime, final int loginDelayFactor, final int applicationBatchSize,
                      final int warningTime) {
      super(user, Arrays.asList(SCENARIO, SCENARIO_II), maximumThinkTime, loginDelayFactor, applicationBatchSize, warningTime);
    }

    @Override
    protected Object initializeApplication() throws CancelException {
      return new Object();
    }

    @Override
    protected void disconnectApplication(final Object application) {}
  }
}
