/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.ProgressReporter;
import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnections;
import org.jminor.framework.domain.Entity;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EntityConnectionUtilTest {

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger")), Databases.getInstance());

  private static EntityConnection DESTINATION_CONNECTION;

  static {
    TestDomain.init();
  }

  @BeforeClass
  public static void setUp() {
    try {
      final H2Database destinationDatabase = new H2Database("TempDB", "../demos/empdept/src/main/sql/create_schema.sql");
      DESTINATION_CONNECTION = LocalEntityConnections.createConnection(destinationDatabase, new User("sa", ""));
      DESTINATION_CONNECTION.getDatabaseConnection().getConnection().createStatement().execute("alter table scott.emp drop constraint emp_mgr_fk");
      DESTINATION_CONNECTION.delete(EntityConditions.condition(TestDomain.T_EMP));
      DESTINATION_CONNECTION.delete(EntityConditions.condition(TestDomain.T_DEPARTMENT));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @AfterClass
  public static void tearDown() {
    DESTINATION_CONNECTION.disconnect();
  }

  @Test
  public void copyEntities() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = CONNECTION_PROVIDER.getConnection();
    EntityConnectionUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 2, true, TestDomain.T_DEPARTMENT);

    assertEquals(sourceConnection.selectRowCount(EntityConditions.condition(TestDomain.T_DEPARTMENT)),
            DESTINATION_CONNECTION.selectRowCount(EntityConditions.condition(TestDomain.T_DEPARTMENT)));

    EntityConnectionUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 2, true, TestDomain.T_EMP);
    final List<Entity> employees = DESTINATION_CONNECTION.selectMany(EntityConditions.selectCondition(TestDomain.T_EMP));

    DESTINATION_CONNECTION.delete(EntityConditions.condition(TestDomain.T_EMP));
    DESTINATION_CONNECTION.delete(EntityConditions.condition(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void batchInsert() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = CONNECTION_PROVIDER.getConnection();

    final List<Entity> source = sourceConnection.selectMany(EntityConditions.selectCondition(TestDomain.T_DEPARTMENT));
    final List<Entity.Key> dest = new ArrayList<>();
    final ProgressReporter progressReporter = currentProgress -> {};
    EntityConnectionUtil.batchInsert(DESTINATION_CONNECTION, source, dest, 2, progressReporter);
    assertEquals(sourceConnection.selectRowCount(EntityConditions.condition(TestDomain.T_DEPARTMENT)),
            DESTINATION_CONNECTION.selectRowCount(EntityConditions.condition(TestDomain.T_DEPARTMENT)));

    EntityConnectionUtil.batchInsert(DESTINATION_CONNECTION, Collections.<Entity>emptyList(), null, 10, null);
    DESTINATION_CONNECTION.delete(EntityConditions.condition(TestDomain.T_DEPARTMENT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void batchInsertNegativeBatchSize() throws DatabaseException {
    EntityConnectionUtil.batchInsert(null, null, null, -6, null);
  }
}
