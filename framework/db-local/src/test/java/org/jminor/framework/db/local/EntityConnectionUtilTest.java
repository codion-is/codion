/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.ProgressReporter;
import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.dbms.h2database.H2Database;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionUtil;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntityConnectionUtilTest {

  private static final Domain DOMAIN = new TestDomain();
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray()));

  private static LocalEntityConnection DESTINATION_CONNECTION;

  @BeforeAll
  public static void setUp() {
    try {
      final H2Database destinationDatabase = new H2Database("TempDB", "src/test/sql/create_h2_db.sql");
      DESTINATION_CONNECTION = LocalEntityConnections.createConnection(DOMAIN, destinationDatabase, new User("sa", null));
      DESTINATION_CONNECTION.getDatabaseConnection().getConnection().createStatement().execute("alter table scott.emp drop constraint emp_mgr_fk");
      DESTINATION_CONNECTION.delete(Conditions.entityCondition(TestDomain.T_EMP));
      DESTINATION_CONNECTION.delete(Conditions.entityCondition(TestDomain.T_DEPARTMENT));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @AfterAll
  public static void tearDown() {
    DESTINATION_CONNECTION.disconnect();
  }

  @Test
  public void copyEntities() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = CONNECTION_PROVIDER.getConnection();
    EntityConnectionUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 2, true, TestDomain.T_DEPARTMENT);

    assertEquals(sourceConnection.selectRowCount(Conditions.entityCondition(TestDomain.T_DEPARTMENT)),
            DESTINATION_CONNECTION.selectRowCount(Conditions.entityCondition(TestDomain.T_DEPARTMENT)));

    EntityConnectionUtil.copyEntities(sourceConnection, DESTINATION_CONNECTION, 2, true, TestDomain.T_EMP);
    DESTINATION_CONNECTION.selectMany(Conditions.entitySelectCondition(TestDomain.T_EMP));

    DESTINATION_CONNECTION.delete(Conditions.entityCondition(TestDomain.T_EMP));
    DESTINATION_CONNECTION.delete(Conditions.entityCondition(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void batchInsert() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = CONNECTION_PROVIDER.getConnection();

    final List<Entity> source = sourceConnection.selectMany(Conditions.entitySelectCondition(TestDomain.T_DEPARTMENT));
    final List<Entity.Key> dest = new ArrayList<>();
    final ProgressReporter progressReporter = currentProgress -> {};
    EntityConnectionUtil.batchInsert(DESTINATION_CONNECTION, source, dest, 2, progressReporter);
    assertEquals(sourceConnection.selectRowCount(Conditions.entityCondition(TestDomain.T_DEPARTMENT)),
            DESTINATION_CONNECTION.selectRowCount(Conditions.entityCondition(TestDomain.T_DEPARTMENT)));

    EntityConnectionUtil.batchInsert(DESTINATION_CONNECTION, emptyList(), null, 10, null);
    DESTINATION_CONNECTION.delete(Conditions.entityCondition(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void batchInsertNegativeBatchSize() throws DatabaseException {
    assertThrows(IllegalArgumentException.class, () -> EntityConnectionUtil.batchInsert(null, null, null, -6, null));
  }
}
