/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.servlet;

import is.codion.common.http.server.HttpServer;
import is.codion.common.http.server.HttpServerConfiguration;
import is.codion.common.rmi.server.AuxiliaryServer;
import is.codion.common.rmi.server.Server;
import is.codion.framework.db.rmi.RemoteEntityConnection;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import java.rmi.Remote;

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
  EntityServletServer(final Server<RemoteEntityConnection, ? extends Remote> server) {
    this(server, HttpServerConfiguration.fromSystemProperties());
  }

  /**
   * Instantiates a new EntityServletServer.
   * @param server the parent server
   * @param configuration the server configuration
   */
  EntityServletServer(final Server<RemoteEntityConnection, ? extends Remote> server, final HttpServerConfiguration configuration) {
    super(configuration);
    requireNonNull(server, "server");
    EntityService.setServer(server);
    EntityJsonService.setServer(server);
    final ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletHandler.setContextPath("/");
    final ServletHolder holder = servletHandler.addServlet(ServletContainer.class, "/entities/ser/*");
    holder.setInitOrder(0);
    holder.setInitParameter("jersey.config.server.provider.classnames", EntityService.class.getCanonicalName());
    final ServletHolder jsonHolder = servletHandler.addServlet(ServletContainer.class, "/entities/json/*");
    jsonHolder.setInitOrder(0);
    jsonHolder.setInitParameter("jersey.config.server.provider.classnames", EntityJsonService.class.getCanonicalName());
    addHandler(servletHandler);
    addServerStoppedListener(() -> {
      EntityService.setServer(null);
      EntityJsonService.setServer(null);
    });
  }
}
