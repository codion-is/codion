/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.db.http.TestDomain.Department;
import is.codion.framework.db.http.TestDomain.Employee;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.framework.servlet.EntityService;
import is.codion.framework.servlet.EntityServiceFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static is.codion.framework.db.condition.Condition.condition;
import static is.codion.framework.db.condition.Condition.where;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractHttpEntityConnectionTest {

  private static final Integer WEB_SERVER_PORT_NUMBER = 8089;

  private static EntityServer server;

  private final EntityConnection connection;

  protected AbstractHttpEntityConnectionTest(EntityConnection connection) {
    this.connection = connection;
  }

  @BeforeAll
  public static void setUp() throws Exception {
    server = EntityServer.startServer(configure());
  }

  @AfterAll
  public static void tearDown() throws Exception {
    server.shutdown();
    deconfigure();
  }

  @Test
  void executeProcedure() throws IOException, DatabaseException {
    connection.executeProcedure(TestDomain.PROCEDURE_ID);
  }

  @Test
  void executeFunction() throws IOException, DatabaseException {
    assertNotNull(connection.executeFunction(TestDomain.FUNCTION_ID));
  }

  @Test
  void fillReport() throws ReportException, DatabaseException, IOException {
    String result = connection.fillReport(TestDomain.REPORT, "");
    assertNotNull(result);
  }

  @Test
  void insert() throws IOException, DatabaseException {
    Entity entity = connection.entities().builder(Department.TYPE)
            .with(Department.ID, 33)
            .with(Department.NAME, "name")
            .with(Department.LOCATION, "loc")
            .build();
    Key key = connection.insert(entity);
    assertEquals(Integer.valueOf(33), key.get());
    connection.delete(key);
  }

  @Test
  void selectByKey() throws IOException, DatabaseException {
    Key key = connection.entities().primaryKey(Department.TYPE, 10);
    Collection<Entity> depts = connection.select(singletonList(key));
    assertEquals(1, depts.size());
  }

  @Test
  void selectByKeyDifferentEntityTypes() throws IOException, DatabaseException {
    Key deptKey = connection.entities().primaryKey(Department.TYPE, 10);
    Key empKey = connection.entities().primaryKey(Employee.TYPE, 8);

    Collection<Entity> selected = connection.select(asList(deptKey, empKey));
    assertEquals(2, selected.size());
  }

  @Test
  void selectByValue() throws IOException, DatabaseException {
    List<Entity> department = connection.select(Department.NAME, "SALES");
    assertEquals(1, department.size());
  }

  @Test
  void update() throws IOException, DatabaseException {
    Entity department = connection.selectSingle(Department.NAME, "ACCOUNTING");
    department.put(Department.NAME, "TEstING");
    connection.update(department);
    department = connection.selectSingle(Department.ID, department.get(Department.ID));
    assertEquals("TEstING", department.get(Department.NAME));
    department.put(Department.NAME, "ACCOUNTING");
    connection.update(department);
  }

  @Test
  void updateByCondition() throws DatabaseException {
    Condition selectCondition = where(Employee.COMMISSION).isNull();

    List<Entity> entities = connection.select(selectCondition);

    UpdateCondition updateCondition = where(Employee.COMMISSION).isNull().updateBuilder()
            .set(Employee.COMMISSION, 500d)
            .set(Employee.SALARY, 4200d)
            .build();
    connection.beginTransaction();
    try {
      connection.update(updateCondition);
      assertEquals(0, connection.rowCount(selectCondition));
      Collection<Entity> afterUpdate = connection.select(Entity.primaryKeys(entities));
      for (Entity entity : afterUpdate) {
        assertEquals(500d, entity.get(Employee.COMMISSION));
        assertEquals(4200d, entity.get(Employee.SALARY));
      }
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void deleteByKey() throws IOException, DatabaseException {
    Entity employee = connection.selectSingle(Employee.NAME, "ADAMS");
    connection.beginTransaction();
    try {
      connection.delete(employee.primaryKey());
      Collection<Entity> selected = connection.select(singletonList(employee.primaryKey()));
      assertTrue(selected.isEmpty());
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void deleteByKeyDifferentEntityTypes() throws IOException, DatabaseException {
    Key deptKey = connection.entities().primaryKey(Department.TYPE, 40);
    Key empKey = connection.entities().primaryKey(Employee.TYPE, 1);
    connection.beginTransaction();
    try {
      assertEquals(2, connection.select(asList(deptKey, empKey)).size());
      connection.delete(asList(deptKey, empKey));
      Collection<Entity> selected = connection.select(asList(deptKey, empKey));
      assertTrue(selected.isEmpty());
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void selectDependencies() throws IOException, DatabaseException {
    Entity department = connection.selectSingle(Department.NAME, "SALES");
    Map<EntityType, Collection<Entity>> dependentEntities = connection.selectDependencies(singletonList(department));
    assertNotNull(dependentEntities);
    assertTrue(dependentEntities.containsKey(Employee.TYPE));
    assertFalse(dependentEntities.get(Employee.TYPE).isEmpty());
  }

  @Test
  void rowCount() throws IOException, DatabaseException {
    assertEquals(4, connection.rowCount(condition(Department.TYPE)));
  }

  @Test
  void selectValues() throws IOException, DatabaseException {
    List<String> values = connection.select(Department.NAME);
    assertEquals(4, values.size());
  }

  @Test
  void transactions() throws IOException, DatabaseException {
    assertFalse(connection.isTransactionOpen());
    connection.beginTransaction();
    assertTrue(connection.isTransactionOpen());
    connection.rollbackTransaction();
    assertFalse(connection.isTransactionOpen());
    connection.beginTransaction();
    assertTrue(connection.isTransactionOpen());
    connection.commitTransaction();
    assertFalse(connection.isTransactionOpen());
  }

  @Test
  void writeReadBlob() throws DatabaseException {
    byte[] bytes = new byte[1024];
    new Random().nextBytes(bytes);

    Entity scott = connection.selectSingle(Employee.ID, 7);
    connection.writeBlob(scott.primaryKey(), Employee.DATA, bytes);
    assertArrayEquals(bytes, connection.readBlob(scott.primaryKey(), Employee.DATA));
  }

  @Test
  void close() throws IOException, DatabaseException {
    connection.close();
    assertFalse(connection.isConnected());
  }

  @Test
  void deleteDepartmentWithEmployees() throws IOException, DatabaseException {
    Entity department = connection.selectSingle(Department.NAME, "SALES");
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(Condition.condition(department.primaryKey())));
  }

  @Test
  void foreignKeyValues() throws DatabaseException {
    Entity employee = connection.selectSingle(Employee.ID, 5);
    assertNotNull(employee.get(Employee.DEPARTMENT_FK));
    assertNotNull(employee.get(Employee.MGR_FK));
  }

  @Test
  void queryCache() throws DatabaseException {
    connection.setQueryCacheEnabled(true);
    assertTrue(connection.isQueryCacheEnabled());
    connection.setQueryCacheEnabled(false);
    assertFalse(connection.isQueryCacheEnabled());
  }

  @Test
  void rollbackWithNoOpenTransaction() {
    assertThrows(IllegalStateException.class, connection::rollbackTransaction);
  }

  private static EntityServerConfiguration configure() {
    Report.REPORT_PATH.set("report/path");
    EntityService.HTTP_SERVER_PORT.set(WEB_SERVER_PORT_NUMBER);
    HttpEntityConnection.SECURE.set(false);
    HttpEntityConnection.PORT.set(WEB_SERVER_PORT_NUMBER);
    System.setProperty("java.security.policy", "../../framework/server/src/main/config/all_permissions.policy");

    return EntityServerConfiguration.builder(3223, 3221)
            .adminPort(3223)
            .database(Database.instance())
            .domainClassNames(singletonList(TestDomain.class.getName()))
            .sslEnabled(false)
            .auxiliaryServerFactoryClassNames(singletonList(EntityServiceFactory.class.getName()))
            .build();
  }

  private static void deconfigure() {
    Report.REPORT_PATH.set(null);
    EntityService.HTTP_SERVER_PORT.set(null);
    HttpEntityConnection.PORT.set(null);
    HttpEntityConnection.SECURE.set(false);
    System.clearProperty("java.security.policy");
  }
}
