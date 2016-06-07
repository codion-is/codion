/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db;

import org.jminor.common.User;
import org.jminor.common.db.dbms.H2Database;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.ProgressReporter;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
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
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class EntityConnectionUtilTest {

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
      DESTINATION_CONNECTION.delete(EntityCriteriaUtil.criteria(TestDomain.T_EMP));
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
    final EntityConnection sourceConnection = EntityConnectionProvidersTest.CONNECTION_PROVIDER.getConnection();
    EntityConnectionUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 2, true, TestDomain.T_DEPARTMENT);

    assertEquals(sourceConnection.selectRowCount(EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT)),
            DESTINATION_CONNECTION.selectRowCount(EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT)));

    EntityConnectionUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 2, false, TestDomain.T_EMP);
    final List<Entity> employees = DESTINATION_CONNECTION.selectMany(EntityCriteriaUtil.selectCriteria(TestDomain.T_EMP));
    boolean zeroIdFound = false;
    for (final Entity emp : employees) {
      if (Objects.equals(emp.get(TestDomain.EMP_ID), 0)) {
        zeroIdFound = true;
      }
    }

    DESTINATION_CONNECTION.delete(EntityCriteriaUtil.criteria(TestDomain.T_EMP));
    DESTINATION_CONNECTION.delete(EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT));

    if (!zeroIdFound) {
      fail("Ids were not regenerated on copy");
    }
  }

  @Test
  public void batchInsert() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = EntityConnectionProvidersTest.CONNECTION_PROVIDER.getConnection();

    final List<Entity> source = sourceConnection.selectMany(EntityCriteriaUtil.selectCriteria(TestDomain.T_DEPARTMENT));
    final List<Entity.Key> dest = new ArrayList<>();
    final ProgressReporter progressReporter = currentProgress -> {};
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
