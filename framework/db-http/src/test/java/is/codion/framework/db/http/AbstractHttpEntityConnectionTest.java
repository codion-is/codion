/*
 * Copyright (c) 2017 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.rmi.client.Clients;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.http.TestDomain.Department;
import is.codion.framework.db.http.TestDomain.Employee;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.framework.servlet.EntityService;
import is.codion.framework.servlet.EntityServiceFactory;

import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static is.codion.framework.db.condition.Condition.column;
import static is.codion.framework.db.condition.Condition.key;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractHttpEntityConnectionTest {

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
  public static void tearDown() {
    server.shutdown();
    deconfigure();
  }

  @Test
  void executeProcedure() throws DatabaseException {
    connection.executeProcedure(TestDomain.PROCEDURE_ID);
  }

  @Test
  void executeFunction() throws DatabaseException {
    assertNotNull(connection.executeFunction(TestDomain.FUNCTION_ID));
  }

  @Test
  void fillReport() throws ReportException, DatabaseException {
    String result = connection.fillReport(TestDomain.REPORT, "");
    assertNotNull(result);
  }

  @Test
  void insert() throws DatabaseException {
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
  void selectByKey() throws DatabaseException {
    Key key = connection.entities().primaryKey(Department.TYPE, 10);
    Collection<Entity> depts = connection.select(singletonList(key));
    assertEquals(1, depts.size());
  }

  @Test
  void selectByKeyDifferentEntityTypes() throws DatabaseException {
    Key deptKey = connection.entities().primaryKey(Department.TYPE, 10);
    Key empKey = connection.entities().primaryKey(Employee.TYPE, 8);

    Collection<Entity> selected = connection.select(asList(deptKey, empKey));
    assertEquals(2, selected.size());
  }

  @Test
  void update() throws DatabaseException {
    Entity department = connection.selectSingle(column(Department.NAME).equalTo("ACCOUNTING"));
    department.put(Department.NAME, "TEstING");
    connection.update(department);
    department = connection.selectSingle(column(Department.ID).equalTo(department.get(Department.ID)));
    assertEquals("TEstING", department.get(Department.NAME));
    department.put(Department.NAME, "ACCOUNTING");
    connection.update(department);
  }

  @Test
  void updateByCondition() throws DatabaseException {
    Condition condition = column(Employee.COMMISSION).isNull();

    List<Entity> entities = connection.select(condition);

    Update update = Update.where(column(Employee.COMMISSION).isNull())
            .set(Employee.COMMISSION, 500d)
            .set(Employee.SALARY, 4200d)
            .build();
    connection.beginTransaction();
    try {
      connection.update(update);
      assertEquals(0, connection.count(condition));
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
  void deleteByKey() throws DatabaseException {
    Entity employee = connection.selectSingle(column(Employee.NAME).equalTo("ADAMS"));
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
  void deleteByKeyDifferentEntityTypes() throws DatabaseException {
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
  void selectDependencies() throws DatabaseException {
    Entity department = connection.selectSingle(column(Department.NAME).equalTo("SALES"));
    Map<EntityType, Collection<Entity>> dependentEntities = connection.selectDependencies(singletonList(department));
    assertNotNull(dependentEntities);
    assertTrue(dependentEntities.containsKey(Employee.TYPE));
    assertFalse(dependentEntities.get(Employee.TYPE).isEmpty());
  }

  @Test
  void rowCount() throws DatabaseException {
    assertEquals(4, connection.count(Condition.all(Department.TYPE)));
  }

  @Test
  void selectValues() throws DatabaseException {
    List<String> values = connection.select(Department.NAME);
    assertEquals(4, values.size());
  }

  @Test
  void transactions() {
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

    Entity scott = connection.selectSingle(column(Employee.ID).equalTo(7));
    connection.writeBlob(scott.primaryKey(), Employee.DATA, bytes);
    assertArrayEquals(bytes, connection.readBlob(scott.primaryKey(), Employee.DATA));
  }

  @Test
  void close() {
    connection.close();
    assertFalse(connection.isConnected());
  }

  @Test
  void deleteDepartmentWithEmployees() throws DatabaseException {
    Entity department = connection.selectSingle(column(Department.NAME).equalTo("SALES"));
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(key(department.primaryKey())));
  }

  @Test
  void foreignKeyValues() throws DatabaseException {
    Entity employee = connection.selectSingle(column(Employee.ID).equalTo(5));
    assertNotNull(employee.get(Employee.DEPARTMENT_FK));
    assertNotNull(employee.get(Employee.MGR_FK));
  }

  @Test
  void queryCache() {
    connection.setQueryCacheEnabled(true);
    assertTrue(connection.isQueryCacheEnabled());
    connection.setQueryCacheEnabled(false);
    assertFalse(connection.isQueryCacheEnabled());
  }

  @Test
  void rollbackWithNoOpenTransaction() {
    assertThrows(IllegalStateException.class, connection::rollbackTransaction);
  }

  static BasicHttpClientConnectionManager createConnectionManager() {
    return new BasicHttpClientConnectionManager();/*
    try {
      SSLContext sslContext = SSLContext.getDefault();

      return new BasicHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create().register("https",
                      new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
              .build());
    }
    catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }*/
  }

  private static EntityServerConfiguration configure() {
    Clients.SERVER_HOSTNAME.set("localhost");
    Clients.TRUSTSTORE.set("../../framework/server/src/main/config/truststore.jks");
    Clients.TRUSTSTORE_PASSWORD.set("crappypass");
    Clients.resolveTrustStore();
    Report.REPORT_PATH.set("report/path");
    EntityService.HTTP_SERVER_KEYSTORE_PATH.set("../../framework/server/src/main/config/keystore.jks");
    EntityService.HTTP_SERVER_KEYSTORE_PASSWORD.set("crappypass");
    EntityService.HTTP_SERVER_SECURE.set(false);
    HttpEntityConnection.SECURE.set(false);

    return EntityServerConfiguration.builder(3223, 3221)
            .adminPort(3223)
            .database(Database.instance())
            .domainClassNames(singletonList(TestDomain.class.getName()))
            .sslEnabled(false)
            .auxiliaryServerFactoryClassNames(singletonList(EntityServiceFactory.class.getName()))
            .build();
  }

  private static void deconfigure() {
    Clients.SERVER_HOSTNAME.set(null);
    Clients.TRUSTSTORE.set(null);
    Clients.TRUSTSTORE_PASSWORD.set(null);
    System.clearProperty(Clients.JAVAX_NET_TRUSTSTORE);
    System.clearProperty(Clients.JAVAX_NET_TRUSTSTORE_PASSWORD);
    Report.REPORT_PATH.set(null);
    EntityService.HTTP_SERVER_KEYSTORE_PATH.set(null);
    EntityService.HTTP_SERVER_KEYSTORE_PASSWORD.set(null);
    HttpEntityConnection.SECURE.set(false);
  }
}
