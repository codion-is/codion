/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.ProgressReporter;
import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionUtil;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entities;
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

  private static final Entities ENTITIES = new TestDomain();
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(ENTITIES, new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray()), Databases.getInstance());
  private static final EntityConditions ENTITY_CONDITIONS = CONNECTION_PROVIDER.getConditions();

  private static EntityConnection DESTINATION_CONNECTION;

  @BeforeClass
  public static void setUp() {
    try {
      final H2Database destinationDatabase = new H2Database("TempDB", "src/test/sql/create_h2_db.sql");
      DESTINATION_CONNECTION = LocalEntityConnections.createConnection(ENTITIES, destinationDatabase, new User("sa", null));
      DESTINATION_CONNECTION.getDatabaseConnection().getConnection().createStatement().execute("alter table scott.emp drop constraint emp_mgr_fk");
      DESTINATION_CONNECTION.delete(ENTITY_CONDITIONS.condition(TestDomain.T_EMP));
      DESTINATION_CONNECTION.delete(ENTITY_CONDITIONS.condition(TestDomain.T_DEPARTMENT));
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
    EntityConnectionUtil.copyEntities(ENTITY_CONDITIONS, sourceConnection, DESTINATION_CONNECTION, 2, true, TestDomain.T_DEPARTMENT);

    assertEquals(sourceConnection.selectRowCount(ENTITY_CONDITIONS.condition(TestDomain.T_DEPARTMENT)),
            DESTINATION_CONNECTION.selectRowCount(ENTITY_CONDITIONS.condition(TestDomain.T_DEPARTMENT)));

    EntityConnectionUtil.copyEntities(ENTITY_CONDITIONS, sourceConnection, DESTINATION_CONNECTION, 2, true, TestDomain.T_EMP);
    final List<Entity> employees = DESTINATION_CONNECTION.selectMany(ENTITY_CONDITIONS.selectCondition(TestDomain.T_EMP));

    DESTINATION_CONNECTION.delete(ENTITY_CONDITIONS.condition(TestDomain.T_EMP));
    DESTINATION_CONNECTION.delete(ENTITY_CONDITIONS.condition(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void batchInsert() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = CONNECTION_PROVIDER.getConnection();

    final List<Entity> source = sourceConnection.selectMany(ENTITY_CONDITIONS.selectCondition(TestDomain.T_DEPARTMENT));
    final List<Entity.Key> dest = new ArrayList<>();
    final ProgressReporter progressReporter = currentProgress -> {};
    EntityConnectionUtil.batchInsert(DESTINATION_CONNECTION, source, dest, 2, progressReporter);
    assertEquals(sourceConnection.selectRowCount(ENTITY_CONDITIONS.condition(TestDomain.T_DEPARTMENT)),
            DESTINATION_CONNECTION.selectRowCount(ENTITY_CONDITIONS.condition(TestDomain.T_DEPARTMENT)));

    EntityConnectionUtil.batchInsert(DESTINATION_CONNECTION, Collections.<Entity>emptyList(), null, 10, null);
    DESTINATION_CONNECTION.delete(ENTITY_CONDITIONS.condition(TestDomain.T_DEPARTMENT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void batchInsertNegativeBatchSize() throws DatabaseException {
    EntityConnectionUtil.batchInsert(null, null, null, -6, null);
  }
}
