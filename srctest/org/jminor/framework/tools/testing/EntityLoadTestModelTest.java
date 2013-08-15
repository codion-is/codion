/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.testing;

import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.DefaultEntityApplicationModel;
import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.db.DefaultEntityConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.demos.empdept.testing.EmpDeptLoadTest;
import org.jminor.framework.server.EntityConnectionServerTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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

  @Test(expected = IllegalArgumentException.class)
  public void setLoginDelayFactorNegative() {
    final EmpDeptLoadTest loadTest = new EmpDeptLoadTest();
    loadTest.setLoginDelayFactor(-1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setUpdateIntervalNegative() {
    final EmpDeptLoadTest loadTest = new EmpDeptLoadTest();
    loadTest.setUpdateInterval(-1);
  }

  @Test
  public void testLoadTesting() throws Exception {
    final EmpDeptLoadTest loadTest = new EmpDeptLoadTest();

    loadTest.setCollectChartData(true);
    loadTest.setUpdateInterval(350);
    loadTest.setLoginDelayFactor(0);

    assertTrue(loadTest.isCollectChartData());
    assertEquals(350, loadTest.getUpdateInterval());
    assertEquals(0, loadTest.getLoginDelayFactor());

    loadTest.setWeight("SelectDepartment", 1);
    loadTest.setWeight("InsertDepartment", 0);
    loadTest.setWeight("InsertEmployee", 0);
    loadTest.setWeight("LoginLogout", 0);
    loadTest.setWeight("UpdateEmployee", 0);

    loadTest.setMaximumThinkTime(100);
    loadTest.setMinimumThinkTime(50);
    loadTest.setWarningTime(10);

    loadTest.setApplicationBatchSize(2);
    assertEquals(2, loadTest.getApplicationBatchSize());

    loadTest.addApplicationBatch();

    Thread.sleep(5000);

    assertEquals("Two clients expected, if this fails try increasing the Thread.sleep() value above",
            2, loadTest.getApplicationCount());
    assertTrue(loadTest.getUsageScenario("SelectDepartment").getTotalRunCount() > 0);
    assertTrue(loadTest.getUsageScenario("SelectDepartment").getSuccessfulRunCount() > 0);
    assertTrue(loadTest.getUsageScenario("SelectDepartment").getUnsuccessfulRunCount() == 0);
    assertTrue(loadTest.getUsageScenario("InsertDepartment").getTotalRunCount() == 0);

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
    final EntityApplicationModel model = new DefaultEntityApplicationModel(DefaultEntityConnectionTest.CONNECTION_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        EmpDept.init();
      }
    };
    model.addEntityModel(new DefaultEntityModel(EmpDept.T_DEPARTMENT, DefaultEntityConnectionTest.CONNECTION_PROVIDER));
    final EntityTableModel tableModel = model.getEntityModel(EmpDept.T_DEPARTMENT).getTableModel();
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
