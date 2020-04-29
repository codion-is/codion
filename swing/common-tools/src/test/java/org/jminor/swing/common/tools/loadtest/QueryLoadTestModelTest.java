/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.tools.loadtest;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class QueryLoadTestModelTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));

  private static final QueryLoadTestModel.QueryScenario SELECT_EMPLOYEE =
          new QueryLoadTestModel.QueryScenario("selectEmployees", "select * from scott.emp where ename not like ?") {
            @Override
            protected List<Object> getParameters() {
              return singletonList("ADAMS");
            }
          };
  private static final QueryLoadTestModel.QueryScenario SELECT_DEPARTMENTS =
          new QueryLoadTestModel.QueryScenario("selectDepartments", "select * from scott.dept", true);
  @Test
  public void test() throws DatabaseException {
    final QueryLoadTestModel loadTest = new QueryLoadTestModel(createTestDatabaseInstance(), UNIT_TEST_USER,
            asList(SELECT_DEPARTMENTS, SELECT_EMPLOYEE));
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
    loadTest.shutdown();
  }

  private static Database createTestDatabaseInstance() {
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPT.set("../../demos/empdept/src/main/sql/create_schema.sql");

    return Databases.getInstance();
  }
}
