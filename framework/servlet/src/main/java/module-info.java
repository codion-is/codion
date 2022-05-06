/**
 * Servlet application server.
 * @provides is.codion.common.rmi.server.AuxiliaryServerFactory
 */
module is.codion.framework.servlet {
  requires org.slf4j;
  requires jakarta.ws.rs;
  requires jakarta.activation;
  requires jakarta.annotation;
  requires jakarta.inject;
  requires com.fasterxml.jackson.databind;
  requires org.eclipse.jetty.servlet;
  requires is.codion.common.http;
  requires is.codion.framework.db.rmi;
  requires is.codion.plugin.jackson.json;

  exports is.codion.framework.servlet;

  provides is.codion.common.rmi.server.AuxiliaryServerFactory
          with is.codion.framework.servlet.EntityServletServerFactory;
}