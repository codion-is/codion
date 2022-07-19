/*
 * Copyright (c) 2017 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
    System.setProperty("javax.net.ssl.trustStore", "../../framework/server/src/main/config/truststore.jks");
    System.setProperty("javax.net.ssl.trustStorePassword", "crappypass");
  }

  private final HttpServerConfiguration configuration;
  private final HttpServer httpServer;

  public HttpServerTest() {
    configuration = HttpServerConfiguration.builder(FILE_SERVER_PORT_NUMBER)
            .secure(true)
            .documentRoot(System.getProperty("user.dir"))
            .keystorePath("../../framework/server/src/main/config/keystore.jks")
            .keystorePassword("crappypass")
            .build();
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
    try (InputStream input = new URL("https://localhost:" + FILE_SERVER_PORT_NUMBER + "/build.gradle").openStream()) {
      assertTrue(input.read() > 0);
    }
  }
}
