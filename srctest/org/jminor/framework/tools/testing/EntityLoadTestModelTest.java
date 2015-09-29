/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.testing;

import org.jminor.common.model.User;
import org.jminor.common.model.tools.ScenarioException;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.server.EntityConnectionServerTest;
import org.jminor.swing.framework.model.DefaultEntityApplicationModel;
import org.jminor.swing.framework.model.DefaultEntityModel;
import org.jminor.swing.framework.model.EntityApplicationModel;
import org.jminor.swing.framework.model.EntityTableModel;
import org.jminor.swing.framework.testing.EntityLoadTestModel;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class EntityLoadTestModelTest {

  private static final String CONNECTION_TYPE_BEFORE_TEST = Configuration.getStringValue(Configuration.CLIENT_CONNECTION_TYPE);

  @BeforeClass
  public static void setUp() throws Exception {
    EntityConnectionServerTest.setUp();
    Configuration.setValue(Configuration.CLIENT_CONNECTION_TYPE, "remote");
  }

  @AfterClass
  public static void tearDown() throws Exception {
    EntityConnectionServerTest.tearDown();
    Configuration.setValue(Configuration.CLIENT_CONNECTION_TYPE, CONNECTION_TYPE_BEFORE_TEST);
  }

  private static final class TestLoadTestModel extends EntityLoadTestModel {

    public TestLoadTestModel() {
      super(User.UNIT_TEST_USER, Arrays.asList(new EntityLoadTestModel.AbstractEntityUsageScenario("1") {
        @Override
        protected void performScenario(final EntityApplicationModel application) throws ScenarioException {}
      }, new EntityLoadTestModel.AbstractEntityUsageScenario("2") {
        @Override
        protected void performScenario(final EntityApplicationModel application) throws ScenarioException {}
      }));
    }

    @Override
    protected EntityApplicationModel initializeApplication() {
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

  @Test
  public void testMethods() {
    final EntityApplicationModel model = new DefaultEntityApplicationModel(EntityConnectionProvidersTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        TestDomain.init();
      }
    };
    model.addEntityModel(new DefaultEntityModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER));
    final EntityTableModel tableModel = model.getEntityModel(TestDomain.T_DEPARTMENT).getTableModel();
    tableModel.setQueryCriteriaRequired(false);
    tableModel.refresh();

    EntityLoadTestModel.selectRandomRow(tableModel);
    assertFalse(tableModel.getSelectionModel().getSelectionEmptyObserver().isActive());

    EntityLoadTestModel.selectRandomRows(tableModel, 3);
    assertEquals(3, tableModel.getSelectionModel().getSelectedItems().size());

    EntityLoadTestModel.selectRandomRows(tableModel, 0.5);
    assertEquals(2, tableModel.getSelectionModel().getSelectedItems().size());

    model.getConnectionProvider().disconnect();
  }
}
