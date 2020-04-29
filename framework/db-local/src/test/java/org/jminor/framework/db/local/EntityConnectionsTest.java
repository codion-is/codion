/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.local;

import org.jminor.common.db.database.Database;
import org.jminor.common.db.database.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.dbms.h2database.H2DatabaseProvider;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnections;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entity;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.jminor.framework.db.condition.Conditions.condition;
import static org.jminor.framework.db.condition.Conditions.selectCondition;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntityConnectionsTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));
  private static final Domain DOMAIN = new TestDomain();
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private static LocalEntityConnection DESTINATION_CONNECTION;

  @BeforeAll
  public static void setUp() {
    try {
      final Database destinationDatabase = new H2DatabaseProvider().createDatabase("jdbc:h2:mem:TempDB", "src/test/sql/create_h2_db.sql");
      DESTINATION_CONNECTION = LocalEntityConnections.createConnection(DOMAIN, destinationDatabase, Users.user("sa"));
      DESTINATION_CONNECTION.getDatabaseConnection().getConnection().createStatement().execute("alter table scott.emp drop constraint emp_mgr_fk");
      DESTINATION_CONNECTION.delete(condition(TestDomain.T_EMP));
      DESTINATION_CONNECTION.delete(condition(TestDomain.T_DEPARTMENT));
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
    EntityConnections.copyEntities(sourceConnection, DESTINATION_CONNECTION, 2, true, TestDomain.T_DEPARTMENT);

    assertEquals(sourceConnection.selectRowCount(condition(TestDomain.T_DEPARTMENT)),
            DESTINATION_CONNECTION.selectRowCount(condition(TestDomain.T_DEPARTMENT)));

    EntityConnections.copyEntities(sourceConnection, DESTINATION_CONNECTION, 2, true, TestDomain.T_EMP);
    DESTINATION_CONNECTION.select(selectCondition(TestDomain.T_EMP));

    DESTINATION_CONNECTION.delete(condition(TestDomain.T_EMP));
    DESTINATION_CONNECTION.delete(condition(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void batchInsert() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = CONNECTION_PROVIDER.getConnection();

    final List<Entity> source = sourceConnection.select(selectCondition(TestDomain.T_DEPARTMENT));

    final EventDataListener<Integer> progressReporter = currentProgress -> {};
    final List<Entity.Key> dest = EntityConnections.batchInsert(DESTINATION_CONNECTION, source, 2, progressReporter);
    assertEquals(sourceConnection.selectRowCount(condition(TestDomain.T_DEPARTMENT)),
            DESTINATION_CONNECTION.selectRowCount(condition(TestDomain.T_DEPARTMENT)));
    assertEquals(4, dest.size());

    EntityConnections.batchInsert(DESTINATION_CONNECTION, emptyList(), 10, null);
    DESTINATION_CONNECTION.delete(condition(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void batchInsertNegativeBatchSize() throws DatabaseException {
    assertThrows(IllegalArgumentException.class, () -> EntityConnections.batchInsert(null, null, -6, null));
  }
}
