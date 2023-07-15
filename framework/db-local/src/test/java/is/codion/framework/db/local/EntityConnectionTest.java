/*
 * Copyright (c) 2014 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.result.ResultIterator;
import is.codion.common.event.EventDataListener;
import is.codion.common.user.User;
import is.codion.dbms.h2database.H2DatabaseFactory;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.local.TestDomain.Department;
import is.codion.framework.db.local.TestDomain.Employee;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static is.codion.framework.db.condition.Condition.condition;
import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntityConnectionTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final Domain DOMAIN = new TestDomain();

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  private static LocalEntityConnection DESTINATION_CONNECTION;

  @BeforeAll
  public static void setUp() {
    try {
      Database destinationDatabase = H2DatabaseFactory.createDatabase("jdbc:h2:mem:TempDB", "src/test/sql/create_h2_db.sql");
      DESTINATION_CONNECTION = localEntityConnection(destinationDatabase, DOMAIN, User.user("sa"));
      DESTINATION_CONNECTION.databaseConnection().getConnection().createStatement().execute("alter table scott.emp drop constraint emp_mgr_fk");
      DESTINATION_CONNECTION.delete(condition(Employee.TYPE));
      DESTINATION_CONNECTION.delete(condition(Department.TYPE));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @AfterAll
  public static void tearDown() {
    DESTINATION_CONNECTION.close();
  }

  @Test
  void copyEntities() throws DatabaseException {
    EntityConnection sourceConnection = CONNECTION_PROVIDER.connection();
    EntityConnection.copyEntities(sourceConnection, DESTINATION_CONNECTION)
            .entityTypes(Department.TYPE)
            .batchSize(2)
            .execute();

    assertEquals(sourceConnection.rowCount(condition(Department.TYPE)),
            DESTINATION_CONNECTION.rowCount(condition(Department.TYPE)));

    assertThrows(IllegalArgumentException.class, () -> EntityConnection.copyEntities(sourceConnection, DESTINATION_CONNECTION)
            .entityTypes(Employee.TYPE)
            .batchSize(-10));

    assertThrows(IllegalArgumentException.class, () -> EntityConnection.copyEntities(sourceConnection, DESTINATION_CONNECTION)
            .entityTypes(Employee.TYPE)
            .condition(Condition.condition(Department.TYPE)));

    EntityConnection.copyEntities(sourceConnection, DESTINATION_CONNECTION)
            .entityTypes(Employee.TYPE)
            .batchSize(2)
            .includePrimaryKeys(false)
            .condition(Condition.where(Employee.SALARY).greaterThan(1000d))
            .execute();
    assertEquals(13, DESTINATION_CONNECTION.rowCount(condition(Employee.TYPE)));

    DESTINATION_CONNECTION.delete(condition(Employee.TYPE));
    DESTINATION_CONNECTION.delete(condition(Department.TYPE));
  }

  @Test
  void insertEntities() throws DatabaseException {
    LocalEntityConnection sourceConnection = CONNECTION_PROVIDER.connection();
    try (ResultIterator<Entity> iterator = sourceConnection.iterator(condition(Department.TYPE))) {
      assertThrows(IllegalArgumentException.class, () -> EntityConnection.insertEntities(DESTINATION_CONNECTION, iterator.iterator())
              .batchSize(-10));

      EventDataListener<Integer> progressReporter = currentProgress -> {};
      EntityConnection.insertEntities(DESTINATION_CONNECTION, iterator.iterator())
              .batchSize(2)
              .progressReporter(progressReporter)
              .onInsert(keys -> {})
              .execute();
    }
    assertEquals(sourceConnection.rowCount(condition(Department.TYPE)),
            DESTINATION_CONNECTION.rowCount(condition(Department.TYPE)));

    EntityConnection.insertEntities(DESTINATION_CONNECTION, Collections.emptyIterator())
            .batchSize(10)
            .execute();
    DESTINATION_CONNECTION.delete(condition(Department.TYPE));
  }

  @Test
  void batchInsertNegativeBatchSize() {
    assertThrows(IllegalArgumentException.class, () ->
            EntityConnection.insertEntities(CONNECTION_PROVIDER.connection(), Collections.emptyIterator())
                    .batchSize(-6));
  }
}
