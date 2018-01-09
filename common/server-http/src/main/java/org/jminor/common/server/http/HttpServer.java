/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.server.http;

import org.jminor.common.Configuration;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.server.Server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public static final Value<Integer> HTTP_SERVER_PORT = Configuration.integerValue("jminor.server.http.port", 8080);

  /**
   * Specifies the document root for file serving<br>.
   * Value type: String<br>
   * Default value: null
   */
  public static final Value<String> DOCUMENT_ROOT = Configuration.stringValue("jminor.server.http.documentRoot", null);

  private final Server connectionServer;
  private final HandlerList handlers;

  /**
   * Instantiates a new HttpServer on the given port.
   * @param connectionServer the Server serving the connection requests
   */
  public HttpServer(final Server connectionServer) {
    this(connectionServer, DOCUMENT_ROOT.get(), HTTP_SERVER_PORT.get());
  }

  /**
   * Instantiates a new HttpServer on the given port.
   * @param connectionServer the Server serving the connection requests
   * @param documentRoot the document root, null to disable file serving
   * @param port the port on which to serve
   */
  public HttpServer(final Server connectionServer, final String documentRoot, final Integer port) {
    super(port);
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
}
