/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.rest;

import org.jminor.common.Configuration;
import org.jminor.common.Value;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.plugins.jetty.JettyServer;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * A simple Jetty/Jersey based REST server
 */
public final class EntityRESTServer extends JettyServer {

  /**
   * Specifies the id of the domain to be supplied to the web server<br>.
   * Value type: String<br>
   * Default value: null
   */
  public static final Value<String> REST_SERVER_DOMAIN_ID = Configuration.stringValue("jminor.server.rest.domainId", null);

  /**
   * Instantiates a new EntityRESTServer on the given port.
   * @param entities the domain model entities
   * @param connectionServer the Server serving the connection requests
   * @param documentRoot the document root
   * @param port the port on which to serve
   */
  public EntityRESTServer(final org.jminor.common.server.Server connectionServer) {
    this(connectionServer, JettyServer.WEB_SERVER_PORT.get());
  }

  /**
   * Instantiates a new EntityRESTServer on the given port.
   * @param entities the domain model entities
   * @param connectionServer the Server serving the connection requests
   * @param documentRoot the document root
   * @param port the port on which to serve
   */
  public EntityRESTServer(final org.jminor.common.server.Server connectionServer, final Integer port) {
    super(connectionServer, JettyServer.DOCUMENT_ROOT.get(), port);
    EntityRESTService.setServer(connectionServer);
    EntityRESTService.setEntities(Entities.getDomainEntities(REST_SERVER_DOMAIN_ID.get()));
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
    EntityRESTService.setServer(null);
    EntityRESTService.setEntities(null);
    super.stopServer();
  }
}
