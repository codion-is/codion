module org.jminor.common.remote.http {
  requires slf4j.api;
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.util;
  requires transitive org.jminor.common.remote;

  exports org.jminor.common.remote.http;
}