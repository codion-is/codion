/*
 * Copyright (c) 2017 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.http.server;

import is.codion.common.event.Event;
import is.codion.common.event.EventListener;

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

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

/**
 * A Jetty based http file server
 */
public class HttpServer extends org.eclipse.jetty.server.Server {

  private static final Logger LOG = LoggerFactory.getLogger(HttpServer.class);

  private final Event<?> serverStartedEvent = Event.event();
  private final Event<?> serverStoppedEvent = Event.event();

  private final HandlerList handlers;
  private final int port;

  /**
   * Instantiates a new HttpServer, using system properties.
   */
  public HttpServer() {
    this(HttpServerConfiguration.builderFromSystemProperties().build());
  }

  /**
   * Instantiates a new HttpServer.
   * @param configuration the server configuration.
   */
  public HttpServer(HttpServerConfiguration configuration) {
    super(requireNonNull(configuration, "configuration").serverPort());
    this.port = configuration.serverPort();
    if (configuration.secure()) {
      setupSecureConnector(configuration);
    }
    LOG.info(getClass().getSimpleName() + " created on port: " + port);
    this.handlers = new HandlerList();
    setHandler(handlers);
    if (!nullOrEmpty(configuration.documentRoot())) {
      LOG.info("HttpServer serving files from: " + configuration.documentRoot());
      ResourceHandler fileHandler = new ResourceHandler();
      fileHandler.setResourceBase(configuration.documentRoot());
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
  public final void addServerStartedListener(EventListener listener) {
    serverStartedEvent.addListener(listener);
  }

  /**
   * Adds a shutdown listener.
   * @param listener a listener notified when this server is stopped.
   */
  public final void addServerStoppedListener(EventListener listener) {
    serverStoppedEvent.addListener(listener);
  }

  @Override
  public final void setHandler(Handler handler) {
    super.setHandler(handler);
  }

  /**
   * Adds a handler to this http server
   * @param handler the handler to add
   */
  protected final void addHandler(Handler handler) {
    LOG.info(getClass().getSimpleName() + " adding handler: " + handler);
    handlers.addHandler(handler);
  }

  private void setupSecureConnector(HttpServerConfiguration configuration) {
    HttpConfiguration httpConfiguration = new HttpConfiguration();
    httpConfiguration.setSecureScheme("https");
    httpConfiguration.setSecurePort(port);

    HttpConfiguration httpsConfig = new HttpConfiguration(httpConfiguration);
    httpsConfig.addCustomizer(new SecureRequestCustomizer());

    requireNonNull(configuration.keystorePath(), HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PATH.toString());

    requireNonNull(configuration.keystorePassword(), HttpServerConfiguration.HTTP_SERVER_KEYSTORE_PASSWORD.toString());

    SslContextFactory sslContextFactory = new SslContextFactory(configuration.keystorePath());
    sslContextFactory.setKeyStorePassword(configuration.keystorePassword());

    ServerConnector httpsConnector = new ServerConnector(this,
            new SslConnectionFactory(sslContextFactory, "http/1.1"),
            new HttpConnectionFactory(httpsConfig));
    httpsConnector.setPort(port);
    httpsConnector.setIdleTimeout(50000);

    setConnectors(new Connector[] {httpsConnector});
  }
}
