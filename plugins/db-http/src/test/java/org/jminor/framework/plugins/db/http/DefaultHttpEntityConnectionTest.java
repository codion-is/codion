/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.db.http;

import org.jminor.common.User;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.server.Server;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.plugins.jetty.JettyServer;
import org.jminor.framework.plugins.rest.EntityRESTServer;
import org.jminor.framework.server.DefaultEntityConnectionServer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.rmi.registry.Registry;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class DefaultHttpEntityConnectionTest {

  private static final Integer REST_SERVER_PORT_NUMBER = 8089;
  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger"));

  private static final Entities ENTITIES = new TestDomain();
  private static DefaultEntityConnectionServer server;

  private final DefaultHttpEntityConnection connection = new DefaultHttpEntityConnection(ENTITIES, UNIT_TEST_USER);

  @BeforeClass
  public static void setUp() throws Exception {
    configure();
    server = DefaultEntityConnectionServer.startServer();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    server.shutdown();
    deconfigure();
  }

  @Test
  public void insert() throws IOException, DatabaseException {
    final Entity entity = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 33);
    entity.put(TestDomain.DEPARTMENT_NAME, "name");
    entity.put(TestDomain.DEPARTMENT_LOCATION, "loc");
    final List<Entity.Key> keys = connection.insert(Collections.singletonList(entity));
    assertEquals(1, keys.size());
    assertEquals(33, keys.get(0).getFirstValue());
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
  public void delete() throws IOException, DatabaseException {
    final Entity employee = connection.selectSingle(TestDomain.T_EMP, TestDomain.EMP_NAME, "ADAMS");
    connection.delete(Collections.singletonList(employee.getKey()));
    final List<Entity> selected = connection.selectMany(Collections.singletonList(employee.getKey()));
    assertTrue(selected.isEmpty());
  }

  private static void configure() {
    Server.REGISTRY_PORT.set(2221);
    Server.SERVER_CONNECTION_SSL_ENABLED.set(false);
    Server.SERVER_PORT.set(2223);
    Server.SERVER_ADMIN_PORT.set(2223);
    Server.SERVER_HOST_NAME.set("localhost");
    HttpEntityConnection.WEB_SERVER_PORT.set(REST_SERVER_PORT_NUMBER);
    System.setProperty("java.security.policy", "resources/security/all_permissions.policy");
    DefaultEntityConnectionServer.SERVER_DOMAIN_MODEL_CLASSES.set(TestDomain.class.getName());
    Server.AUXILIARY_SERVER_CLASS_NAMES.set(EntityRESTServer.class.getName());
    JettyServer.WEB_SERVER_PORT.set(REST_SERVER_PORT_NUMBER);
  }

  private static void deconfigure() {
    Server.REGISTRY_PORT.set(Registry.REGISTRY_PORT);
    Server.SERVER_CONNECTION_SSL_ENABLED.set(true);
    Server.SERVER_PORT.set(null);
    Server.SERVER_ADMIN_PORT.set(null);
    Server.SERVER_HOST_NAME.set(null);
    HttpEntityConnection.WEB_SERVER_PORT.set(null);
    System.clearProperty("java.security.policy");
    DefaultEntityConnectionServer.SERVER_DOMAIN_MODEL_CLASSES.set(null);
    Server.AUXILIARY_SERVER_CLASS_NAMES.set(null);
    JettyServer.WEB_SERVER_PORT.set(null);
  }
}
