/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.testing;

import org.jminor.common.User;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.model.DefaultEntityApplicationModel;
import org.jminor.framework.model.EntityLoadTestModel;
import org.jminor.framework.server.DefaultEntityConnectionServerTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class EntityLoadTestModelTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  private static final String CONNECTION_TYPE_BEFORE_TEST = Configuration.getStringValue(Configuration.CLIENT_CONNECTION_TYPE);

  @BeforeClass
  public static void setUp() throws Exception {
    DefaultEntityConnectionServerTest.setUp();
    Configuration.setValue(Configuration.CLIENT_CONNECTION_TYPE, "remote");
  }

  @AfterClass
  public static void tearDown() throws Exception {
    DefaultEntityConnectionServerTest.tearDown();
    Configuration.setValue(Configuration.CLIENT_CONNECTION_TYPE, CONNECTION_TYPE_BEFORE_TEST);
  }

  private static final class TestLoadTestModel extends EntityLoadTestModel<DefaultEntityApplicationModel> {

    public TestLoadTestModel() {
      super(UNIT_TEST_USER, Arrays.asList(new EntityLoadTestModel.AbstractEntityUsageScenario<DefaultEntityApplicationModel>("1") {
        @Override
        protected void performScenario(final DefaultEntityApplicationModel application) throws ScenarioException {}
      }, new EntityLoadTestModel.AbstractEntityUsageScenario<DefaultEntityApplicationModel>("2") {
        @Override
        protected void performScenario(final DefaultEntityApplicationModel application) throws ScenarioException {}
      }));
    }

    @Override
    protected DefaultEntityApplicationModel initializeApplication() {
      return new DefaultEntityApplicationModel(
              EntityConnectionProviders.connectionProvider(getUser(), EntityLoadTestModelTest.class.getSimpleName())) {
        @Override
        protected void loadDomainModel() {
          TestDomain.init();
        }
      };
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void setLoginDelayFactorNegative() {
    new TestLoadTestModel().setLoginDelayFactor(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setUpdateIntervalNegative() {
    new TestLoadTestModel().setUpdateInterval(-1);
  }

  @Test
  public void testLoadTesting() throws Exception {
    final TestLoadTestModel loadTest = new TestLoadTestModel();

    loadTest.setCollectChartData(true);
    loadTest.setUpdateInterval(350);
    loadTest.setLoginDelayFactor(0);

    assertTrue(loadTest.isCollectChartData());
    assertEquals(350, loadTest.getUpdateInterval());
    assertEquals(0, loadTest.getLoginDelayFactor());

    loadTest.setWeight("1", 1);
    loadTest.setWeight("2", 0);

    loadTest.setMaximumThinkTime(100);
    loadTest.setMinimumThinkTime(50);
    loadTest.setWarningTime(10);

    loadTest.setApplicationBatchSize(2);
    assertEquals(2, loadTest.getApplicationBatchSize());

    loadTest.addApplicationBatch();

    Thread.sleep(1500);

    assertEquals("Two clients expected, if this fails try increasing the Thread.sleep() value above",
            2, loadTest.getApplicationCount());
    assertTrue(loadTest.getUsageScenario("1").getTotalRunCount() > 0);
    assertTrue(loadTest.getUsageScenario("1").getSuccessfulRunCount() > 0);
    assertTrue(loadTest.getUsageScenario("1").getUnsuccessfulRunCount() == 0);
    assertTrue(loadTest.getUsageScenario("2").getTotalRunCount() == 0);

    loadTest.setPaused(true);
    assertTrue(loadTest.isPaused());

    loadTest.resetChartData();

    loadTest.setApplicationBatchSize(1);
    loadTest.removeApplicationBatch();
    assertEquals(1, loadTest.getApplicationCount());
    loadTest.exit();

    Thread.sleep(500);

    assertFalse(loadTest.isPaused());
    assertEquals(0, loadTest.getApplicationCount());
  }
}
