/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.http.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpServerTest {

  private static final int FILE_SERVER_PORT_NUMBER = 8089;

  private final HttpServerConfiguration configuration = HttpServerConfiguration.configuration(FILE_SERVER_PORT_NUMBER, ServerHttps.FALSE);
  private final HttpServer httpServer;

  public HttpServerTest() {
    configuration.setDocumentRoot(System.getProperty("user.dir"));
    httpServer = new HttpServer(configuration);
  }

  @BeforeEach
  public void setUp() throws Exception {
    httpServer.startServer();
  }

  @AfterEach
  public void tearDown() throws Exception {
    httpServer.stopServer();
  }

  @Test
  public void testWebServer() throws Exception {
    try (final InputStream input = new URL("http://localhost:" + FILE_SERVER_PORT_NUMBER + "/build.gradle").openStream()) {
      assertTrue(input.read() > 0);
    }
  }
}
