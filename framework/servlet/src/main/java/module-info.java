/**
 * Servlet application server.
 * @provides org.jminor.common.rmi.server.AuxiliaryServer
 */
module org.jminor.framework.servlet {
  requires org.slf4j;
  requires java.ws.rs;
  requires javax.servlet.api;
  requires jersey.container.jetty.servlet;
  requires jersey.container.servlet.core;
  requires org.jminor.common.http;
  requires org.jminor.framework.db.rmi;

  exports org.jminor.framework.servlet;

  provides org.jminor.common.rmi.server.AuxiliaryServerProvider
          with org.jminor.framework.servlet.EntityServletServerProvider;
}