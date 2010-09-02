/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.tools;

import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.model.User;

import org.junit.Test;

import java.util.Arrays;

public final class QueryLoadTestModelTest {

  @Test
  public void test() {
    final H2Database database = new H2Database("h2db/h2");
    final QueryLoadTestModel loadTest = new QueryLoadTestModel(database, User.UNIT_TEST_USER,
            Arrays.asList(
                    new QueryLoadTestModel.QueryScenario("selectEmployees", "select * from scott.emp"),
                    new QueryLoadTestModel.QueryScenario("selectDepartments", "select * from scott.dept"),
                    new QueryLoadTestModel.QueryScenario("selectCustomers", "select * from chinook.customer"),
                    new QueryLoadTestModel.QueryScenario("selectAlbum", "select * from chinook.album"),
                    new QueryLoadTestModel.QueryScenario("selectProducts", "select * from petstore.product")));
    loadTest.addApplicationBatch();
    try {
      Thread.sleep(1000);
    }
    catch (InterruptedException e) {/**/}
    loadTest.removeApplicationBatch();
    try {
      Thread.sleep(1000);
    }
    catch (InterruptedException e) {/**/}
    loadTest.exit();
  }
}
