/**
 * Servlet application server.
 * @provides is.codion.common.rmi.server.AuxiliaryServer
 */
module is.codion.framework.servlet {
  requires org.slf4j;
  requires java.ws.rs;
  requires javax.servlet.api;
  requires jersey.container.jetty.servlet;
  requires jersey.container.servlet.core;
  requires is.codion.common.http;
  requires is.codion.framework.db.rmi;

  exports is.codion.framework.servlet;

  provides is.codion.common.rmi.server.AuxiliaryServerProvider
          with is.codion.framework.servlet.EntityServletServerProvider;
}