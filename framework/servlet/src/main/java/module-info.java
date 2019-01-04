module org.jminor.framework.servlet {
  requires slf4j.api;
  requires java.ws.rs;
  requires javax.servlet.api;
  requires jetty.servlet;
  requires jersey.container.servlet.core;
  requires org.jminor.common.server.http;
  requires org.jminor.framework.db.remote;

  exports org.jminor.framework.servlet;
}