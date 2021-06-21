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

  static {
    System.setProperty("javax.net.ssl.trustStore", "../../framework/server/src/main/security/truststore.jks");
	  System.setProperty("javax.net.ssl.trustStorePassword", "crappypass");
  }

  private final HttpServerConfiguration configuration;
  private final HttpServer httpServer;

  public HttpServerTest() {
    configuration = HttpServerConfiguration.configuration(FILE_SERVER_PORT_NUMBER, ServerHttps.TRUE);
    configuration.setDocumentRoot(System.getProperty("user.dir"));
    configuration.setKeystore("../../framework/server/src/main/security/keystore.jks", "crappypass");
    httpServer = new HttpServer(configuration);
  }

  @BeforeEach
  void setUp() throws Exception {
    httpServer.startServer();
  }

  @AfterEach
  void tearDown() throws Exception {
    httpServer.stopServer();
  }

  @Test
  void testWebServer() throws Exception {
    try (final InputStream input = new URL("https://localhost:" + FILE_SERVER_PORT_NUMBER + "/build.gradle").openStream()) {
      assertTrue(input.read() > 0);
    }
  }
}
