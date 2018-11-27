module org.jminor.framework.servlet {
  requires java.rmi;
  requires java.ws.rs;
  requires javax.servlet.api;
  requires slf4j.api;
  requires org.jminor.common.core;
  requires org.jminor.common.db;
  requires org.jminor.common.server;
  requires org.jminor.common.server.http;
  requires org.jminor.framework.db.core;
  requires org.jminor.framework.db.remote;

  exports org.jminor.framework.servlet;
}