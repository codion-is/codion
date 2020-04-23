/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.servlet;

import org.jminor.common.remote.server.AuxiliaryServer;
import org.jminor.common.remote.server.Server;
import org.jminor.common.remote.server.http.HttpServer;
import org.jminor.common.remote.server.http.HttpServerConfiguration;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * A Entity servlet server
 */
public final class EntityServletServer extends HttpServer implements AuxiliaryServer {

  /**
   * Instantiates a new EntityServletServer, using configuration values from system properties.
   * @see HttpServerConfiguration#DOCUMENT_ROOT
   * @see HttpServerConfiguration#HTTP_SERVER_PORT
   * @see HttpServerConfiguration#HTTP_SERVER_SECURE
   */
  public EntityServletServer() {
    this(HttpServerConfiguration.fromSystemProperties());
  }

  /**
   * Instantiates a new EntityServletServer.
   * @param configuration the server configuration
   */
  public EntityServletServer(final HttpServerConfiguration configuration) {
    super(configuration);
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
