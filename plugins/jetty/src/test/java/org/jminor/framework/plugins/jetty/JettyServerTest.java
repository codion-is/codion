/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jetty;

import org.jminor.common.server.Server;
import org.jminor.framework.server.DefaultEntityConnectionServer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;
import java.rmi.registry.Registry;

import static org.junit.Assert.assertTrue;

public class JettyServerTest {

  private static final int FILE_SERVER_PORT_NUMBER = 8089;

  private static DefaultEntityConnectionServer server;

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
  public void testWebServer() throws Exception {
    try (final InputStream input = new URL("http://localhost:" + FILE_SERVER_PORT_NUMBER + "/ivy.xml").openStream()) {
      assertTrue(input.read() > 0);
    }
  }

  private static void configure() {
    Server.REGISTRY_PORT.set(2221);
    Server.SERVER_CONNECTION_SSL_ENABLED.set(false);
    Server.SERVER_PORT.set(2223);
    Server.SERVER_ADMIN_PORT.set(2223);
    Server.SERVER_ADMIN_USER.set("scott:tiger");
    Server.SERVER_HOST_NAME.set("localhost");
    Server.RMI_SERVER_HOSTNAME.set("localhost");
    System.setProperty("java.security.policy", "resources/security/all_permissions.policy");
    Server.AUXILIARY_SERVER_CLASS_NAMES.set(JettyServer.class.getName());
    JettyServer.DOCUMENT_ROOT.set(System.getProperty("user.dir"));
    Server.WEB_SERVER_PORT.set(FILE_SERVER_PORT_NUMBER);
  }

  private static void deconfigure() {
    Server.REGISTRY_PORT.set(Registry.REGISTRY_PORT);
    Server.SERVER_CONNECTION_SSL_ENABLED.set(true);
    Server.SERVER_PORT.set(null);
    Server.SERVER_ADMIN_PORT.set(null);
    Server.SERVER_ADMIN_USER.set(null);
    Server.SERVER_HOST_NAME.set(null);
    Server.RMI_SERVER_HOSTNAME.set(null);
    System.clearProperty("java.security.policy");
    Server.AUXILIARY_SERVER_CLASS_NAMES.set(null);
    JettyServer.DOCUMENT_ROOT.set(null);
    Server.WEB_SERVER_PORT.set(null);
  }
}
