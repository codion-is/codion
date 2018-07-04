/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.http;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.reports.ReportDataWrapper;
import org.jminor.common.db.reports.ReportException;
import org.jminor.common.db.reports.ReportResult;
import org.jminor.common.db.reports.ReportWrapper;
import org.jminor.common.server.Server;
import org.jminor.common.server.http.HttpServer;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.server.DefaultEntityConnectionServer;
import org.jminor.framework.servlet.EntityServletServer;

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
import java.io.Serializable;
import java.rmi.registry.Registry;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public final class HttpEntityConnectionTest {

  private static final Integer WEB_SERVER_PORT_NUMBER = 8089;
  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private static DefaultEntityConnectionServer server;

  private final HttpEntityConnection connection = new HttpEntityConnection("TestDomain",
          HttpEntityConnectionProvider.HTTP_CLIENT_HOST_NAME.get(),
          HttpEntityConnectionProvider.HTTP_CLIENT_PORT.get(),
          HttpEntityConnectionProvider.HTTP_CLIENT_SECURE.get(),
          UNIT_TEST_USER, "HttpEntityConnectionTest", UUID.randomUUID(),
          createConnectionManager());

  private final EntityConditions conditions = new EntityConditions(connection.getDomain());

  @BeforeAll
  public static void setUp() throws Exception {
    configure();
    server = DefaultEntityConnectionServer.startServer();
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
    final TestReportResult result = (TestReportResult) connection.fillReport(new TestReportWrapper());
    assertNotNull(result);
  }

  @Test
  public void insert() throws IOException, DatabaseException {
    final Entity entity = connection.getDomain().entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 33);
    entity.put(TestDomain.DEPARTMENT_NAME, "name");
    entity.put(TestDomain.DEPARTMENT_LOCATION, "loc");
    final List<Entity.Key> keys = connection.insert(Collections.singletonList(entity));
    assertEquals(1, keys.size());
    assertEquals(33, keys.get(0).getFirstValue());
    connection.delete(keys);
  }

  @Test
  public void selectByKey() throws IOException, DatabaseException {
    final Entity.Key key = connection.getDomain().key(TestDomain.T_DEPARTMENT);
    key.put(TestDomain.DEPARTMENT_ID, 10);
    final List<Entity> depts = connection.selectMany(Collections.singletonList(key));
    assertEquals(1, depts.size());
  }

  @Test
  public void selectManyByValue() throws IOException, DatabaseException {
    final List<Entity> department = connection.selectMany(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    assertEquals(1, department.size());
  }

  @Test
  public void update() throws IOException, DatabaseException {
    Entity department = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    department.put(TestDomain.DEPARTMENT_NAME, "TEstING");
    connection.update(Collections.singletonList(department));
    department = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID, department.get(TestDomain.DEPARTMENT_ID));
    assertEquals("TEstING", department.getString(TestDomain.DEPARTMENT_NAME));
  }

  @Test
  public void deleteByKey() throws IOException, DatabaseException {
    final Entity employee = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "ADAMS");
    connection.delete(Collections.singletonList(employee.getKey()));
    final List<Entity> selected = connection.selectMany(Collections.singletonList(employee.getKey()));
    assertTrue(selected.isEmpty());
  }

  @Test
  public void selectDependentEntities() throws IOException, DatabaseException {
    final Entity department = connection.selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    final Map<String, Collection<Entity>> dependentEntities = connection.selectDependentEntities(Collections
            .singletonList(department));
    assertNotNull(dependentEntities);
    assertTrue(dependentEntities.containsKey(TestDomain.T_EMP));
    assertFalse(dependentEntities.get(TestDomain.T_EMP).isEmpty());
  }

  @Test
  public void selectRowCount() throws IOException, DatabaseException {
    assertEquals(4, connection.selectRowCount(conditions.condition(TestDomain.T_DEPARTMENT)));
  }

  @Test
  public void selectValues() throws IOException, DatabaseException {
    final List<Object> values = connection.selectValues(TestDomain.DEPARTMENT_NAME, conditions.condition(TestDomain.T_DEPARTMENT));
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
  public void disconnect() throws IOException, DatabaseException {
    connection.disconnect();
    assertFalse(connection.isConnected());
  }

  @Test
  public void deleteDepartmentWithEmployees() throws IOException, DatabaseException {
    final Entity department = connection.selectSingle(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_NAME, "SALES");
    assertThrows(DatabaseException.class, () -> connection.delete(conditions.condition(department.getKey())));
  }

  @Test
  public void rollbackWithNoOpenTransaction() {
    assertThrows(IllegalStateException.class, connection::rollbackTransaction);
  }

  private static void configure() {
    Server.REGISTRY_PORT.set(2221);
    Server.SERVER_CONNECTION_SSL_ENABLED.set(false);
    Server.SERVER_PORT.set(2223);
    Server.SERVER_ADMIN_PORT.set(2223);
    Server.SERVER_HOST_NAME.set("localhost");
    HttpServer.HTTP_SERVER_PORT.set(WEB_SERVER_PORT_NUMBER);
    HttpEntityConnectionProvider.HTTP_CLIENT_PORT.set(WEB_SERVER_PORT_NUMBER);
    System.setProperty("java.security.policy", "resources/security/all_permissions.policy");
    DefaultEntityConnectionServer.SERVER_DOMAIN_MODEL_CLASSES.set(TestDomain.class.getName());
    Server.AUXILIARY_SERVER_CLASS_NAMES.set(EntityServletServer.class.getName());
    HttpServer.HTTP_SERVER_KEYSTORE_PATH.set("../../resources/security/JMinorServerKeystore");
    Server.TRUSTSTORE.set("../../resources/security/JMinorClientTruststore");
    HttpServer.HTTP_SERVER_KEYSTORE_PASSWORD.set("crappypass");
    HttpServer.HTTP_SERVER_SECURE.set(true);
    HttpEntityConnectionProvider.HTTP_CLIENT_SECURE.set(true);
  }

  private static void deconfigure() {
    Server.REGISTRY_PORT.set(Registry.REGISTRY_PORT);
    Server.SERVER_CONNECTION_SSL_ENABLED.set(true);
    Server.SERVER_PORT.set(null);
    Server.SERVER_ADMIN_PORT.set(null);
    Server.SERVER_HOST_NAME.set(null);
    HttpServer.HTTP_SERVER_PORT.set(null);
    HttpEntityConnectionProvider.HTTP_CLIENT_PORT.set(null);
    System.clearProperty("java.security.policy");
    DefaultEntityConnectionServer.SERVER_DOMAIN_MODEL_CLASSES.set(null);
    Server.AUXILIARY_SERVER_CLASS_NAMES.set(null);
    HttpServer.HTTP_SERVER_KEYSTORE_PATH.set(null);
    Server.TRUSTSTORE.set(null);
    HttpServer.HTTP_SERVER_KEYSTORE_PASSWORD.set(null);
    HttpServer.HTTP_SERVER_SECURE.set(false);
    HttpEntityConnectionProvider.HTTP_CLIENT_SECURE.set(false);
  }

  private static class TestReportWrapper implements ReportWrapper, Serializable {

    @Override
    public String getReportName() {
      return "testReportName";
    }

    @Override
    public ReportResult fillReport(final Connection connection) throws ReportException {
      return new TestReportResult();
    }

    @Override
    public ReportResult fillReport(final ReportDataWrapper reportDataWrapper) throws ReportException {
      return new TestReportResult();
    }
  }

  private static class TestReportResult implements ReportResult, Serializable {

    @Override
    public Object getResult() {
      return "ReportResult";
    }
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
