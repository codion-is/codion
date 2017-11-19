module org.jminor.common.server.http {
  requires slf4j.api;
  requires org.jminor.common.core;
  requires org.jminor.common.server;
  requires jetty.server;

  exports org.jminor.common.server.http;
}