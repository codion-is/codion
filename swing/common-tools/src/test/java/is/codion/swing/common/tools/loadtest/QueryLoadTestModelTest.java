/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.tools.loadtest;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class QueryLoadTestModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final QueryLoadTestModel.QueryScenario SELECT_EMPLOYEE =
          new QueryLoadTestModel.QueryScenario(UNIT_TEST_USER, "selectEmployees", "select * from scott.emp where ename not like ?") {
            @Override
            protected List<Object> getParameters() {
              return singletonList("ADAMS");
            }
          };
  private static final QueryLoadTestModel.QueryScenario SELECT_DEPARTMENTS =
          new QueryLoadTestModel.QueryScenario(UNIT_TEST_USER, "selectDepartments", "select * from scott.dept", true);
  @Test
  void test() throws DatabaseException {
    QueryLoadTestModel loadTest = new QueryLoadTestModel(createTestDatabaseInstance(), UNIT_TEST_USER,
            asList(SELECT_DEPARTMENTS, SELECT_EMPLOYEE));
    loadTest.getMinimumThinkTimeValue().set(10);
    loadTest.getMaximumThinkTimeValue().set(30);
    loadTest.getLoginDelayFactorValue().set(1);
    loadTest.getApplicationBatchSizeValue().set(6);
    loadTest.addApplicationBatch();
    try {
      Thread.sleep(1500);
    }
    catch (InterruptedException ignored) {/*ignored*/}
    loadTest.removeApplicationBatch();
    try {
      Thread.sleep(500);
    }
    catch (InterruptedException ignored) {/*ignored*/}
    assertTrue(SELECT_DEPARTMENTS.getSuccessfulRunCount() > 0);
    assertTrue(SELECT_EMPLOYEE.getSuccessfulRunCount() > 0);
    loadTest.shutdown();
  }

  private static Database createTestDatabaseInstance() {
    Database.DATABASE_URL.set("jdbc:h2:mem:h2db");
    Database.DATABASE_INIT_SCRIPTS.set("../../demos/empdept/src/main/sql/create_schema.sql");

    return DatabaseFactory.getDatabase();
  }
}
