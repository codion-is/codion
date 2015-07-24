/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.ProgressReporter;
import org.jminor.common.model.User;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.local.LocalEntityConnectionTest;
import org.jminor.framework.db.local.LocalEntityConnections;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.TestDomain;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EntityConnectionUtilTest {

  private static EntityConnection DESTINATION_CONNECTION;

  static {
    TestDomain.init();
  }

  @BeforeClass
  public static void setUp() {
    try {
      final H2Database destinationDatabase = new H2Database("TempDB", "resources/demos/empdept/scripts/ddl.sql");
      DESTINATION_CONNECTION = LocalEntityConnections.createConnection(destinationDatabase, new User("sa", ""));
      DESTINATION_CONNECTION.delete(EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT));
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
    final EntityConnection sourceConnection = LocalEntityConnectionTest.CONNECTION_PROVIDER.getConnection();
    EntityConnectionUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 2, true, TestDomain.T_DEPARTMENT);

    assertEquals(sourceConnection.selectRowCount(EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT)),
            DESTINATION_CONNECTION.selectRowCount(EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT)));
    DESTINATION_CONNECTION.delete(EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void batchInsert() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = LocalEntityConnectionTest.CONNECTION_PROVIDER.getConnection();

    final List<Entity> source = sourceConnection.selectAll(TestDomain.T_DEPARTMENT);
    final List<Entity.Key> dest = new ArrayList<>();
    final ProgressReporter progressReporter = new ProgressReporter() {
      @Override
      public void reportProgress(final int currentProgress) {}
    };
    EntityConnectionUtil.batchInsert(DESTINATION_CONNECTION, source, dest, 2, progressReporter);
    assertEquals(sourceConnection.selectRowCount(EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT)),
            DESTINATION_CONNECTION.selectRowCount(EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT)));

    EntityConnectionUtil.batchInsert(DESTINATION_CONNECTION, Collections.<Entity>emptyList(), null, 10, null);
    DESTINATION_CONNECTION.delete(EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT));
  }

  @Test(expected = IllegalArgumentException.class)
  public void batchInsertNegativeBatchSize() throws DatabaseException {
    EntityConnectionUtil.batchInsert(null, null, null, -6, null);
  }
}
