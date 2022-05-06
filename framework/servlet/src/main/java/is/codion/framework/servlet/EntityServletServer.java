/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.servlet;

import is.codion.common.http.server.HttpServer;
import is.codion.common.http.server.HttpServerConfiguration;
import is.codion.common.rmi.server.AuxiliaryServer;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerAdmin;
import is.codion.framework.db.rmi.RemoteEntityConnection;

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
  EntityServletServer(Server<RemoteEntityConnection, ? extends ServerAdmin> server) {
    this(server, HttpServerConfiguration.builderFromSystemProperties().build());
  }

  /**
   * Instantiates a new EntityServletServer.
   * @param server the parent server
   * @param configuration the server configuration
   */
  EntityServletServer(Server<RemoteEntityConnection, ? extends ServerAdmin> server, HttpServerConfiguration configuration) {
    super(configuration);
    requireNonNull(server, "server");
    AbstractEntityService.setServer(server);
    ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletHandler.setContextPath("/entities");
    ServletHolder holder = servletHandler.addServlet(ServletContainer.class, "/ser/*");
    holder.setInitOrder(0);
    holder.setInitParameter("jersey.config.server.provider.classnames", EntityService.class.getCanonicalName());
    ServletHolder jsonHolder = servletHandler.addServlet(ServletContainer.class, "/json/*");
    jsonHolder.setInitOrder(0);
    jsonHolder.setInitParameter("jersey.config.server.provider.classnames", EntityJsonService.class.getCanonicalName());
    addHandler(servletHandler);
    addServerStoppedListener(() -> AbstractEntityService.setServer(null));
  }
}
