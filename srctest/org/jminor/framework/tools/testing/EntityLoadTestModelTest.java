/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.testing;

import org.jminor.framework.client.model.DefaultEntityApplicationModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.db.EntityDbConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.demos.empdept.testing.EmpDeptLoadTest;
import org.jminor.framework.server.EntityDbRemoteServerTest;

import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class EntityLoadTestModelTest {

  @BeforeClass
  public static void setUp() throws Exception {
    EntityDbRemoteServerTest.setUp();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    EntityDbRemoteServerTest.tearDown();
  }

  @Test
  public void testLoadTesting() throws Exception {
    final EmpDeptLoadTest loadTest = new EmpDeptLoadTest();

    loadTest.setCollectChartData(true);
    loadTest.setUpdateInterval(350);
    loadTest.setLoginDelayFactor(0);

    try {
      loadTest.setLoginDelayFactor(-1);
      fail();
    }
    catch (Exception e) {}
    try {
      loadTest.setUpdateInterval(-1);
      fail();
    }
    catch (Exception e) {}

    assertTrue(loadTest.isCollectChartData());
    assertEquals(350, loadTest.getUpdateInterval());
    assertEquals(0, loadTest.getLoginDelayFactor());

    loadTest.getScenarioChooser().setWeight(loadTest.getUsageScenario("SelectDepartment"), 1);
    loadTest.getScenarioChooser().setWeight(loadTest.getUsageScenario("InsertDepartment"), 0);
    loadTest.getScenarioChooser().setWeight(loadTest.getUsageScenario("InsertEmployee"), 0);
    loadTest.getScenarioChooser().setWeight(loadTest.getUsageScenario("LoginLogout"), 0);
    loadTest.getScenarioChooser().setWeight(loadTest.getUsageScenario("UpdateEmployee"), 0);

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
    final EntityApplicationModel model = new DefaultEntityApplicationModel(EntityDbConnectionTest.DB_PROVIDER) {
      @Override
      protected void loadDomainModel() {
        new EmpDept();
      }
    };
    final EntityTableModel tableModel = model.getMainApplicationModel(EmpDept.T_DEPARTMENT).getTableModel();
    tableModel.setQueryCriteriaRequired(false);
    tableModel.refresh();

    EntityLoadTestModel.selectRandomRow(tableModel);
    assertFalse(tableModel.stateSelectionEmpty().isActive());

    EntityLoadTestModel.selectRandomRows(tableModel, 3);
    assertEquals(3, tableModel.getSelectedItems().size());

    EntityLoadTestModel.selectRandomRows(tableModel, 0.5);
    assertEquals(2, tableModel.getSelectedItems().size());

    model.getDbProvider().disconnect();
  }
}
