/**
 * Servlet application server.
 * @provides is.codion.common.rmi.server.AuxiliaryServerFactory
 */
module is.codion.framework.servlet {
  requires org.slf4j;
  requires io.javalin;
  requires kotlin.stdlib;
  requires org.eclipse.jetty.servlet;
  requires org.eclipse.jetty.websocket.jetty.server;
  requires is.codion.framework.db.rmi;
  requires is.codion.framework.json.domain;
  requires is.codion.framework.json.db;

  exports is.codion.framework.servlet;

  provides is.codion.common.rmi.server.AuxiliaryServerFactory
          with is.codion.framework.servlet.EntityServiceFactory;
}