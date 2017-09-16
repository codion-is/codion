/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server.http;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.assertTrue;

public class HttpServerTest {

  private static final int FILE_SERVER_PORT_NUMBER = 8089;

  private final HttpServer httpServer = new HttpServer(null,
          System.getProperty("user.dir"), FILE_SERVER_PORT_NUMBER);

  @Before
  public void setUp() throws Exception {
    httpServer.startServer();
  }

  @After
  public void tearDown() throws Exception {
    httpServer.stopServer();
  }

  @Test
  public void testWebServer() throws Exception {
    try (final InputStream input = new URL("http://localhost:" + FILE_SERVER_PORT_NUMBER + "/ivy.xml").openStream()) {
      assertTrue(input.read() > 0);
    }
  }
}
