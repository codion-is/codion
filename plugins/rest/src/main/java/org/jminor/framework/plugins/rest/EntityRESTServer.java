/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.rest;

import org.jminor.framework.plugins.jetty.JettyServer;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * A simple Jetty/Jersey based REST server
 */
public final class EntityRESTServer extends JettyServer {

  /**
   * Instantiates a new EntityRESTServer on the given port.
   * @param connectionServer the Server serving the connection requests
   */
  public EntityRESTServer(final org.jminor.common.server.Server connectionServer) {
    super(connectionServer, JettyServer.DOCUMENT_ROOT.get(), JettyServer.WEB_SERVER_PORT.get());
    EntityRESTService.setServer(connectionServer);
    final ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletHandler.setContextPath("/");
    final ServletHolder holder = servletHandler.addServlet(ServletContainer.class, "/entities/*");
    holder.setInitOrder(0);
    holder.setInitParameter("jersey.config.server.provider.classnames", EntityRESTService.class.getCanonicalName());
    addHandler(servletHandler);
  }

  /** {@inheritDoc} */
  @Override
  public void stopServer() throws Exception {
    super.stopServer();
    EntityRESTService.setServer(null);
  }
}
