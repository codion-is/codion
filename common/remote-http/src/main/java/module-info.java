/**
 * http server classes.
 */
module org.jminor.common.remote.http {
  requires org.slf4j;
  requires org.eclipse.jetty.server;
  requires org.eclipse.jetty.util;
  requires transitive org.jminor.common.remote;

  exports org.jminor.common.remote.http;
}