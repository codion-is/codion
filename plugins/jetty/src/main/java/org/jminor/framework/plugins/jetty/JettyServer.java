/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jetty;

import org.jminor.common.Configuration;
import org.jminor.common.Util;
import org.jminor.common.Value;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple Jetty based file server
 */
public class JettyServer extends Server implements org.jminor.common.server.Server.AuxiliaryServer {

  private static final Logger LOG = LoggerFactory.getLogger(JettyServer.class);

  /**
   * Specifies the document root for file serving<br>.
   * Value type: String<br>
   * Default value: null
   */
  public static final Value<String> DOCUMENT_ROOT = Configuration.stringValue("jminor.server.web.documentRoot", null);

  /**
   * Specifies the web server port<br>.
   * Value type: String<br>
   * Default value: 8080
   */
  public static final Value<Integer> WEB_SERVER_PORT = Configuration.integerValue("jminor.server.web.port", 8080);

  private final org.jminor.common.server.Server connectionServer;
  private final HandlerList handlers;

  /**
   * Instantiates a new JettyServer on the given port.
   * @param connectionServer the Server serving the connection requests
   */
  public JettyServer(final org.jminor.common.server.Server connectionServer) {
    this(connectionServer, DOCUMENT_ROOT.get(), WEB_SERVER_PORT.get());
  }

  /**
   * Instantiates a new JettyServer on the given port.
   * @param connectionServer the Server serving the connection requests
   * @param documentRoot the document root, null to disable file serving
   * @param port the port on which to serve
   */
  public JettyServer(final org.jminor.common.server.Server connectionServer, final String documentRoot, final Integer port) {
    super(port);
    LOG.info(getClass().getSimpleName() + " created on port: " + port);
    this.connectionServer = connectionServer;
    this.handlers = new HandlerList();
    setHandler(handlers);
    if (!Util.nullOrEmpty(documentRoot)) {
      LOG.info("JettyServer serving files from: " + documentRoot);
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
   * Adds a handler to this Jetty server
   * @param handler the handler to add
   */
  protected final void addHandler(final Handler handler) {
    LOG.info(getClass().getSimpleName() + " adding handler: " + handler);
    handlers.addHandler(handler);
  }

  /**
   * @return the {@link org.jminor.common.server.Server} this Jetty server is associated with
   */
  protected final org.jminor.common.server.Server getConnectionServer() {
    return connectionServer;
  }
}
