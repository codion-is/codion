/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.loadtest;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.common.user.Users;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class LoadTestModelTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

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
  public void constructorNegativeThinkTime() {
    assertThrows(IllegalArgumentException.class, () -> new TestLoadTestModel(UNIT_TEST_USER, -100, 2, 5));
  }

  @Test
  public void constructorNegativeLoginDelayFactor() {
    assertThrows(IllegalArgumentException.class, () -> new TestLoadTestModel(UNIT_TEST_USER, 100, -2, 5));
  }

  @Test
  public void constructorNegativeApplicationBatchSize() {
    assertThrows(IllegalArgumentException.class, () -> new TestLoadTestModel(UNIT_TEST_USER, 100, 2, -5));
  }

  @Test
  public void setApplicationBatchSizeNegative() {
    final TestLoadTestModel model = new TestLoadTestModel(Users.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.setApplicationBatchSize(-5));
  }

  @Test
  public void setUpdateIntervalNegative() {
    final TestLoadTestModel model = new TestLoadTestModel(Users.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.setUpdateInterval(-1));
  }

  @Test
  public void setLoginDelayFactorNegative() {
    final TestLoadTestModel model = new TestLoadTestModel(Users.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.setLoginDelayFactor(-1));
  }

  @Test
  public void setMinimumThinkTimeNegative() {
    final TestLoadTestModel model = new TestLoadTestModel(Users.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.setMinimumThinkTime(-1));
  }

  @Test
  public void setMaximumThinkTimeNegative() {
    final TestLoadTestModel model = new TestLoadTestModel(Users.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.setMaximumThinkTime(-1));
  }

  @Test
  public void getUnknownUsageScenario() {
    final TestLoadTestModel model = new TestLoadTestModel(Users.user("test", "hello".toCharArray()), 50, 2, 2);
    assertThrows(IllegalArgumentException.class, () -> model.getUsageScenario("bla"));
  }

  @Test
  public void test() throws Exception {
    final TestLoadTestModel model = new TestLoadTestModel(UNIT_TEST_USER, 50, 2, 2);
    assertEquals(2, model.getApplicationBatchSize());
    model.setCollectChartData(true);

    assertNotNull(model.applicationBatchSizeObserver());
    assertNotNull(model.applicationCountObserver());
    assertNotNull(model.collectChartDataObserver());
    assertNotNull(model.maximumThinkTimeObserver());
    assertNotNull(model.getMinimumThinkTimeObserver());
    assertNotNull(model.getPauseObserver());

    assertNotNull(model.getMemoryUsageDataset());
    assertNotNull(model.getNumberOfApplicationsDataset());
    assertNotNull(model.getThinkTimeDataset());
    assertNotNull(model.getUsageScenarioDataset());

    assertEquals(2, model.getLoginDelayFactor());
    model.setLoginDelayFactor(3);
    assertEquals(3, model.getLoginDelayFactor());
    assertEquals(LoadTestModel.DEFAULT_CHART_DATA_UPDATE_INTERVAL_MS, model.getUpdateInterval());
    assertEquals(2, model.getApplicationBatchSize());

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
