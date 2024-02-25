/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.tools.loadtest;

import is.codion.common.db.database.Database;
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
          new QueryLoadTestModel.QueryScenario(UNIT_TEST_USER, "selectEmployees", "select * from employees.employee where ename not like ?") {
            @Override
            protected List<Object> parameters() {
              return singletonList("ADAMS");
            }
          };
  private static final QueryLoadTestModel.QueryScenario SELECT_DEPARTMENTS =
          new QueryLoadTestModel.QueryScenario(UNIT_TEST_USER, "selectDepartments", "select * from employees.department", true);
  @Test
  void test() throws DatabaseException {
    QueryLoadTestModel queryLoadTest = new QueryLoadTestModel(Database.instance(), UNIT_TEST_USER,
            asList(SELECT_DEPARTMENTS, SELECT_EMPLOYEE));
    queryLoadTest.loadTest().minimumThinkTime().set(10);
    queryLoadTest.loadTest().maximumThinkTime().set(30);
    queryLoadTest.loadTest().loginDelayFactor().set(1);
    queryLoadTest.loadTest().applicationBatchSize().set(6);
    queryLoadTest.loadTest().addApplicationBatch();
    try {
      Thread.sleep(1500);
    }
    catch (InterruptedException ignored) {/*ignored*/}
    queryLoadTest.loadTest().removeApplicationBatch();
    try {
      Thread.sleep(500);
    }
    catch (InterruptedException ignored) {/*ignored*/}
    assertTrue(SELECT_DEPARTMENTS.successfulRunCount() > 0);
    assertTrue(SELECT_EMPLOYEE.successfulRunCount() > 0);
    queryLoadTest.loadTest().shutdown();
  }
}
