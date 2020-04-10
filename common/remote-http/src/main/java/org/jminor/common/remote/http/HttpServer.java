/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.remote.http;

import org.jminor.common.Configuration;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.Events;
import org.jminor.common.value.PropertyValue;

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

import static java.util.Objects.requireNonNull;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * A simple Jetty based http file server
 */
public class HttpServer extends org.eclipse.jetty.server.Server {

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

  private final Event serverStartedEvent = Events.event();
  private final Event serverStoppedEvent = Events.event();

  private final HandlerList handlers;
  private final int port;

  /**
   * Instantiates a new HttpServer on the given port.
   */
  public HttpServer() {
    this(DOCUMENT_ROOT.get(), HTTP_SERVER_PORT.get(), HTTP_SERVER_SECURE.get());
  }

  /**
   * Instantiates a new HttpServer on the given port.
   * @param documentRoot the document root, null to disable file serving
   * @param port the port on which to serve
   * @param useHttps true if https should be used
   */
  public HttpServer(final String documentRoot, final Integer port, final Boolean useHttps) {
    super(requireNonNull(port, "port"));
    this.port = port;
    if (requireNonNull(useHttps, "useHttps")) {
      setupSecureConnector();
    }
    LOG.info(getClass().getSimpleName() + " created on port: " + port);
    this.handlers = new HandlerList();
    setHandler(handlers);
    if (!nullOrEmpty(documentRoot)) {
      LOG.info("HttpServer serving files from: " + documentRoot);
      final ResourceHandler fileHandler = new ResourceHandler();
      fileHandler.setResourceBase(documentRoot);
      addHandler(fileHandler);
    }
  }

  /**
   * Starts this server.
   * @throws Exception in case of an exception
   */
  public final void startServer() throws Exception {
    start();
    serverStartedEvent.onEvent();
    LOG.info(getClass().getSimpleName() + " started");
  }

  /**
   * Stops this server.
   * @throws Exception in case of an exception
   */
  public final void stopServer() throws Exception {
    stop();
    join();
    serverStoppedEvent.onEvent();
    LOG.info(getClass().getSimpleName() + " stopped");
  }

  /**
   * Adds a startup listener.
   * @param listener a listener notified when this server is started.
   */
  public final void addServerStartedListener(final EventListener listener) {
    serverStartedEvent.addListener(listener);
  }

  /**
   * Adds a shutdown listener.
   * @param listener a listener notified when this server is stopped.
   */
  public final void addServerStoppedListener(final EventListener listener) {
    serverStoppedEvent.addListener(listener);
  }

  /**
   * Adds a handler to this http server
   * @param handler the handler to add
   */
  protected final void addHandler(final Handler handler) {
    LOG.info(getClass().getSimpleName() + " adding handler: " + handler);
    handlers.addHandler(handler);
  }

  private void setupSecureConnector() {
    final HttpConfiguration httpConfiguration = new HttpConfiguration();
    httpConfiguration.setSecureScheme("https");
    httpConfiguration.setSecurePort(port);

    final HttpConfiguration httpsConfig = new HttpConfiguration(httpConfiguration);
    httpsConfig.addCustomizer(new SecureRequestCustomizer());

    final String keystore = HTTP_SERVER_KEYSTORE_PATH.get();
    requireNonNull(keystore, HTTP_SERVER_KEYSTORE_PATH.toString());

    final String keystorePassword = HTTP_SERVER_KEYSTORE_PASSWORD.get();
    requireNonNull(keystorePassword, HTTP_SERVER_KEYSTORE_PASSWORD.toString());

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
