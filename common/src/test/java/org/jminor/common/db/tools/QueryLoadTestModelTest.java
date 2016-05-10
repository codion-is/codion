/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.tools;

import org.jminor.common.db.DatabasesTest;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.User;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public final class QueryLoadTestModelTest {

  private static final QueryLoadTestModel.QueryScenario SELECT_EMPLOYEE =
          new QueryLoadTestModel.QueryScenario("selectEmployees", "select * from scott.emp");
  private static final QueryLoadTestModel.QueryScenario SELECT_DEPARTMENTS =
          new QueryLoadTestModel.QueryScenario("selectDepartments", "select * from scott.dept");
  @Test
  public void test() throws DatabaseException {
    final QueryLoadTestModel loadTest = new QueryLoadTestModel(DatabasesTest.createTestDatabaseInstance(), User.UNIT_TEST_USER,
            Arrays.asList(SELECT_DEPARTMENTS, SELECT_EMPLOYEE));
    loadTest.setMinimumThinkTime(10);
    loadTest.setMaximumThinkTime(30);
    loadTest.setLoginDelayFactor(0);
    loadTest.setApplicationBatchSize(6);
    loadTest.addApplicationBatch();
    try {
      Thread.sleep(1500);
    }
    catch (final InterruptedException ignored) {/*ignored*/}
    loadTest.removeApplicationBatch();
    try {
      Thread.sleep(500);
    }
    catch (final InterruptedException ignored) {/*ignored*/}
    assertTrue(SELECT_DEPARTMENTS.getSuccessfulRunCount() > 0);
    assertTrue(SELECT_EMPLOYEE.getSuccessfulRunCount() > 0);
    loadTest.exit();
  }
}
