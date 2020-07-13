/**
 * Servlet application server.
 * @provides is.codion.common.rmi.server.AuxiliaryServerFactory
 */
module is.codion.framework.servlet {
  requires org.slf4j;
  requires java.ws.rs;
  requires javax.servlet.api;
  requires com.fasterxml.jackson.databind;
  requires jersey.container.jetty.servlet;
  requires jersey.container.servlet.core;
  requires is.codion.common.http;
  requires is.codion.framework.db.rmi;
  requires is.codion.plugin.jackson.json;

  exports is.codion.framework.servlet;

  provides is.codion.common.rmi.server.AuxiliaryServerFactory
          with is.codion.framework.servlet.EntityServletServerFactory;
}