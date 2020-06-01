/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.http;

import is.codion.common.db.Operator;
import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.db.reports.ReportException;
import is.codion.common.db.reports.ReportWrapper;
import is.codion.common.http.server.HttpServerConfiguration;
import is.codion.common.http.server.ServerHttps;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.condition.EntitySelectCondition;
import is.codion.framework.db.condition.EntityUpdateCondition;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.identity.Identity;
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
import java.util.UUID;

import static is.codion.framework.db.condition.Conditions.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class HttpEntityConnectionTest {

  private static final Integer WEB_SERVER_PORT_NUMBER = 8089;
  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static EntityServer server;

  private final HttpEntityConnection connection = new HttpEntityConnection(Entities.domainIdentity("TestDomain"),
          HttpEntityConnectionProvider.HTTP_CLIENT_HOST_NAME.get(),
          HttpEntityConnectionProvider.HTTP_CLIENT_PORT.get(),
          HttpEntityConnectionProvider.HTTP_CLIENT_SECURE.get(),
          UNIT_TEST_USER, "HttpEntityConnectionTest", UUID.randomUUID(),
          createConnectionManager());

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
  public void executeProcedure() throws IOException, DatabaseException {
    connection.executeProcedure(TestDomain.PROCEDURE_ID);
  }

  @Test
  public void executeFunction() throws IOException, DatabaseException {
    assertNotNull(connection.executeFunction(TestDomain.FUNCTION_ID));
  }

  @Test
  public void fillReport() throws ReportException, DatabaseException, IOException {
    final String result = connection.fillReport(TestDomain.REPORT, "");
    assertNotNull(result);
  }

  @Test
  public void insert() throws IOException, DatabaseException {
    final Entity entity = connection.getEntities().entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 33);
    entity.put(TestDomain.DEPARTMENT_NAME, "name");
    entity.put(TestDomain.DEPARTMENT_LOCATION, "loc");
    final Entity.Key key = connection.insert(entity);
    assertEquals(33, key.getFirstValue());
    connection.delete(key);
  }

  @Test
  public void selectByKey() throws IOException, DatabaseException {
    final Entity.Key key = connection.getEntities().key(TestDomain.T_DEPARTMENT, 10);
    final List<Entity> depts = connection.select(singletonList(key));
    assertEquals(1, depts.size());
  }

  @Test
  public void selectByKeyDifferentEntityIds() throws IOException, DatabaseException {
    final Entity.Key deptKey = connection.getEntities().key(TestDomain.T_DEPARTMENT, 10);
    final Entity.Key empKey = connection.getEntities().key(TestDomain.T_EMP, 8);

    final List<Entity> selected = connection.select(asList(deptKey, empKey));
    assertEquals(2, selected.size());
  }

  @Test
  public void selectByValue() throws IOException, DatabaseException {
    final List<Entity> department = connection.select(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    assertEquals(1, department.size());
  }

  @Test
  public void update() throws IOException, DatabaseException {
    Entity department = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    department.put(TestDomain.DEPARTMENT_NAME, "TEstING");
    connection.update(department);
    department = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID, department.get(TestDomain.DEPARTMENT_ID));
    assertEquals("TEstING", department.get(TestDomain.DEPARTMENT_NAME));
  }

  @Test
  public void updateByCondition() throws DatabaseException {
    final EntitySelectCondition selectCondition = selectCondition(TestDomain.T_EMP,
            TestDomain.EMP_COMMISSION, Operator.LIKE, null);

    final List<Entity> entities = connection.select(selectCondition);

    final EntityUpdateCondition updateCondition = updateCondition(TestDomain.T_EMP,
            TestDomain.EMP_COMMISSION, Operator.LIKE, null)
            .set(TestDomain.EMP_COMMISSION, 500d)
            .set(TestDomain.EMP_SALARY, 4200d);
    try {
      connection.beginTransaction();
      connection.update(updateCondition);
      assertEquals(0, connection.selectRowCount(selectCondition));
      final List<Entity> afterUpdate = connection.select(Entities.getKeys(entities));
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
  public void deleteByKey() throws IOException, DatabaseException {
    final Entity employee = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "ADAMS");
    try {
      connection.beginTransaction();
      assertTrue(connection.delete(employee.getKey()));
      final List<Entity> selected = connection.select(singletonList(employee.getKey()));
      assertTrue(selected.isEmpty());
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void deleteByKeyDifferentEntityIds() throws IOException, DatabaseException {
    final Entity.Key deptKey = connection.getEntities().key(TestDomain.T_DEPARTMENT, 40);
    final Entity.Key empKey = connection.getEntities().key(TestDomain.T_EMP, 1);
    try {
      connection.beginTransaction();
      assertEquals(2, connection.select(asList(deptKey, empKey)).size());
      assertEquals(2, connection.delete(asList(deptKey, empKey)));
      final List<Entity> selected = connection.select(asList(deptKey, empKey));
      assertTrue(selected.isEmpty());
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void selectDependencies() throws IOException, DatabaseException {
    final Entity department = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    final Map<Identity, Collection<Entity>> dependentEntities = connection.selectDependencies(singletonList(department));
    assertNotNull(dependentEntities);
    assertTrue(dependentEntities.containsKey(TestDomain.T_EMP));
    assertFalse(dependentEntities.get(TestDomain.T_EMP).isEmpty());
  }

  @Test
  public void selectRowCount() throws IOException, DatabaseException {
    assertEquals(4, connection.selectRowCount(condition(TestDomain.T_DEPARTMENT)));
  }

  @Test
  public void selectValues() throws IOException, DatabaseException {
    final List<String> values = connection.selectValues(TestDomain.DEPARTMENT_NAME, condition(TestDomain.T_DEPARTMENT));
    assertEquals(4, values.size());
  }

  @Test
  public void transactions() throws IOException, DatabaseException {
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
  public void writeReadBlob() throws DatabaseException {
    final byte[] bytes = new byte[1024];
    new Random().nextBytes(bytes);

    final Entity scott = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_ID, 7);
    connection.writeBlob(scott.getKey(), TestDomain.EMP_DATA, bytes);
    assertArrayEquals(bytes, connection.readBlob(scott.getKey(), TestDomain.EMP_DATA));
  }

  @Test
  public void disconnect() throws IOException, DatabaseException {
    connection.disconnect();
    assertFalse(connection.isConnected());
  }

  @Test
  public void deleteDepartmentWithEmployees() throws IOException, DatabaseException {
    final Entity department = connection.selectSingle(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_NAME, "SALES");
    assertThrows(ReferentialIntegrityException.class, () -> connection.delete(condition(department.getKey())));
  }

  @Test
  public void rollbackWithNoOpenTransaction() {
    assertThrows(IllegalStateException.class, connection::rollbackTransaction);
  }

  private static EntityServerConfiguration configure() {
    ServerConfiguration.SERVER_HOST_NAME.set("localhost");
    ServerConfiguration.TRUSTSTORE.set("../../framework/server/src/main/security/truststore.jks");
    ServerConfiguration.TRUSTSTORE_PASSWORD.set("crappypass");
    ReportWrapper.REPORT_PATH.set("report/path");
    HttpServerConfiguration.HTTP_SERVER_PORT.set(WEB_SERVER_PORT_NUMBER);
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PATH.set("../../framework/server/src/main/security/keystore.jks");
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PASSWORD.set("crappypass");
    HttpServerConfiguration.HTTP_SERVER_SECURE.set(ServerHttps.TRUE);
    HttpEntityConnectionProvider.HTTP_CLIENT_SECURE.set(ClientHttps.TRUE);
    HttpEntityConnectionProvider.HTTP_CLIENT_PORT.set(WEB_SERVER_PORT_NUMBER);
    System.setProperty("java.security.policy", "../../framework/server/src/main/security/all_permissions.policy");
    final EntityServerConfiguration configuration = EntityServerConfiguration.configuration(2223, 2221);
    configuration.setAdminPort(2223);
    configuration.setDatabase(Databases.getInstance());
    configuration.setDomainModelClassNames(singletonList(TestDomain.class.getName()));
    configuration.setSslEnabled(false);
    configuration.setAuxiliaryServerFactoryClassNames(singletonList(EntityServletServerFactory.class.getName()));

    return configuration;
  }

  private static void deconfigure() {
    ServerConfiguration.SERVER_HOST_NAME.set(null);
    ServerConfiguration.TRUSTSTORE.set(null);
    ServerConfiguration.TRUSTSTORE_PASSWORD.set(null);
    ReportWrapper.REPORT_PATH.set(null);
    HttpServerConfiguration.HTTP_SERVER_PORT.set(null);
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PATH.set(null);
    HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PASSWORD.set(null);
    HttpServerConfiguration.HTTP_SERVER_SECURE.set(ServerHttps.FALSE);
    HttpEntityConnectionProvider.HTTP_CLIENT_PORT.set(null);
    HttpEntityConnectionProvider.HTTP_CLIENT_SECURE.set(ClientHttps.FALSE);
    System.clearProperty("java.security.policy");
  }

  private static BasicHttpClientConnectionManager createConnectionManager() {
    try {
      final SSLContext sslContext = SSLContext.getDefault();

      return new BasicHttpClientConnectionManager(RegistryBuilder.<ConnectionSocketFactory>create().register("https",
              new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
              .build());
    }
    catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
