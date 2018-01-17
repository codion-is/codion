/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.tools;

import org.jminor.common.User;
import org.jminor.common.db.Database;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

public final class QueryLoadTestModelTest {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private static final QueryLoadTestModel.QueryScenario SELECT_EMPLOYEE =
          new QueryLoadTestModel.QueryScenario("selectEmployees", "select * from scott.emp where ename not like ?") {
            @Override
            protected List<Object> getParameters() {
              return Collections.singletonList("ADAMS");
            }
          };
  private static final QueryLoadTestModel.QueryScenario SELECT_DEPARTMENTS =
          new QueryLoadTestModel.QueryScenario("selectDepartments", "select * from scott.dept", true);
  @Test
  public void test() throws DatabaseException {
    final QueryLoadTestModel loadTest = new QueryLoadTestModel(createTestDatabaseInstance(), UNIT_TEST_USER,
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

  private static Database createTestDatabaseInstance() {
    final String type = Database.DATABASE_TYPE.get();
    final String host = Database.DATABASE_HOST.get();
    final Integer port = Database.DATABASE_PORT.get();
    final String sid = Database.DATABASE_SID.get();
    final Boolean embedded = Database.DATABASE_EMBEDDED.get();
    final Boolean embeddedInMemory = Database.DATABASE_EMBEDDED_IN_MEMORY.get();
    final String initScript = Database.DATABASE_INIT_SCRIPT.get();
    try {
      Database.DATABASE_TYPE.set(type == null ? Database.Type.H2.toString() : type);
      Database.DATABASE_HOST.set(host == null ? "h2db/h2" : host);
      Database.DATABASE_PORT.set(port);
      Database.DATABASE_SID.set(sid);
      Database.DATABASE_EMBEDDED.set(embedded == null ? true : embedded);
      Database.DATABASE_EMBEDDED_IN_MEMORY.set(embeddedInMemory == null ? true : embeddedInMemory);
      Database.DATABASE_INIT_SCRIPT.set(initScript == null ? "demos/src/main/sql/create_h2_db.sql" : initScript);

      return Databases.getInstance();
    }
    finally {
      setSystemProperties(type, host, port, sid, embedded, embeddedInMemory, initScript);
    }
  }

  private static void setSystemProperties(final String type, final String host, final Integer port, final String sid,
                                          final Boolean embedded, final Boolean embeddedInMemory, final String initScript) {
    if (type != null) {
      Database.DATABASE_TYPE.set(type);
    }
    if (host != null) {
      Database.DATABASE_HOST.set(host);
    }
    if (port != null) {
      Database.DATABASE_PORT.set(port);
    }
    if (sid != null) {
      Database.DATABASE_SID.set(sid);
    }
    if (embedded != null) {
      Database.DATABASE_EMBEDDED.set(embedded);
    }
    if (embeddedInMemory != null) {
      Database.DATABASE_EMBEDDED_IN_MEMORY.set(embeddedInMemory);
    }
    if (initScript != null) {
      Database.DATABASE_INIT_SCRIPT.set(initScript);
    }
  }
}
