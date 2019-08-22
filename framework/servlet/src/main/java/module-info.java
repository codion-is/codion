/**
 * Servlet application server.
 */
module org.jminor.framework.servlet {
  requires org.slf4j;
  requires java.ws.rs;
  requires javax.servlet.api;
  requires jetty.servlet;
  requires jersey.container.servlet.core;
  requires org.jminor.common.remote.http;
  requires org.jminor.framework.db.remote;

  exports org.jminor.framework.servlet;
}