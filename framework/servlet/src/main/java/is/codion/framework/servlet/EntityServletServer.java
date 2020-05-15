/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.servlet;

import dev.codion.common.http.server.HttpServer;
import dev.codion.common.http.server.HttpServerConfiguration;
import dev.codion.common.rmi.server.AuxiliaryServer;
import dev.codion.common.rmi.server.Server;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import static java.util.Objects.requireNonNull;

/**
 * A Entity servlet server
 */
public final class EntityServletServer extends HttpServer implements AuxiliaryServer {

  /**
   * Instantiates a new EntityServletServer, using configuration values from system properties.
   * @param server the parent server
   * @see HttpServerConfiguration#DOCUMENT_ROOT
   * @see HttpServerConfiguration#HTTP_SERVER_PORT
   * @see HttpServerConfiguration#HTTP_SERVER_SECURE
   */
  EntityServletServer(final Server server) {
    this(server, HttpServerConfiguration.fromSystemProperties());
  }

  /**
   * Instantiates a new EntityServletServer.
   * @param server the parent server
   * @param configuration the server configuration
   */
  EntityServletServer(final Server server, final HttpServerConfiguration configuration) {
    super(configuration);
    EntityService.setServer(requireNonNull(server, "server"));
    final ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletHandler.setContextPath("/");
    final ServletHolder holder = servletHandler.addServlet(ServletContainer.class, "/entities/*");
    holder.setInitOrder(0);
    holder.setInitParameter("jersey.config.server.provider.classnames", EntityService.class.getCanonicalName());
    addHandler(servletHandler);
    addServerStoppedListener(() -> EntityService.setServer(null));
  }
}
