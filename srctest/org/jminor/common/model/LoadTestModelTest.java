package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

public class LoadTestModelTest {

  private static final User USER = new User("hello", "world");
  private static final LoadTestModel.UsageScenario SCENARIO = new LoadTestModel.UsageScenario("test") {
    @Override
    protected void performScenario(final Object application) throws Exception {}
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
    model.setApplicationBatchSize(5);
    assertTrue(model.getUsageScenarios().contains(SCENARIO));
    model.setUser(USER);
    assertEquals(USER, model.getUser());
    assertNotNull(model.getScenarioChooser());
    model.addApplications();
    Thread.sleep(500);
    assertEquals(5, model.getApplicationCount());
    System.out.println(SCENARIO.getSuccessfulRunCount());
    assertTrue(SCENARIO.getSuccessfulRunCount() > 0);
    model.exit();
    Thread.sleep(200);
    assertEquals(0, model.getApplicationCount());
  }

  private static class TestLoadTestModel extends LoadTestModel {

    TestLoadTestModel(final User user, final int maximumThinkTime, final int loginDelayFactor, final int applicationBatchSize,
                      final int warningTime) {
      super(user, maximumThinkTime, loginDelayFactor, applicationBatchSize, warningTime);
    }
    @Override
    protected Object initializeApplication() throws CancelException {
      return new Object();
    }

    @Override
    protected void disconnectApplication(final Object application) {}

    @Override
    protected Collection<UsageScenario> initializeUsageScenarios() {
      final Collection<UsageScenario> ret = new ArrayList<UsageScenario>();
      ret.add(SCENARIO);
      return ret;
    }
  }
}
