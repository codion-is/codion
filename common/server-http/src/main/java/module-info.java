module org.jminor.common.remote.http {
  requires slf4j.api;
  requires org.jminor.common.core;
  requires org.jminor.common.remote;
  requires jetty.server;

  exports org.jminor.common.remote.http;
}