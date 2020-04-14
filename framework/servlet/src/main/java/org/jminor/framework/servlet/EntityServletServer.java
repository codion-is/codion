/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.servlet;

import org.jminor.common.remote.http.HttpServer;
import org.jminor.common.remote.server.Server;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * A Entity servlet server
 */
public final class EntityServletServer extends HttpServer implements Server.AuxiliaryServer {

  /**
   * Instantiates a new EntityServletServer.
   * @see HttpServer#DOCUMENT_ROOT
   * @see HttpServer#HTTP_SERVER_PORT
   * @see HttpServer#HTTP_SERVER_SECURE
   */
  public EntityServletServer() {
    super(HttpServer.DOCUMENT_ROOT.get(), HttpServer.HTTP_SERVER_PORT.get(),
            HttpServer.HTTP_SERVER_SECURE.get());
    final ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletHandler.setContextPath("/");
    final ServletHolder holder = servletHandler.addServlet(ServletContainer.class, "/entities/*");
    holder.setInitOrder(0);
    holder.setInitParameter("jersey.config.server.provider.classnames", EntityService.class.getCanonicalName());
    addHandler(servletHandler);
    addServerStoppedListener(() -> EntityService.setServer(null));
  }

  @Override
  public void setServer(final Server server) {
    EntityService.setServer(server);
  }
}
