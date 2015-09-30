/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.rest;

import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * A simple Jetty/Jersey based REST and file server
 */
public final class EntityRESTServer extends Server implements org.jminor.common.server.Server.AuxiliaryServer {

  /**
   * Instantiates a new EntityRESTServer on the given port.
   * @param connectionServer the Server serving the connection requests
   * @param documentRoot the document root
   * @param port the port on which to serve
   * @throws Exception in case of an exception
   */
  public EntityRESTServer(final org.jminor.common.server.Server connectionServer, final String documentRoot, final Integer port) throws Exception {
    super(port);
    EntityRESTService.setServer(connectionServer);
    final ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletHandler.setContextPath("/");
    servletHandler.addServlet(new ServletHolder(
            new ServletContainer(new PackagesResourceConfig("org.jminor.framework.plugins.rest"))), "/entities/*");

    final ResourceHandler fileHandler = new ResourceHandler();
    fileHandler.setResourceBase(documentRoot);

    final HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[] {fileHandler, servletHandler});

    setHandler(handlers);
  }

  /** {@inheritDoc} */
  @Override
  public void startServer() throws Exception {
    start();
  }

  /** {@inheritDoc} */
  @Override
  public void stopServer() throws Exception {
    EntityRESTService.setServer(null);
    stop();
    join();
  }
}
