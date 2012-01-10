/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.tools;

import org.jminor.common.db.Databases;
import org.jminor.common.model.User;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public final class QueryLoadTestModelTest {

  private static final QueryLoadTestModel.QueryScenario SELECT_EMPLOYEE =
          new QueryLoadTestModel.QueryScenario("selectEmployees", "select * from scott.emp");
  private static final QueryLoadTestModel.QueryScenario SELECT_DEPARTMENTS =
          new QueryLoadTestModel.QueryScenario("selectDepartments", "select * from scott.dept");
  private static final QueryLoadTestModel.QueryScenario SELECT_CUSTOMERS =
          new QueryLoadTestModel.QueryScenario("selectCustomers", "select * from chinook.customer");
  private static final QueryLoadTestModel.QueryScenario SELECT_ALBUMS =
          new QueryLoadTestModel.QueryScenario("selectAlbum", "select * from chinook.album");
  private static final QueryLoadTestModel.QueryScenario SELECT_PRODUCTS =
          new QueryLoadTestModel.QueryScenario("selectProducts", "select * from petstore.product");
  @Test
  public void test() {
    final QueryLoadTestModel loadTest = new QueryLoadTestModel(Databases.createInstance(), User.UNIT_TEST_USER,
            Arrays.asList(SELECT_ALBUMS, SELECT_CUSTOMERS, SELECT_DEPARTMENTS,
                    SELECT_EMPLOYEE, SELECT_PRODUCTS));
    loadTest.setMinimumThinkTime(10);
    loadTest.setMaximumThinkTime(30);
    loadTest.setLoginDelayFactor(0);
    loadTest.setApplicationBatchSize(6);
    loadTest.addApplicationBatch();
    try {
      Thread.sleep(1500);
    }
    catch (InterruptedException ignored) {}
    loadTest.removeApplicationBatch();
    try {
      Thread.sleep(500);
    }
    catch (InterruptedException ignored) {}
    assertTrue(SELECT_ALBUMS.getSuccessfulRunCount() > 0);
    assertTrue(SELECT_CUSTOMERS.getSuccessfulRunCount() > 0);
    assertTrue(SELECT_DEPARTMENTS.getSuccessfulRunCount() > 0);
    assertTrue(SELECT_EMPLOYEE.getSuccessfulRunCount() > 0);
    assertTrue(SELECT_PRODUCTS.getSuccessfulRunCount() > 0);
    loadTest.exit();
  }
}
