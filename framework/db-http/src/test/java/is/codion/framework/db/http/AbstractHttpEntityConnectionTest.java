/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.http.server.HttpServerConfiguration;
import is.codion.common.http.server.ServerHttps;
import is.codion.common.rmi.client.Clients;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.condition.UpdateCondition;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.server.EntityServer;
import is.codion.framework.server.EntityServerConfiguration;
import is.codion.framework.servlet.EntityServletServerFactory;

import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static is.codion.framework.db.condition.Conditions.condition;
import static is.codion.framework.db.condition.Conditions.where;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractHttpEntityConnectionTest {

  private static final Integer WEB_SERVER_PORT_NUMBER = 8089;

  private static EntityServer server;

  private final EntityConnection connection;

  protected AbstractHttpEntityConnectionTest(final EntityConnection connection) {
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
    Entity entity = connection.getEntities().builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, 33)
            .with(TestDomain.DEPARTMENT_NAME, "name")
            .with(TestDomain.DEPARTMENT_LOCATION, "loc")
            .build();
    Key key = connection.insert(entity);
    assertEquals(Integer.valueOf(33), key.get());
    connection.delete(key);
  }

  @Test
  void selectByKey() throws IOException, DatabaseException {
    Key key = connection.getEntities().primaryKey(TestDomain.T_DEPARTMENT, 10);
    List<Entity> depts = connection.select(singletonList(key));
    assertEquals(1, depts.size());
  }

  @Test
  void selectByKeyDifferentEntityTypes() throws IOException, DatabaseException {
    Key deptKey = connection.getEntities().primaryKey(TestDomain.T_DEPARTMENT, 10);
    Key empKey = connection.getEntities().primaryKey(TestDomain.T_EMP, 8);

    List<Entity> selected = connection.select(asList(deptKey, empKey));
    assertEquals(2, selected.size());
  }

  @Test
  void selectByValue() throws IOException, DatabaseException {
    List<Entity> department = connection.select(TestDomain.DEPARTMENT_NAME, "SALES");
    assertEquals(1, department.size());
  }

  @Test
  void update() throws IOException, DatabaseException {
    Entity department = connection.selectSingle(TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    department.put(TestDomain.DEPARTMENT_NAME, "TEstING");
    connection.update(department);
    department = connection.selectSingle(TestDomain.DEPARTMENT_ID, department.get(TestDomain.DEPARTMENT_ID));
    assertEquals("TEstING", department.get(TestDomain.DEPARTMENT_NAME));
    department.put(TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    connection.update(department);
  }

  @Test
  void updateByCondition() throws DatabaseException {
    Condition selectCondition = where(TestDomain.EMP_COMMISSION).isNull();

    List<Entity> entities = connection.select(selectCondition);

    UpdateCondition updateCondition = where(TestDomain.EMP_COMMISSION).isNull().toUpdateCondition()
            .set(TestDomain.EMP_COMMISSION, 500d)
            .set(TestDomain.EMP_SALARY, 4200d);
    connection.beginTransaction();
    try {
      connection.update(updateCondition);
      assertEquals(0, connection.rowCount(selectCondition));
      List<Entity> afterUpdate = connection.select(Entity.getPrimaryKeys(entities));
      for (final Entity entity : afterUpdate) {
        assertEquals(500d, entity.get(TestDomain.EMP_COMMISSION));
        assertEquals(4200d, entity.get(TestDomain.EMP_SALARY));
      }
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void deleteByKey() throws IOException, DatabaseException {
    Entity employee = connection.selectSingle(TestDomain.EMP_NAME, "ADAMS");
    connection.beginTransaction();
    try {
      connection.delete(employee.getPrimaryKey());
      List<Entity> selected = connection.select(singletonList(employee.getPrimaryKey()));
      assertTrue(selected.isEmpty());
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void deleteByKeyDifferentEntityTypes() throws IOException, DatabaseException {
    Key deptKey = connection.getEntities().primaryKey(TestDomain.T_DEPARTMENT, 40);
    Key empKey = connection.getEntities().primaryKey(TestDomain.T_EMP, 1);
    connection.beginTransaction();
    try {
      assertEquals(2, connection.select(asList(deptKey, empKey)).size());
      connection.delete(asList(deptKey, empKey));
      List<Entity> selected = connection.select(asList(deptKey, empKey));
      assertTrue(selected.isEmpty());
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void selectDependencies() throws IOException, DatabaseException {
    Entity department = connection.selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    Map<EntityType, Collection<Entity>> dependentEntities = connection.selectDependencies(singletonList(department));
    assertNotNull(dependentEntities);
    assertTrue(dependentEntities.containsKey(TestDomain.T_EMP));
    assertFalse(dependentEntities.get(TestDomain.T_EMP).isEmpty());
  }

  @Test
  void rowCount() throws IOException, DatabaseException {
    assertEquals(4, connection.rowCount(condition(TestDomain.T_DEPARTMENT)));
  }

  @Test
  void selectValues() throws IOException, DatabaseException {
    List<String> values = connection.select(TestDomain.DEPARTMENT_NAME);
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

    Entity scott = connection.selectSingle(TestDomain.EMP_ID, 7);
    connection.writeBlob(scott.getPrimaryKey(), TestDomain.EMP_DATA, bytes);
    assertArrayEquals(bytes, connection.readBlob(scott.getPrimaryKey(), TestDomain.EMP_DATA));
  }

  @Test
  void close() throws IOException, DatabaseException {
    connection.close();
    assertFalse(connection.isConnected());
  }

  @Test
  void deleteDepartmentWithEmployees() throws IOException, DatabaseException {
    Entity department = connection.selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(Conditions.condition(department.getPrimaryKey())));
  }

  @Test
  void foreignKeyValues() throws DatabaseException {
    Entity employee = connection.selectSingle(TestDomain.EMP_ID, 5);
    assertNotNull(employee.get(TestDomain.EMP_DEPARTMENT_FK));
    assertNotNull(employee.get(TestDomain.EMP_MGR_FK));
  }

  @Test
  void rollbackWithNoOpenTransaction() {
    assertThrows(IllegalStateException.class, connection::rollbackTransaction);
  }

  static BasicHttpClientConnectionManager createConnectionManager() {
    try {
      SSLContext sslContext = SSLContext.getDefault();

      return new BasicHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create().register("https",
              new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
              .build());
    }
    catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private static EntityServerConfiguration configure() {
    Clients.SERVER_HOST_NAME.set("localhost");
    Clients.TRUSTSTORE.set("../../framework/server/src/main/config/truststore.jks");
    Clients.TRUSTSTORE_PASSWORD.set("crappypass");
    Clients.resolveTrustStore();
    Report.REPORT_PATH.set("report/path");
    HttpServerConfiguration.HTTP_SERVER_PORT.set(WEB_SERVER_PORT_NUMBER);
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PATH.set("../../framework/server/src/main/config/keystore.jks");
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PASSWORD.set("crappypass");
    HttpServerConfiguration.HTTP_SERVER_SECURE.set(ServerHttps.TRUE);
    HttpEntityConnectionProvider.HTTP_CLIENT_SECURE.set(ClientHttps.TRUE);
    HttpEntityConnectionProvider.HTTP_CLIENT_PORT.set(WEB_SERVER_PORT_NUMBER);
    System.setProperty("java.security.policy", "../../framework/server/src/main/config/all_permissions.policy");

    return EntityServerConfiguration.builder(3223, 3221)
            .adminPort(3223)
            .database(DatabaseFactory.getDatabase())
            .domainModelClassNames(singletonList(TestDomain.class.getName()))
            .sslEnabled(false)
            .auxiliaryServerFactoryClassNames(singletonList(EntityServletServerFactory.class.getName()))
            .build();
  }

  private static void deconfigure() {
    Clients.SERVER_HOST_NAME.set(null);
    Clients.TRUSTSTORE.set(null);
    Clients.TRUSTSTORE_PASSWORD.set(null);
    System.clearProperty(Clients.JAVAX_NET_TRUSTSTORE);
    System.clearProperty(Clients.JAVAX_NET_TRUSTSTORE_PASSWORD);
    Report.REPORT_PATH.set(null);
    HttpServerConfiguration.HTTP_SERVER_PORT.set(null);
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PATH.set(null);
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PASSWORD.set(null);
    HttpServerConfiguration.HTTP_SERVER_SECURE.set(ServerHttps.FALSE);
    HttpEntityConnectionProvider.HTTP_CLIENT_PORT.set(null);
    HttpEntityConnectionProvider.HTTP_CLIENT_SECURE.set(ClientHttps.FALSE);
    System.clearProperty("java.security.policy");
  }
}
