package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

public class LoadTestModelTest {

  private static final User USER = new User("hello", "world");
  private static final LoadTestModel.UsageScenario SCENARIO = new LoadTestModel.UsageScenario("test") {
    final Random random = new Random();
    @Override
    protected void performScenario(final Object application) throws Exception {
      if (random.nextDouble() > 0.5) {
        throw new Exception();
      }
    }
  };

  @Test
  public void test() throws Exception {
    try {
      new TestLoadTestModel(USER, -100, 2, 5, 1000);
    }
    catch (IllegalArgumentException e) {}
    try {
      new TestLoadTestModel(USER, 100, -2, 5, 1000);
    }
    catch (IllegalArgumentException e) {}
    try {
      new TestLoadTestModel(USER, 100, 2, -5, 1000);
    }
    catch (IllegalArgumentException e) {}
    try {
      new TestLoadTestModel(USER, 100, 2, 5, -1000);
    }
    catch (IllegalArgumentException e) {}

    final LoadTestModel model = new TestLoadTestModel(new User("test", "hello"), 50, 2, 2, 1000);
    assertEquals(2, model.getApplicationBatchSize());
    try {
      model.setApplicationBatchSize(-5);
    }
    catch (IllegalArgumentException e) {}

    model.setCollectChartData(true);

    assertNotNull(model.eventApplicationBatchSizeChanged());
    assertNotNull(model.eventApplicationCountChanged());
    assertNotNull(model.eventCollectChartDataChanged());
    assertNotNull(model.eventDoneExiting());
    assertNotNull(model.eventMaximumThinkTimeChanged());
    assertNotNull(model.eventMinimumThinkTimeChanged());
    assertNotNull(model.eventPausedChanged());
    assertNotNull(model.eventWarningTimeChanged());

    assertNotNull(model.getMemoryUsageDataset());
    assertNotNull(model.getNumberOfApplicationsDataset());
    assertNotNull(model.getThinkTimeDataset());
    assertNotNull(model.getUsageScenarioDataset());
    assertNotNull(model.getWorkRequestsDataset());


    assertEquals(2, model.getLoginDelayFactor());
    model.setLoginDelayFactor(3);
    assertEquals(3, model.getLoginDelayFactor());
    assertEquals(LoadTestModel.DEFAULT_UPDATE_INTERVAL, model.getUpdateInterval());
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
    try {
      model.setLoginDelayFactor(-1);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      model.setUpdateInterval(-1);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      model.setWarningTime(-1);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      model.setMinimumThinkTime(-1);
      fail();
    }
    catch (IllegalArgumentException e) {}
    try {
      model.setMaximumThinkTime(-1);
      fail();
    }
    catch (IllegalArgumentException e) {}

    try {
      model.getUsageScenario("bla");
      fail();
    }
    catch (RuntimeException e) {}

    model.setApplicationBatchSize(5);
    assertTrue(model.getUsageScenarios().contains(SCENARIO));
    model.setUser(USER);
    assertEquals(USER, model.getUser());
    assertNotNull(model.getScenarioChooser());
    model.addApplicationBatch();
    Thread.sleep(500);
    model.setPaused(true);
    assertTrue(model.isPaused());
    model.setPaused(false);
    assertFalse(model.isPaused());
    assertEquals(5, model.getApplicationCount());
    System.out.println(SCENARIO.toString() + ": " + SCENARIO.getSuccessfulRunCount());
    assertTrue(SCENARIO.getSuccessfulRunCount() > 0);
    assertTrue(SCENARIO.getUnsuccessfulRunCount() > 0);
    assertEquals(SCENARIO.getSuccessfulRunCount() + SCENARIO.getUnsuccessfulRunCount(), SCENARIO.getTotalRunCount());
    SCENARIO.resetRunCount();
    assertTrue(SCENARIO.getSuccessfulRunCount() == 0);
    assertTrue(SCENARIO.getUnsuccessfulRunCount() == 0);
    model.resetChartData();
    model.exit();
    Thread.sleep(200);
    assertEquals(0, model.getApplicationCount());
  }

  public static final class TestLoadTestModel extends LoadTestModel {

    public TestLoadTestModel(final User user, final int maximumThinkTime, final int loginDelayFactor, final int applicationBatchSize,
                      final int warningTime) {
      super(user, Arrays.asList(SCENARIO), maximumThinkTime, loginDelayFactor, applicationBatchSize, warningTime);
    }
    @Override
    protected Object initializeApplication() throws CancelException {
      return new Object();
    }

    @Override
    protected void disconnectApplication(final Object application) {}
  }
}
