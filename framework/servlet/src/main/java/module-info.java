/**
 * Servlet application server.
 * @provides dev.codion.common.rmi.server.AuxiliaryServer
 */
module dev.codion.framework.servlet {
  requires org.slf4j;
  requires java.ws.rs;
  requires javax.servlet.api;
  requires jersey.container.jetty.servlet;
  requires jersey.container.servlet.core;
  requires dev.codion.common.http;
  requires dev.codion.framework.db.rmi;

  exports dev.codion.framework.servlet;

  provides dev.codion.common.rmi.server.AuxiliaryServerProvider
          with dev.codion.framework.servlet.EntityServletServerProvider;
}