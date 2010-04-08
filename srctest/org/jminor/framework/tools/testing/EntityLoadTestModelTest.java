package org.jminor.framework.tools.testing;

import org.jminor.common.db.User;
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

    loadTest.setLoginDelayFactor(0);

    loadTest.getRandomModel().setWeight(loadTest.getUsageScenario("selectDepartment"), 1);
    loadTest.getRandomModel().setWeight(loadTest.getUsageScenario("insertDepartment"), 0);
    loadTest.getRandomModel().setWeight(loadTest.getUsageScenario("insertEmployee"), 0);
    loadTest.getRandomModel().setWeight(loadTest.getUsageScenario("logoutLogin"), 0);
    loadTest.getRandomModel().setWeight(loadTest.getUsageScenario("updateEmployee"), 0);

    loadTest.setMaximumThinkTime(100);
    loadTest.setMinimumThinkTime(50);

    loadTest.setClientBatchSize(1);
    assertEquals(1, loadTest.getClientBatchSize());

    loadTest.addClients();

    Thread.sleep(2000);

    assertEquals("One client expected, if this fails try increasing the Thread.sleep() value above",
            1, loadTest.getClientCount());
    assertTrue(loadTest.getUsageScenario("selectDepartment").getTotalRunCount() > 0);
    assertTrue(loadTest.getUsageScenario("selectDepartment").getSuccessfulRunCount() > 0);
    assertTrue(loadTest.getUsageScenario("selectDepartment").getUnsuccessfulRunCount() == 0);
    assertTrue(loadTest.getUsageScenario("insertDepartment").getTotalRunCount() == 0);

    loadTest.setPaused(true);
    assertTrue(loadTest.isPaused());

    loadTest.exit();

    Thread.sleep(500);

    assertFalse(loadTest.isPaused());
    assertEquals(0, loadTest.getClientCount());
  }

  @Test
  public void testMethods() {
    final EntityApplicationModel model = new EmpDeptAppModel(new EntityDbRemoteProvider(new User("scott", "tiger"), "clientID", "EntityLoadTestModelTest"));
    model.refreshAll();
    final EntityTableModel tableModel = model.getMainApplicationModel(DepartmentModel.class).getTableModel();

    EntityLoadTestModel.selectRandomRow(tableModel);
    assertFalse(tableModel.stateSelectionEmpty().isActive());

    EntityLoadTestModel.selectRandomRows(tableModel, 3);
    assertEquals(3, tableModel.getSelectedEntities().size());

    EntityLoadTestModel.selectRandomRows(tableModel, 0.5);
    assertEquals(2, tableModel.getSelectedEntities().size());

    model.getDbProvider().disconnect();
  }
}
