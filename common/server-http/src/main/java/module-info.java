module org.jminor.common.server.http {
  requires slf4j.api;
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.util;
  requires transitive org.jminor.common.server;

  exports org.jminor.common.server.http;
}