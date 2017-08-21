/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.rest;

import org.jminor.framework.domain.Entities;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * A simple Jetty/Jersey based REST and file server
 */
public final class EntityRESTServer extends Server implements org.jminor.common.server.Server.AuxiliaryServer {

  /**
   * Instantiates a new EntityRESTServer on the given port.
   * @param entities the domain model entities
   * @param connectionServer the Server serving the connection requests
   * @param documentRoot the document root
   * @param port the port on which to serve
   */
  public EntityRESTServer(final Entities entities, final org.jminor.common.server.Server connectionServer,
                          final String documentRoot, final Integer port) {
    super(port);
    EntityRESTService.setServer(connectionServer);
    EntityRESTService.setEntities(entities);
    final ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletHandler.setContextPath("/");
    final ServletHolder holder = servletHandler.addServlet(ServletContainer.class, "/entities/*");
    holder.setInitOrder(0);
    holder.setInitParameter("jersey.config.server.provider.classnames", EntityRESTService.class.getCanonicalName());

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
    EntityRESTService.setEntities(null);
    stop();
    join();
  }
}
