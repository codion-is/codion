/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.servlet;

import org.jminor.common.remote.http.HttpServer;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * A Entity servlet server
 */
public final class EntityServletServer extends HttpServer {

  /**
   * Instantiates a new EntityServletServer.
   * @param connectionServer the Server serving the connection requests
   * @see HttpServer#DOCUMENT_ROOT
   * @see HttpServer#HTTP_SERVER_PORT
   * @see HttpServer#HTTP_SERVER_SECURE
   */
  public EntityServletServer(final org.jminor.common.remote.Server connectionServer) {
    super(connectionServer, HttpServer.DOCUMENT_ROOT.get(), HttpServer.HTTP_SERVER_PORT.get(),
            HttpServer.HTTP_SERVER_SECURE.get());
    EntityService.setServer(connectionServer);
    final ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletHandler.setContextPath("/");
    final ServletHolder holder = servletHandler.addServlet(ServletContainer.class, "/entities/*");
    holder.setInitOrder(0);
    holder.setInitParameter("jersey.config.server.provider.classnames", EntityService.class.getCanonicalName());
    addHandler(servletHandler);
  }

  /** {@inheritDoc} */
  @Override
  public void stopServer() throws Exception {
    super.stopServer();
    EntityService.setServer(null);
  }
}
