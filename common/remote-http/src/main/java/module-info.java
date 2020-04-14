/**
 * http server classes.
 */
module org.jminor.common.remote.http {
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.util;
  requires org.jminor.common.core;

  exports org.jminor.common.remote.server.http;
}