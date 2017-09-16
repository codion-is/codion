/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.servlet;

import org.jminor.common.server.Server;
import org.jminor.common.server.http.HttpServer;

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
   * @see JettyServer#DOCUMENT_ROOT
   * @see Server#WEB_SERVER_PORT
   */
  public EntityServletServer(final org.jminor.common.server.Server connectionServer) {
    super(connectionServer, HttpServer.DOCUMENT_ROOT.get(), Server.WEB_SERVER_PORT.get());
    EntityServlet.setServer(connectionServer);
    final ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletHandler.setContextPath("/");
    final ServletHolder holder = servletHandler.addServlet(ServletContainer.class, "/entities/*");
    holder.setInitOrder(0);
    holder.setInitParameter("jersey.config.server.provider.classnames", EntityServlet.class.getCanonicalName());
    addHandler(servletHandler);
  }

  /** {@inheritDoc} */
  @Override
  public void stopServer() throws Exception {
    super.stopServer();
    EntityServlet.setServer(null);
  }
}
