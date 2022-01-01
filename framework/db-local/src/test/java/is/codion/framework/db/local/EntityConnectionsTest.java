/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.user.User;
import is.codion.dbms.h2database.H2DatabaseFactory;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.IncludePrimaryKeys;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.TestDomain.Department;
import is.codion.framework.db.local.TestDomain.Employee;
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
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private static LocalEntityConnection DESTINATION_CONNECTION;

  @BeforeAll
  public static void setUp() {
    try {
      final Database destinationDatabase = new H2DatabaseFactory().createDatabase("jdbc:h2:mem:TempDB", "src/test/sql/create_h2_db.sql");
      DESTINATION_CONNECTION = localEntityConnection(DOMAIN, destinationDatabase, User.user("sa"));
      DESTINATION_CONNECTION.getDatabaseConnection().getConnection().createStatement().execute("alter table scott.emp drop constraint emp_mgr_fk");
      DESTINATION_CONNECTION.delete(condition(Employee.TYPE));
      DESTINATION_CONNECTION.delete(condition(Department.TYPE));
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
  void copyEntities() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = CONNECTION_PROVIDER.getConnection();
    EntityConnection.copyEntities(sourceConnection, DESTINATION_CONNECTION, 2, IncludePrimaryKeys.YES, Department.TYPE);

    assertEquals(sourceConnection.rowCount(condition(Department.TYPE)),
            DESTINATION_CONNECTION.rowCount(condition(Department.TYPE)));

    EntityConnection.copyEntities(sourceConnection, DESTINATION_CONNECTION, 2, IncludePrimaryKeys.YES, Employee.TYPE);
    DESTINATION_CONNECTION.select(condition(Employee.TYPE));

    DESTINATION_CONNECTION.delete(condition(Employee.TYPE));
    DESTINATION_CONNECTION.delete(condition(Department.TYPE));
  }

  @Test
  void batchInsert() throws SQLException, DatabaseException {
    final EntityConnection sourceConnection = CONNECTION_PROVIDER.getConnection();

    final List<Entity> source = sourceConnection.select(condition(Department.TYPE));

    final EventDataListener<Integer> progressReporter = currentProgress -> {};
    EntityConnection.batchInsert(DESTINATION_CONNECTION, source.iterator(), 2, progressReporter, null);
    assertEquals(sourceConnection.rowCount(condition(Department.TYPE)),
            DESTINATION_CONNECTION.rowCount(condition(Department.TYPE)));

    EntityConnection.batchInsert(DESTINATION_CONNECTION, Collections.emptyIterator(), 10, null, null);
    DESTINATION_CONNECTION.delete(condition(Department.TYPE));
  }

  @Test
  void batchInsertNegativeBatchSize() throws DatabaseException {
    assertThrows(IllegalArgumentException.class, () -> EntityConnection.batchInsert(CONNECTION_PROVIDER.getConnection(),
            Collections.emptyIterator(), -6, null, null));
  }
}
