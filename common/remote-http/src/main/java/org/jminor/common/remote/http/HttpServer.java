/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.http;

import org.jminor.common.Configuration;
import org.jminor.common.PropertyValue;
import org.jminor.common.Util;
import org.jminor.common.remote.Server;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A simple Jetty based http file server
 */
public class HttpServer extends org.eclipse.jetty.server.Server implements Server.AuxiliaryServer {

  private static final Logger LOG = LoggerFactory.getLogger(HttpServer.class);

  /**
   * The port on which the http server is made available to clients.<br>
   * Value type: Integer<br>
   * Default value: 8080
   */
  public static final PropertyValue<Integer> HTTP_SERVER_PORT = Configuration.integerValue("jminor.server.http.port", 8080);

  /**
   * Specifies whether https should be used.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> HTTP_SERVER_SECURE = Configuration.booleanValue("jminor.server.http.secure", true);

  /**
   * Specifies the keystore to use for securing http connections.<br>
   * Value type: String<br>
   * Default value: null
   */
  public static final PropertyValue<String> HTTP_SERVER_KEYSTORE_PATH = Configuration.stringValue("jminor.server.http.keyStore", null);


  /**
   * Specifies the password for the keystore used for securing http connections.<br>
   * Value type: String<br>
   * Default value: null
   */
  public static final PropertyValue<String> HTTP_SERVER_KEYSTORE_PASSWORD = Configuration.stringValue("jminor.server.http.keyStorePassword", null);

  /**
   * Specifies the document root for file serving<br>.
   * Value type: String<br>
   * Default value: null
   */
  public static final PropertyValue<String> DOCUMENT_ROOT = Configuration.stringValue("jminor.server.http.documentRoot", null);

  private final Server connectionServer;
  private final HandlerList handlers;
  private final int port;

  /**
   * Instantiates a new HttpServer on the given port.
   * @param connectionServer the Server serving the connection requests
   */
  public HttpServer(final Server connectionServer) {
    this(connectionServer, DOCUMENT_ROOT.get(), HTTP_SERVER_PORT.get(), HTTP_SERVER_SECURE.get());
  }

  /**
   * Instantiates a new HttpServer on the given port.
   * @param connectionServer the Server serving the connection requests
   * @param documentRoot the document root, null to disable file serving
   * @param port the port on which to serve
   * @param useHttps true if https should be used
   */
  public HttpServer(final Server connectionServer, final String documentRoot, final Integer port,
                    final Boolean useHttps) {
    super(Objects.requireNonNull(port, "port"));
    this.port = port;
    if (Objects.requireNonNull(useHttps, "useHttps")) {
      setupSecureConnector();
    }
    LOG.info(getClass().getSimpleName() + " created on port: " + port);
    this.connectionServer = connectionServer;
    this.handlers = new HandlerList();
    setHandler(handlers);
    if (!Util.nullOrEmpty(documentRoot)) {
      LOG.info("HttpServer serving files from: " + documentRoot);
      final ResourceHandler fileHandler = new ResourceHandler();
      fileHandler.setResourceBase(documentRoot);
      addHandler(fileHandler);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void startServer() throws Exception {
    start();
    LOG.info(getClass().getSimpleName() + " started");
  }

  /** {@inheritDoc} */
  @Override
  public void stopServer() throws Exception {
    stop();
    join();
    LOG.info(getClass().getSimpleName() + " stopped");
  }

  /**
   * Adds a handler to this http server
   * @param handler the handler to add
   */
  protected final void addHandler(final Handler handler) {
    LOG.info(getClass().getSimpleName() + " adding handler: " + handler);
    handlers.addHandler(handler);
  }

  /**
   * @return the {@link Server} this http server is running alongside
   */
  protected final Server getConnectionServer() {
    return connectionServer;
  }

  private void setupSecureConnector() {
    final HttpConfiguration httpConfiguration = new HttpConfiguration();
    httpConfiguration.setSecureScheme("https");
    httpConfiguration.setSecurePort(port);

    final HttpConfiguration httpsConfig = new HttpConfiguration(httpConfiguration);
    httpsConfig.addCustomizer(new SecureRequestCustomizer());

    final String keystore = HTTP_SERVER_KEYSTORE_PATH.get();
    Objects.requireNonNull(keystore, HTTP_SERVER_KEYSTORE_PATH.toString());

    final String keystorePassword = HTTP_SERVER_KEYSTORE_PASSWORD.get();
    Objects.requireNonNull(keystorePassword, HTTP_SERVER_KEYSTORE_PASSWORD.toString());

    final SslContextFactory sslContextFactory = new SslContextFactory(keystore);
    sslContextFactory.setKeyStorePassword(keystorePassword);

    final ServerConnector httpsConnector = new ServerConnector(this,
            new SslConnectionFactory(sslContextFactory, "http/1.1"),
            new HttpConnectionFactory(httpsConfig));
    httpsConnector.setPort(port);
    httpsConnector.setIdleTimeout(50000);

    setConnectors(new Connector[] {httpsConnector});
  }
}
