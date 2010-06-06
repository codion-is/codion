package org.jminor.framework.tools.testing;

import org.jminor.common.model.User;
import org.jminor.common.ui.LoadTestPanel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.demos.empdept.beans.DepartmentModel;
import org.jminor.framework.demos.empdept.client.EmpDeptAppModel;
import org.jminor.framework.demos.empdept.testing.EmpDeptLoadTest;
import org.jminor.framework.server.EntityDbRemoteServerTest;
import org.jminor.framework.server.provider.EntityDbRemoteProvider;

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

    new LoadTestPanel(loadTest).showFrame();

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

    loadTest.getScenarioChooser().setWeight(loadTest.getUsageScenario("selectDepartment"), 1);
    loadTest.getScenarioChooser().setWeight(loadTest.getUsageScenario("insertDepartment"), 0);
    loadTest.getScenarioChooser().setWeight(loadTest.getUsageScenario("insertEmployee"), 0);
    loadTest.getScenarioChooser().setWeight(loadTest.getUsageScenario("logoutLogin"), 0);
    loadTest.getScenarioChooser().setWeight(loadTest.getUsageScenario("updateEmployee"), 0);

    loadTest.setMaximumThinkTime(100);
    loadTest.setMinimumThinkTime(50);
    loadTest.setWarningTime(10);

    loadTest.setApplicationBatchSize(2);
    assertEquals(2, loadTest.getApplicationBatchSize());

    loadTest.addApplications();

    Thread.sleep(3000);

    assertEquals("Two clients expected, if this fails try increasing the Thread.sleep() value above",
            2, loadTest.getApplicationCount());
    assertTrue(loadTest.getUsageScenario("selectDepartment").getTotalRunCount() > 0);
    assertTrue(loadTest.getUsageScenario("selectDepartment").getSuccessfulRunCount() > 0);
    assertTrue(loadTest.getUsageScenario("selectDepartment").getUnsuccessfulRunCount() == 0);
    assertTrue(loadTest.getUsageScenario("insertDepartment").getTotalRunCount() == 0);

    loadTest.setPaused(true);
    assertTrue(loadTest.isPaused());

    loadTest.resetChartData();

    loadTest.setApplicationBatchSize(1);
    loadTest.removeApplications();
    assertEquals(1, loadTest.getApplicationCount());
    loadTest.exit();

    Thread.sleep(500);

    assertFalse(loadTest.isPaused());
    assertEquals(0, loadTest.getApplicationCount());
  }

  @Test
  public void testMethods() {
    final EntityApplicationModel model = new EmpDeptAppModel(new EntityDbRemoteProvider(User.UNIT_TEST_USER, "clientID", "EntityLoadTestModelTest"));
    model.refresh();
    final EntityTableModel tableModel = model.getMainApplicationModel(DepartmentModel.class).getTableModel();

    EntityLoadTestModel.selectRandomRow(tableModel);
    assertFalse(tableModel.stateSelectionEmpty().isActive());

    EntityLoadTestModel.selectRandomRows(tableModel, 3);
    assertEquals(3, tableModel.getSelectedItems().size());

    EntityLoadTestModel.selectRandomRows(tableModel, 0.5);
    assertEquals(2, tableModel.getSelectedItems().size());

    model.getDbProvider().disconnect();
  }
}
