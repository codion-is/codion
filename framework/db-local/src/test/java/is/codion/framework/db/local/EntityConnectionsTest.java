/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.user.User;
import is.codion.dbms.h2database.H2DatabaseFactory;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.IncludePrimaryKeys;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static is.codion.framework.db.condition.Conditions.condition;
import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntityConnectionsTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final Domain DOMAIN = new TestDomain();
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private static LocalEntityConnection DESTINATION_CONNECTION;

  @BeforeAll
  public static void setUp() {
    try {
      final Database destinationDatabase = new H2DatabaseFactory().createDatabase("jdbc:h2:mem:TempDB", "src/test/sql/create_h2_db.sql");
      DESTINATION_CONNECTION = localEntityConnection(DOMAIN, destinationDatabase, User.user("sa"));
      DESTINATION_CONNECTION.getDatabaseConnection().getConnection().createStatement().execute("alter table scott.emp drop constraint emp_mgr_fk");
      DESTINATION_CONNECTION.delete(condition(TestDomain.T_EMP));
      DESTINATION_CONNECTION.delete(condition(TestDomain.Department.TYPE));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @AfterAll
  public static void tearDown() {
    DESTINATION_CONNECTION.close();
  }

  @Test
  public void copyEntities() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = CONNECTION_PROVIDER.getConnection();
    EntityConnection.copyEntities(sourceConnection, DESTINATION_CONNECTION, 2, IncludePrimaryKeys.YES, TestDomain.Department.TYPE);

    assertEquals(sourceConnection.rowCount(condition(TestDomain.Department.TYPE)),
            DESTINATION_CONNECTION.rowCount(condition(TestDomain.Department.TYPE)));

    EntityConnection.copyEntities(sourceConnection, DESTINATION_CONNECTION, 2, IncludePrimaryKeys.YES, TestDomain.T_EMP);
    DESTINATION_CONNECTION.select(condition(TestDomain.T_EMP));

    DESTINATION_CONNECTION.delete(condition(TestDomain.T_EMP));
    DESTINATION_CONNECTION.delete(condition(TestDomain.Department.TYPE));
  }

  @Test
  public void batchInsert() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = CONNECTION_PROVIDER.getConnection();

    final List<Entity> source = sourceConnection.select(condition(TestDomain.Department.TYPE));

    final EventDataListener<Integer> progressReporter = currentProgress -> {};
    EntityConnection.batchInsert(DESTINATION_CONNECTION, source.iterator(), 2, progressReporter, null);
    assertEquals(sourceConnection.rowCount(condition(TestDomain.Department.TYPE)),
            DESTINATION_CONNECTION.rowCount(condition(TestDomain.Department.TYPE)));

    EntityConnection.batchInsert(DESTINATION_CONNECTION, Collections.emptyIterator(), 10, null, null);
    DESTINATION_CONNECTION.delete(condition(TestDomain.Department.TYPE));
  }

  @Test
  public void batchInsertNegativeBatchSize() throws DatabaseException {
    assertThrows(IllegalArgumentException.class, () -> EntityConnection.batchInsert(CONNECTION_PROVIDER.getConnection(),
            Collections.emptyIterator(), -6, null, null));
  }
}
